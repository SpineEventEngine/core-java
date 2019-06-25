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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;
import io.spine.server.NodeId;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
import io.spine.server.storage.memory.InMemoryInboxStorage;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
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
 * <p>Delegates the message dispatching and low-level handling of message duplicates to
 * {@link #newInbox(TypeUrl) Inbox}es of each target entity. The respective {@code Inbox} instances
 * should be created in each of {@code Entity} repositories.
 *
 * <b>Configuration</b>
 *
 * <p>By default, a shard is assigned according to the identifier of the target entity. The messages
 * heading to a single entity will always reside in a single shard. However, the framework users
 * may {@linkplain Builder#setStrategy(DeliveryStrategy) customize} this behavior.
 *
 * <p>{@linkplain Builder#setIdempotenceWindow(Duration) Provides} the time-based de-duplication
 * capabilities to eliminate the messages, which may have been already delivered to their targets.
 * The duplicates will be detected among the messages, which are not older, than
 * {@code now - [idempotence window]}.
 *
 * <p>{@code Delivery} is responsible for providing the {@link InboxStorage} for every inbox
 * registered. Framework users should {@linkplain Builder#setInboxStorage(InboxStorage)
 * configure} the storage, taking into account that it is typically multi-tenant.
 *
 * <p>Once a message is written to the {@code Inbox},
 * the {@linkplain Delivery#subscribe(ShardObserver) pre-configured shard observers} are
 * {@linkplain ShardObserver#onMessage(InboxMessage) notified}. In this way any third-party
 * environment planners, load balancers, and schedulers may plug into the delivery and perform
 * various routines to enable the further processing of the sharded messages. In a distributed
 * environment a message queue may be used to notify the node cluster of a shard that has some
 * messages pending for the delivery.
 *
 * <p>Once an application node picks the shard to deliver the messages from it,
 * it registers itself in a {@link ShardedWorkRegistry}. It serves as a list of locks-per-shard
 * that only allows to pick a shard to a single node at a time.
 *
 * <b>Local environment</b>
 *
 * <p>By default, the delivery is configured to {@linkplain Delivery#local() run locally}. It
 * uses {@linkplain LocalDispatchingObserver see-and-dispatch observer}, which delivers the messages
 * from the observed shard once a message is passed to its
 * {@link LocalDispatchingObserver#onMessage(InboxMessage) onMessage(InboxMessage)} method. This
 * process is synchronous.
 *
 * <p>To deal with the multi-threaded access in a local mode,
 * an {@linkplain InMemoryShardedWorkRegistry} is used. It operates on top of the
 * {@code synchronized} in-memory data structures and prevents several threads from picking up the
 * same shard.
 */
public final class Delivery {

    /**
     * The width of the idempotence window in a local environment.
     *
     * <p>Selected to be pretty big to avoid dispatching duplicates to any entities.
     */
    private static final Duration LOCAL_IDEMPOTENCE_WINDOW = Durations.fromSeconds(30);

    /**
     * The strategy of assigning a shard index for a message that is delivered to a particular
     * target.
     */
    private final DeliveryStrategy strategy;

    /**
     * For how long we keep the previously delivered message per-target to ensure the new messages
     * aren't duplicates.
     */
    private final Duration idempotenceWindow;

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

    /**
     * The observers that are notified when a message is written into a particular shard.
     */
    private final List<ShardObserver> shardObservers;

    /**
     * The registry keeping track of which shards are processed by which application nodes.
     */
    private final ShardedWorkRegistry workRegistry;

    /**
     * The storage of messages to deliver.
     */
    private final InboxStorage inboxStorage;

    private Delivery(Builder builder) {
        this.strategy = builder.strategy;
        this.workRegistry = builder.workRegistry;
        this.idempotenceWindow = builder.idempotenceWindow;
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
     * <p>Uses a {@linkplain UniformAcrossAllShards#singleShard() single-shard} splitting.
     */
    public static Delivery local() {
        return localWithShardsAndWindow(1, LOCAL_IDEMPOTENCE_WINDOW);
    }

    /**
     * Creates a new instance of {@code Delivery} suitable for local and development environment
     * with the given number of shards.
     */
    @VisibleForTesting
    static Delivery localWithShardsAndWindow(int shardCount, Duration idempotenceWindow) {
        checkArgument(shardCount > 0, "Shard count must be positive");
        checkNotNull(idempotenceWindow);

        DeliveryStrategy strategy = UniformAcrossAllShards.forNumber(shardCount);
        return localWithStrategyAndWindow(strategy, idempotenceWindow);
    }

    @VisibleForTesting
    static Delivery localWithStrategyAndWindow(DeliveryStrategy strategy,
                                               Duration idempotenceWindow) {
        Delivery delivery =
                newBuilder().setIdempotenceWindow(idempotenceWindow)
                            .setStrategy(strategy)
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
     * <p>In case the given shard is already processed by some node, this method does nothing.
     *
     * <p>The content of the shard is read and delivered on page-by-page basis. The runtime
     * exceptions occurring while a page is being delivered are accumulated and then the first
     * exception is rethrown, if any.
     *
     * <p>After all the pages are read, the delivery process is launched again for the same shard.
     * It is required in order to handle the messages, that may have been put to the same shard
     * as an outcome of the first-wave messages.
     *
     * <p>Once the shard has no more messages to deliver, the delivery process ends, releasing
     * the lock for the respective {@code ShardIndex}.
     *
     * @param index
     *         the shard index to deliver the messages from.
     */
    @SuppressWarnings("WeakerAccess")   // a part of the public API.
    public void deliverMessagesFrom(ShardIndex index) {
        NodeId currentNode = ServerEnvironment.instance()
                                              .nodeId();
        Optional<ShardProcessingSession> picked =
                workRegistry.pickUp(index, currentNode);
        if (!picked.isPresent()) {
            return;
        }
        ShardProcessingSession session = picked.get();
        try {
            int deliveredMsgCount;
            do {
                deliveredMsgCount = doDeliver(session);
            } while (deliveredMsgCount > 0);
        } finally {
            session.complete();
        }
    }

    /**
     * Runs the delivery for the shard, which session is passed.
     *
     * @return the number of messages delivered
     */
    private int doDeliver(ShardProcessingSession session) {
        ShardIndex index = session.shardIndex();
        Timestamp now = Time.currentTime();
        Timestamp idempotenceWndStart = Timestamps.subtract(now, idempotenceWindow);

        Page<InboxMessage> startingPage = inboxStorage.readAll(index);
        Optional<Page<InboxMessage>> maybePage = Optional.of(startingPage);

        int totalMessagesDelivered = 0;
        while (maybePage.isPresent()) {
            Page<InboxMessage> currentPage = maybePage.get();
            ImmutableList<InboxMessage> messages = currentPage.contents();
            if (!messages.isEmpty()) {
                MessageClassifier classifier = MessageClassifier.of(messages, idempotenceWndStart);
                int deliveredInBatch = deliverClassified(classifier);
                totalMessagesDelivered += deliveredInBatch;

                ImmutableList<InboxMessage> toRemove = classifier.removals();
                inboxStorage.removeAll(toRemove);
            }
            maybePage = currentPage.next();
        }
        return totalMessagesDelivered;
    }

    /**
     * Takes the messages classified as those to deliver and performs the actual delivery.
     *
     * @return the number of messages delivered
     */
    private int deliverClassified(MessageClassifier classifier) {
        ImmutableList<InboxMessage> toDeliver = classifier.toDeliver();
        int deliveredInBatch = 0;
        if (!toDeliver.isEmpty()) {
            ImmutableList<InboxMessage> idempotenceSource = classifier.idempotenceSource();

            ImmutableList<RuntimeException> observedExceptions =
                    deliverByType(toDeliver, idempotenceSource);

            deliveredInBatch = toDeliver.size();

            if (!observedExceptions.isEmpty()) {
                throw observedExceptions.iterator()
                                        .next();
            }
        }
        return deliveredInBatch;
    }

    private ImmutableList<RuntimeException>
    deliverByType(ImmutableList<InboxMessage> toDeliver, ImmutableList<InboxMessage> idmptSource) {

        Map<String, List<InboxMessage>> messagesByType = groupByTargetType(toDeliver);
        Map<String, List<InboxMessage>> idmptSourceByType = groupByTargetType(idmptSource);

        ImmutableList.Builder<RuntimeException> exceptionsAccumulator = ImmutableList.builder();
        for (String typeUrl : messagesByType.keySet()) {
            ShardedMessageDelivery<InboxMessage> delivery = inboxDeliveries.get(typeUrl);
            List<InboxMessage> deliveryPackage = messagesByType.get(typeUrl);
            List<InboxMessage> idempotenceWnd = idmptSourceByType.getOrDefault(typeUrl,
                                                                               ImmutableList.of());
            try {
                delivery.deliver(deliveryPackage, idempotenceWnd);
            } catch (RuntimeException e) {
                exceptionsAccumulator.add(e);
            }
        }
        ImmutableList<RuntimeException> exceptions = exceptionsAccumulator.build();
        inboxStorage.markDelivered(toDeliver);
        return exceptions;
    }

    /**
     * Notifies that the contents of the shard with the given index have been updated
     * with some message.
     *
     * @param message
     *         a message that was written into the shard
     */
    private void onNewMessage(InboxMessage message) {
        for (ShardObserver observer : shardObservers) {
            observer.onMessage(message);
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

    /**
     * Registers the passed {@code Inbox} and puts its {@linkplain Inbox#delivery() delivery
     * callbacks} into the list of those to be called, when the previously sharded messages
     * are dispatched to their targets.
     */
    void register(Inbox<?> inbox) {
        TypeUrl entityType = inbox.entityStateType();
        inboxDeliveries.put(entityType.value(), inbox.delivery());
    }

    /**
     * Determines the shard index for the message, judging on the identifier of the entity,
     * to which this message is dispatched.
     *
     * @param entityId
     *         the ID of the entity, to which the message is heading
     * @return the index of the shard for the message
     */
    ShardIndex whichShardFor(Object entityId) {
        return strategy.indexFor(entityId);
    }

    /**
     * Unregisters the given {@code Inbox} and removes all the {@linkplain Inbox#delivery()
     * delivery callbacks} previously registered by this {@code Inbox}.
     */
    void unregister(Inbox<?> inbox) {
        TypeUrl entityType = inbox.entityStateType();
        inboxDeliveries.remove(entityType.value());
    }

    @VisibleForTesting
    InboxStorage storage() {
        return inboxStorage;
    }

    @VisibleForTesting
    int shardCount() {
        return strategy.shardCount();
    }

    private InboxWriter inboxWriter() {
        return new NotifyingWriter(inboxStorage) {

            @Override
            protected void onShardUpdated(InboxMessage message) {
                Delivery.this.onNewMessage(message);
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
     * A builder for {@code Delivery} instances.
     */
    public static class Builder {

        private @Nullable InboxStorage inboxStorage;
        private @Nullable DeliveryStrategy strategy;
        private @Nullable ShardedWorkRegistry workRegistry;
        private @Nullable Duration idempotenceWindow;

        /**
         * Prevents a direct instantiation of this class.
         */
        private Builder() {
        }

        public Optional<InboxStorage> inboxStorage() {
            return Optional.ofNullable(inboxStorage);
        }

        public Optional<DeliveryStrategy> strategy() {
            return Optional.ofNullable(strategy);
        }

        public Optional<ShardedWorkRegistry> workRegistry() {
            return Optional.ofNullable(workRegistry);
        }

        public Optional<Duration> idempotenceWindow() {
            return Optional.ofNullable(idempotenceWindow);
        }

        @CanIgnoreReturnValue
        public Builder setWorkRegistry(ShardedWorkRegistry workRegistry) {
            this.workRegistry = checkNotNull(workRegistry);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setStrategy(DeliveryStrategy strategy) {
            this.strategy = checkNotNull(strategy);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setIdempotenceWindow(Duration idempotenceWindow) {
            this.idempotenceWindow = checkNotNull(idempotenceWindow);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setInboxStorage(InboxStorage inboxStorage) {
            checkNotNull(inboxStorage);
            this.inboxStorage = inboxStorage;
            return this;
        }

        public Delivery build() {
            if (strategy == null) {
                strategy = UniformAcrossAllShards.singleShard();
            }

            if (idempotenceWindow == null) {
                idempotenceWindow = Duration.getDefaultInstance();
            }

            if (this.inboxStorage == null) {
                this.inboxStorage = new InMemoryInboxStorage(true);
            }

            if (workRegistry == null) {
                workRegistry = new InMemoryShardedWorkRegistry();
            }

            Delivery delivery = new Delivery(this);
            return delivery;
        }
    }
}
