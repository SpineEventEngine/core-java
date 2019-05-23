/*
 * Copyright 2019, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.sharding;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;
import io.spine.server.inbox.InboxMessage;
import io.spine.server.inbox.InboxStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.groupingBy;

/**
 * A mechanism of splitting the data among the application nodes and dedicating the processing
 * of a particular data piece to a particular node to avoid concurrent modification and prevent
 * the data loss.
 *
 * @author Alex Tymchenko
 */
public final class Sharding {

    /**
     * The unset value of the {@link Timestamp} values.
     *
     * <p>Used to improve the performance of checking if some {@code Timestamp} value is default,
     * rather than calling {@link io.spine.validate.Validate#isDefault(Message)
     * Validate.isDefault(Message)}.
     */
    private static final Timestamp UNSET = Timestamp.getDefaultInstance();

    /**
     * Used to calculate the period, which, when passing, guarantees that messages older than it,
     * are in the past.
     *
     * <p>The messages cannot be read up until the current moment, as there may be several messages
     * in a single millisecond.
     */
    public static final Duration ONE_MS = Durations.fromMillis(1);

    private final ShardingStrategy strategy;
    private final Duration deduplicationPeriod;
    private final Map<String, ProcessingBehavior<InboxMessage>> behaviors;
    private final ShardedWorkRegistry workRegistry;
    private final InboxStorage inboxStorage;

    private Sharding(Builder builder) {
        this.strategy = builder.strategy;
        this.workRegistry = builder.workRegistry;
        this.deduplicationPeriod = builder.deduplicationPeriod;
        this.inboxStorage = builder.inboxStorage;
        this.behaviors = ImmutableMap.copyOf(builder.behaviors);
    }

    /**
     * Creates an instance of new {@code Builder} of {@code Sharding}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public void process(ShardIndex index) {
        ShardProcessingSession session = workRegistry.pickUp(index);

        Timestamp now = Time.currentTime();
        Timestamp deduplicationStart = Timestamps.subtract(now, deduplicationPeriod);
        Timestamp whenLastProcessed = session.whenLastMessageProcessed();

        Timestamp readTo = Timestamps.subtract(now, ONE_MS);
        Timestamp readFrom = UNSET;
        if (!UNSET.equals(whenLastProcessed)) {

            // Take the oldest of two timestamps.
            readFrom = Timestamps.compare(whenLastProcessed, deduplicationStart) > 0
                       ? deduplicationStart
                       : whenLastProcessed;
        }

        Optional<ShardedStorage.Page<InboxMessage>> maybePage =
                Optional.of(inboxStorage.readAll(index, readFrom, readTo));

        while (maybePage.isPresent()) {
            ShardedStorage.Page<InboxMessage> currentPage = maybePage.get();
            ImmutableList<InboxMessage> messages = currentPage.contents();
            ImmutableList.Builder<InboxMessage> toProcessBuilder = ImmutableList.builder();
            ImmutableList.Builder<InboxMessage> dedupSourceBuilder = ImmutableList.builder();
            ImmutableList.Builder<InboxMessage> toRemoveBuilder = ImmutableList.builder();
            for (InboxMessage message : messages) {
                Timestamp msgTime = message.getWhenReceived();

                if (Timestamps.compare(msgTime, deduplicationStart) >= 0) {

                    // The message was received later than de-duplication start time,
                    // so it goes to both de-duplication source list and the list to process.
                    dedupSourceBuilder.add(message);
                } else {

                    // The message is scheduled to be removed after it's processed.
                    toRemoveBuilder.add(message);
                }
                toProcessBuilder.add(message);
            }

            ImmutableList<InboxMessage> messagesToProcess = toProcessBuilder.build();
            ImmutableList<InboxMessage> dedupSource = dedupSourceBuilder.build();

            Map<String, List<InboxMessage>> messagesByType = groupByTargetType(messagesToProcess);
            Map<String, List<InboxMessage>> dedupSourceByType = groupByTargetType(dedupSource);

            for (String typeUrl : messagesByType.keySet()) {
                ProcessingBehavior<InboxMessage> behavior = behaviors.get(typeUrl);
                List<InboxMessage> messagesForBehavior = messagesByType.get(typeUrl);
                List<InboxMessage> dedupSourceForBehavior = dedupSourceByType.get(typeUrl);
                behavior.process(messagesForBehavior, dedupSourceForBehavior);
            }

            InboxMessage lastMessage = messages.get(messages.size() - 1);
            session.updateLastProcessed(lastMessage.getWhenReceived());
            inboxStorage.removeAll(toRemoveBuilder.build());
            maybePage = currentPage.next();
        }
    }

    private static Map<String, List<InboxMessage>> groupByTargetType(List<InboxMessage> messages) {
        return messages.stream()
                       .collect(groupingBy(m -> m.getInboxId()
                                                 .getTypeUrl()));
    }

    /**
     * Tells whether the sharding is enabled.
     *
     * <p>If there is just a single shard configured, the sharding is considered disabled.
     */
    public boolean enabled() {
        return strategy.getShardCount() > 1;
    }

    public ShardIndex whichShardFor(Object msgDestinationId) {
        return strategy.getIndexFor(msgDestinationId);
    }

    /**
     * A builder for {@code Sharding} instances.
     */
    public static class Builder {

        private InboxStorage inboxStorage;
        private Supplier<StorageFactory> storageFactorySupplier;
        private ShardingStrategy strategy;
        private Duration deduplicationPeriod;
        private final Map<String, ProcessingBehavior<InboxMessage>> behaviors = new HashMap<>();
        private ShardedWorkRegistry workRegistry;

        /**
         * Prevents a direct instantiation of this class.
         */
        private Builder() {
        }

        public Builder setStrategy(ShardingStrategy strategy) {
            this.strategy = checkNotNull(strategy);
            return this;
        }

        public Builder addBehavior(TypeUrl messageType, ProcessingBehavior<InboxMessage> behavior) {
            checkNotNull(behavior);
            behaviors.put(messageType.value(), behavior);
            return this;
        }

        public Builder setDeduplicationPeriod(Duration deduplicationPeriod) {
            checkNotNull(deduplicationPeriod);
            this.deduplicationPeriod = deduplicationPeriod;
            return this;
        }

        public Builder setStorageFactorySupplier(Supplier<StorageFactory> storageFactorySupplier) {
            checkNotNull(storageFactorySupplier);
            this.storageFactorySupplier = storageFactorySupplier;
            return this;
        }

        //TODO:2019-05-22:alex.tymchenko: set the work registry using the storage factory.
        public Sharding build() {
            if (strategy == null) {
                strategy = UniformAcrossAllShards.singleShard();
            }
            inboxStorage = storageFactorySupplier.get()
                                                 .createInboxStorage();
            Sharding sharding = new Sharding(this);
            return sharding;
        }
    }
}
