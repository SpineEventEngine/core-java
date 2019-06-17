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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.grpc.stub.StreamObserver;
import io.spine.base.Time;
import io.spine.core.BoundedContextNames;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
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
 * Delivers the messages to the entities.
 *
 * <p>Splits the incoming messages into shards and allows to deliver the
 * messages to their destinations on a per-shard basis. Guarantees that one and only one
 * application server node serves the messages from the given shard at a time, thus preventing
 * any concurrent modifications of entity state.
 *
 * <b>Configuration</b>
 *
 * <p>By default, a shard is assigned according to the identifier of the target entity. The messages
 * heading to a single entity will always reside in a single shard. However, the framework users
 * may {@linkplain Builder#setStrategy(DeliveryStrategy) customize} this behavior.
 *
 * <p>{@linkplain Builder#setDeduplicationWindow(Duration) Provides} the time-based de-duplication
 * capabilities to eliminate the messages, which may have been already delivered to their targets.
 * The duplicates will be detected among the messages, which are not older, than
 * {@code now - [de-duplication window]}.
 *
 * <p>Delegates the message dispatching and low-level handling of message duplicates to
 * {@link #newInbox(TypeUrl) Inbox}es of each target entity. The respective {@code Inbox} instances
 * should be created in each of {@code Entity} repositories.
 */
public final class Delivery {

    private final DeliveryStrategy strategy;

    private final Duration deduplicationWindow;

    /**
     * The delivery strategies to use for the postponed message dispatching.
     *
     * <p>Stored per {@link TypeUrl}, which is a state type of a target {@code Entity}.
     *
     * <p>Once messages arrive for the postponed processing, a corresponding delivery is selected
     * according to the message contents. The {@code TypeUrl} in this map is stored
     * as {@code String} to avoid an extra boxing into {@code TypeUrl} of the value,
     * which resides as a Protobuf {@code string} inside an incoming message.
     */
    private final Map<String, ShardedMessageDelivery<InboxMessage>> inboxDeliveries;
    private final List<ShardObserver> shardObservers;
    private final ShardedWorkRegistry workRegistry;
    private final InboxStorage inboxStorage;

    private Delivery(Builder builder) {
        this.strategy = builder.strategy;
        this.workRegistry = builder.workRegistry;
        this.deduplicationWindow = builder.deduplicationWindow;
        this.inboxStorage = builder.inboxStorage;
        this.inboxDeliveries = Maps.newConcurrentMap();
        this.shardObservers = synchronizedList(new ArrayList<>());
    }

    /**
     * Creates an instance of new {@code Builder} of {@code Delivery}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new instance of {@code Delivery} suitable for local and development environment.
     *
     * <p>Uses a single-shard splitting. Delivers the sharded messages from their {@code Inbox}es
     * right away.
     */
    public static Delivery local() {
        Delivery delivery = newBuilder().setDeduplicationWindow(Durations.fromMillis(30000))
                                        .build();
        delivery.subscribe(new LocalDispatchingObserver());
        return delivery;
    }

    /**
     * Delivers the messages put into the shard with the passed index to their targets.
     *
     * <p>At a given moment of time, exactly one application node may serve messages from
     * a particular shard. Therefore, in scope of this delivery, an approach based on pessimistic
     * locking per-{@code ShardIndex} is applied.
     *
     * //TODO:2019-06-03:alex.tymchenko: this is not a behavior I'd expect.
     * <p>In case the given shard is not available for delivery, this method does nothing.
     *
     * //TODO:2019-06-06:alex.tymchenko: descibe per-page reading.
     *
     * @param index
     *         the shard index to deliver the messages from.
     */
    public void deliverMessagesFrom(ShardIndex index) {
        Optional<ShardProcessingSession> picked =
                workRegistry.pickUp(index, ServerEnvironment.getInstance()
                                                            .getNodeId());
        if (!picked.isPresent()) {
            return;
        }
        ShardProcessingSession session = picked.get();
        try {
            Integer deliveredMsgCount = null;
            while (deliveredMsgCount == null || deliveredMsgCount > 0) {
                //TODO:2019-06-13:alex.tymchenko: can we detect the same in a more elegant way?
                deliveredMsgCount = doDeliver(session);
            }
        } finally {
            session.complete();
        }
    }

    private int doDeliver(ShardProcessingSession session) {
        ShardIndex index = session.shardIndex();
        Timestamp now = Time.currentTime();
        Timestamp idempotenceWndStart = Timestamps.subtract(now, deduplicationWindow);

        ShardedStorage.Page<InboxMessage> startingPage = inboxStorage.contentsBackwards(index);
        Optional<ShardedStorage.Page<InboxMessage>> maybePage =
                Optional.of(startingPage);

        int totalMessagesDelivered = 0;
        while (maybePage.isPresent()) {
            ShardedStorage.Page<InboxMessage> currentPage = maybePage.get();
            ImmutableList<InboxMessage> messages = currentPage.contents();
            if (messages.isEmpty()) {
                maybePage = currentPage.next();
                continue;
            }
            ImmutableList.Builder<InboxMessage> deliveryBuilder = ImmutableList.builder();
            ImmutableList.Builder<InboxMessage> idempotenceBuilder = ImmutableList.builder();
            ImmutableList.Builder<InboxMessage> removalBuilder = ImmutableList.builder();
            for (InboxMessage message : messages) {
                Timestamp msgTime = message.getWhenReceived();
                boolean insideIdempotentWnd =
                        Timestamps.compare(msgTime, idempotenceWndStart) >= 0;
                InboxMessageStatus status = message.getStatus();

                if (insideIdempotentWnd) {
                    if (InboxMessageStatus.TO_DELIVER != status) {
                        idempotenceBuilder.add(message);
                    }
                } else {
                    removalBuilder.add(message);
                }

                if (InboxMessageStatus.TO_DELIVER == status) {
                    deliveryBuilder.add(message);
                }
            }

            ImmutableList<InboxMessage> toDeliver = deliveryBuilder.build();
            if (!toDeliver.isEmpty()) {

                Map<String, List<InboxMessage>> messagesByType =
                        groupByTargetType(toDeliver);
                Map<String, List<InboxMessage>> idempotenceWndByType =
                        groupByTargetType(idempotenceBuilder.build());

                ImmutableList.Builder<RuntimeException> exceptionsBuilder = ImmutableList.builder();
                for (String typeUrl : messagesByType.keySet()) {
                    ShardedMessageDelivery<InboxMessage> delivery =
                            inboxDeliveries.get(typeUrl);
                    List<InboxMessage> deliveryPackage = messagesByType.get(typeUrl);
                    List<InboxMessage> idempotenceWnd = idempotenceWndByType.get(typeUrl);
                    try {
                        delivery.deliver(deliveryPackage,
                                         idempotenceWnd ==
                                                 null ? ImmutableList.of() : idempotenceWnd);
                    } catch (RuntimeException e) {
                        exceptionsBuilder.add(e);
                    }
                }
                inboxStorage.markDelivered(toDeliver);
                ImmutableList<InboxMessage> toRemove = removalBuilder.build();
                inboxStorage.removeAll(toRemove);
                int deliveredInBatch = toDeliver.size();
                totalMessagesDelivered += deliveredInBatch;
                Timestamp lastMsgTimestamp = toDeliver.get(deliveredInBatch - 1)
                                                      .getWhenReceived();
                session.updateLastProcessed(lastMsgTimestamp);
                ImmutableList<RuntimeException> exceptions = exceptionsBuilder.build();
                if(!exceptions.isEmpty()) {
                    throw exceptions.iterator()
                                    .next();
                }
            }
            maybePage = currentPage.next();
        }
        return totalMessagesDelivered;
    }

    /**
     * Notifies that the shard with the given index has been updated with some message(s).
     *
     * @param shardIndex
     *         an index of the shard
     */
    private void notify(ShardIndex shardIndex) {
        for (StreamObserver<ShardIndex> observer : shardObservers) {
            observer.onNext(shardIndex);
        }
    }

    /**
     * Creates an instance of {@link Inbox.Builder} for the given entity type.
     *
     * @param entityType
     *         the type of the entity, to which the inbox will belong
     * @param <I>
     *         the type if entity identifiers
     * @return the builder for the {@code Inbox}
     */
    public <I> Inbox.Builder<I> newInbox(TypeUrl entityType) {
        return Inbox.newBuilder(entityType, inboxWriter());
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
    public void subscribe(ShardObserver observer) {
        shardObservers.add(observer);
    }

    void register(Inbox<?> inbox) {
        TypeUrl entityType = inbox.getEntityStateType();
        inboxDeliveries.put(entityType.value(), inbox.delivery());
    }

    void unregister(Inbox<?> inbox) {
        TypeUrl entityType = inbox.getEntityStateType();
        inboxDeliveries.remove(entityType.value());
    }

    private InboxWriter inboxWriter() {
        return new NotifyingWriter(inboxStorage) {

            @Override
            protected void onUpdated(ShardIndex index) {
                Delivery.this.notify(index);
            }
        };
    }

    private static Map<String, List<InboxMessage>> groupByTargetType
            (List<InboxMessage> messages) {
        return messages.stream()
                       .collect(groupingBy(m -> m.getInboxId()
                                                 .getTypeUrl()));
    }

    /**
     * Tells whether the delivery is enabled.
     *
     * <p>If there is just a single shard configured, the delivery is considered disabled.
     */
    public boolean enabled() {
        return strategy.getShardCount() > 1;
    }

    ShardIndex whichShardFor(Object msgDestinationId) {
        return strategy.getIndexFor(msgDestinationId);
    }

    /**
     * A builder for {@code Delivery} instances.
     */
    public static class Builder {

        private InboxStorage inboxStorage;
        private Supplier<StorageFactory> storageFactorySupplier;
        private DeliveryStrategy strategy;
        private ShardedWorkRegistry workRegistry;
        private Duration deduplicationWindow = Duration.getDefaultInstance();

        /**
         * Prevents a direct instantiation of this class.
         */
        private Builder() {
        }

        public void setWorkRegistry(ShardedWorkRegistry workRegistry) {
            this.workRegistry = checkNotNull(workRegistry);
        }

        public Builder setStrategy(DeliveryStrategy strategy) {
            this.strategy = checkNotNull(strategy);
            return this;
        }

        public Builder setDeduplicationWindow(Duration deduplicationWindow) {
            this.deduplicationWindow = checkNotNull(deduplicationWindow); ;
            return this;
        }

        public Builder setStorageFactorySupplier(
                Supplier<StorageFactory> storageFactorySupplier) {
            checkNotNull(storageFactorySupplier);
            this.storageFactorySupplier = storageFactorySupplier;
            return this;
        }

        //TODO:2019-05-22:alex.tymchenko: set the work registry using the storage factory.
        public Delivery build() {
            if (strategy == null) {
                strategy = UniformAcrossAllShards.singleShard();
            }

            StorageFactory storageFactory = initStorageFactory();

            inboxStorage = storageFactory.createInboxStorage();

            if (workRegistry == null) {
                workRegistry = new InMemoryShardedWorkRegistry();
            }

            Delivery delivery = new Delivery(this);
            return delivery;
        }

        private StorageFactory initStorageFactory() {
            StorageFactory storageFactory;
            if (storageFactorySupplier == null) {
                storageFactory = InMemoryStorageFactory.newInstance(
                        BoundedContextNames.newName("Delivery"), true);
            } else {
                storageFactory = storageFactorySupplier.get();
            }
            return storageFactory;
        }
    }
}
