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
import com.google.common.collect.Maps;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.grpc.stub.StreamObserver;
import io.spine.base.Time;
import io.spine.core.BoundedContextNames;
import io.spine.server.inbox.Inbox;
import io.spine.server.inbox.InboxMessage;
import io.spine.server.inbox.InboxStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.type.TypeUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.synchronizedList;
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
    private static final Duration ONE_MS = Durations.fromMillis(1);

    private final ShardingStrategy strategy;
    private final Duration deduplicationPeriod;

    /**
     * The behaviors to call for the postponed message dispatching.
     *
     * <p>Mapped per {@link TypeUrl}, which is a state type of a target {@code Entity}.
     *
     * <p>Once messages arrive for the postponed processing, a corresponding behavior is selected
     * according to the message contents. The {@code TypeUrl} in this map is stored
     * as {@code String} to avoid an extra boxing into {@code TypeUrl} of the value,
     * which resides as a Protobuf {@code string} inside an incoming message.
     */
    private final Map<String, ShardedMessageDelivery<InboxMessage>> inboxDeliveries;
    private final List<StreamObserver<ShardIndex>> shardObservers;
    private final ShardedWorkRegistry workRegistry;
    private final InboxStorage inboxStorage;

    private Sharding(Builder builder) {
        this.strategy = builder.strategy;
        this.workRegistry = builder.workRegistry;
        this.deduplicationPeriod = builder.deduplicationPeriod;
        this.inboxStorage = builder.inboxStorage;
        this.inboxDeliveries = Maps.newConcurrentMap();
        this.shardObservers = synchronizedList(new ArrayList<>());
    }

    /**
     * Creates an instance of new {@code Builder} of {@code Sharding}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Delivers the messages put into the shard with the passed index to their targets.
     *
     * <p>At a given moment of time, the only application node may deliver messages from
     * a particular shard. Therefore, in scope of this delivery, an approach based on pessimistic
     * locking per-{@code ShardIndex} is applied.
     *
     * //TODO:2019-06-03:alex.tymchenko: this is not a behavior I'd expect.
     * <p>In case the given shard is not available for delivery, this method does nothing.
     *
     * @param index
     *         the shard index to deliver the messages from.
     */
    public void deliverMessagesFrom(ShardIndex index) {
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
                ShardedMessageDelivery<InboxMessage> delivery = inboxDeliveries.get(typeUrl);
                List<InboxMessage> messagesForBehavior = messagesByType.get(typeUrl);
                List<InboxMessage> dedupSourceForBehavior = dedupSourceByType.get(typeUrl);
                delivery.deliver(messagesForBehavior, dedupSourceForBehavior);
            }

            InboxMessage lastMessage = messages.get(messages.size() - 1);
            session.updateLastProcessed(lastMessage.getWhenReceived());
            inboxStorage.removeAll(toRemoveBuilder.build());
            maybePage = currentPage.next();
        }
    }

    //TODO:2019-06-04:alex.tymchenko: try to pack the logic into the `register` call.
    /**
     * Notifies that the shard with the given index has been updated with some message(s).
     *
     * @param shardIndex
     *         an index of the shard
     */
    public void notify(ShardIndex shardIndex) {
        for (StreamObserver<ShardIndex> observer : shardObservers) {
            observer.onNext(shardIndex);
        }
    }

    /**
     * Subscribes to the updates of shard contents.
     *
     * <p>The passed observer will be notified that the contents of a shard with a particular index
     * were changed.
     *
     * @param observer
     *         an observer to notify of updates.
     */
    public void subscribe(StreamObserver<ShardIndex> observer) {
        shardObservers.add(observer);
    }

    public void register(Inbox<?> inbox) {
        TypeUrl entityType = inbox.getEntityStateType();
        inboxDeliveries.put(entityType.value(), inbox.delivery());
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
        private ShardedWorkRegistry workRegistry;
        private Duration deduplicationPeriod = Duration.getDefaultInstance();

        /**
         * Prevents a direct instantiation of this class.
         */
        private Builder() {
        }

        public void setWorkRegistry(ShardedWorkRegistry workRegistry) {
            this.workRegistry = checkNotNull(workRegistry);
        }

        public Builder setStrategy(ShardingStrategy strategy) {
            this.strategy = checkNotNull(strategy);
            return this;
        }

        public Builder setDeduplicationPeriod(Duration deduplicationPeriod) {
            this.deduplicationPeriod = checkNotNull(deduplicationPeriod); ;
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
            StorageFactory storageFactory;
            if (storageFactorySupplier == null) {
                storageFactory = InMemoryStorageFactory.newInstance(
                        BoundedContextNames.newName("Sharding"), true);
            } else {
                storageFactory = storageFactorySupplier.get();
            }
            inboxStorage = storageFactory.createInboxStorage();
            Sharding sharding = new Sharding(this);
            return sharding;
        }
    }
}
