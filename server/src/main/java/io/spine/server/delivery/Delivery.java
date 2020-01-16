/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.base.Time;
import io.spine.core.SignalId;
import io.spine.core.TenantId;
import io.spine.logging.Logging;
import io.spine.server.NodeId;
import io.spine.server.ServerEnvironment;
import io.spine.server.bus.MulticastDispatchListener;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
import io.spine.server.model.ModelError;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.string.Stringifiers;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Multimaps.synchronizedListMultimap;
import static com.google.common.flogger.LazyArgs.lazy;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedSet;
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
 * <p>By default, a shard is assigned according to the identifier of the target entity. The
 * messages heading to a single entity will always reside in a single shard. However,
 * the framework users may {@linkplain DeliveryBuilder#setStrategy(DeliveryStrategy) customize}
 * this behavior.
 *
 * <p>{@linkplain DeliveryBuilder#setIdempotenceWindow(Duration) Provides} the time-based
 * de-duplication capabilities to eliminate the messages, which may have been already delivered
 * to their targets. The duplicates will be detected among the messages, which are not older, than
 * {@code now - [idempotence window]}.
 *
 * <p>{@code Delivery} is responsible for providing the {@link InboxStorage} for every inbox
 * registered. Framework users may {@linkplain DeliveryBuilder#setInboxStorage(InboxStorage)
 * configure} the storage, taking into account that it is typically multi-tenant. By default,
 * the {@code InboxStorage} for the delivery is provided by the environment-specific
 * {@linkplain ServerEnvironment#storageFactory() storage factory} and is multi-tenant.
 *
 * <p>Once a message is written to the {@code Inbox},
 * the {@linkplain Delivery#subscribe(ShardObserver) pre-configured shard observers} are
 * {@linkplain ShardObserver#onMessage(InboxMessage) notified}. In this way any third-party
 * environment planners, load balancers, and schedulers may plug into the delivery and perform
 * various routines to enable the further processing of the sharded messages. In a distributed
 * environment a message queue may be used to notify the node cluster of a shard that has some
 * messages pending for the delivery.
 *
 * <p>Once an application node picks the shard to deliver the messages from it, it registers itself
 * in a {@link ShardedWorkRegistry}. It serves as a list of locks-per-shard that only allows
 * to pick a shard to a single node at a time.
 *
 * <p>The delivery process for each shard index is split into {@link DeliveryStage}s. In scope of
 * each stage, a certain number of messages is read from the respective shard of the {@code Inbox}.
 * The messages are grouped per-target and delivered in batches if possible. The maximum
 * number of the messages within a {@code DeliveryStage} can be
 * {@linkplain DeliveryBuilder#setPageSize(int) configured}.
 *
 * <p>After each {@code DeliveryStage} it is possible to stop the delivery by
 * {@link DeliveryBuilder#setMonitor(DeliveryMonitor) supplying} a custom delivery monitor.
 * Please refer to the {@link DeliveryMonitor documentation} for the details.
 *
 * <b>Local environment</b>
 *
 * <p>By default, the delivery is configured to {@linkplain Delivery#local() run locally}. It
 * uses {@linkplain LocalDispatchingObserver see-and-dispatch observer}, which delivers the
 * messages from the observed shard once a message is passed to its
 * {@link LocalDispatchingObserver#onMessage(InboxMessage) onMessage(InboxMessage)} method. This
 * process is synchronous.
 *
 * <p>To deal with the multi-threaded access in a local mode,
 * an {@linkplain InMemoryShardedWorkRegistry} is used. It operates on top of the
 * {@code synchronized} in-memory data structures and prevents several threads from picking up the
 * same shard.
 */
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods"}) // It's fine for a centerpiece.
public final class Delivery implements Logging {

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

    /**
     * The monitor of delivery stages.
     */
    private final DeliveryMonitor monitor;

    /**
     * The maximum amount of messages to deliver within a {@link DeliveryStage}.
     */
    private final int pageSize;

    /**
     * The listener of the dispatching operations inside the {@link io.spine.server.bus.MulticastBus
     * MulticastBus}es.
     *
     * <p>Responsible for sending the notifications to the shard observers.
     */
    private final DeliveryDispatchListener dispatchListener = new DeliveryDispatchListener();

    Delivery(DeliveryBuilder builder) {
        this.strategy = builder.getStrategy();
        this.workRegistry = builder.getWorkRegistry();
        this.idempotenceWindow = builder.getIdempotenceWindow();
        this.inboxStorage = builder.getInboxStorage();
        this.monitor = builder.getMonitor();
        this.pageSize = builder.getPageSize();
        this.inboxDeliveries = Maps.newConcurrentMap();
        this.shardObservers = synchronizedList(new ArrayList<>());
    }

    /**
     * Creates an instance of new {@code Builder} of {@code Delivery}.
     */
    public static DeliveryBuilder newBuilder() {
        return new DeliveryBuilder();
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
     * <p>In case the given shard is already processed by some node, this method does nothing and
     * returns {@code Optional.empty()}.
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
     * @return the statistics on the performed delivery, or {@code Optional.empty()} if there
     *         were no delivery performed
     */
    public Optional<DeliveryStats> deliverMessagesFrom(ShardIndex index) {
        NodeId currentNode = ServerEnvironment.instance()
                                              .nodeId();
        Optional<ShardProcessingSession> picked = workRegistry.pickUp(index, currentNode);
        if (!picked.isPresent()) {
            return Optional.empty();
        }
        ShardProcessingSession session = picked.get();
        monitor.onDeliveryStarted(index);

        RunResult runResult;
        int totalDelivered = 0;
        try {
            do {
                runResult = doDeliver(session);
                totalDelivered += runResult.deliveredCount();
            } while (runResult.shouldRunAgain());
        } finally {
            session.complete();
        }
        DeliveryStats stats = new DeliveryStats(index, totalDelivered);
        monitor.onDeliveryCompleted(stats);
        Optional<InboxMessage> lateMessage = inboxStorage.oldestMessageToDeliver(index);
        lateMessage.ifPresent(this::onNewMessage);

        return Optional.of(stats);
    }

    /**
     * Runs the delivery for the shard, which session is passed.
     *
     * <p>The messages are read page-by-page according to the {@link #pageSize page size} setting.
     *
     * <p>After delivering each page of messages, a {@code DeliveryStage} is produced.
     * The configured {@link #monitor DeliveryMonitor} may stop the execution according to
     * the monitored {@code DeliveryStage}.
     *
     * @return the passed delivery stage
     */
    private RunResult doDeliver(ShardProcessingSession session) {
        ShardIndex index = session.shardIndex();
        Timestamp now = Time.currentTime();
        Timestamp idempotenceWndStart = Timestamps.subtract(now, idempotenceWindow);

        Page<InboxMessage> startingPage = inboxStorage.readAll(index, pageSize);
        Optional<Page<InboxMessage>> maybePage = Optional.of(startingPage);

        int totalMessagesDelivered = 0;
        boolean continueAllowed = true;
        while (continueAllowed && maybePage.isPresent()) {
            Page<InboxMessage> currentPage = maybePage.get();
            ImmutableList<InboxMessage> messages = currentPage.contents();
            if (!messages.isEmpty()) {
                MessageClassifier classifier = MessageClassifier.of(messages, idempotenceWndStart);
                int deliveredInBatch = deliverClassified(classifier);
                totalMessagesDelivered += deliveredInBatch;

                ImmutableList<InboxMessage> toRemove = classifier.removals();
                inboxStorage.removeAll(toRemove);
                DeliveryStage stage = newStage(index, deliveredInBatch);
                continueAllowed = monitorTellsToContinue(stage);
            }
            if (continueAllowed) {
                maybePage = currentPage.next();
            }
        }
        return new RunResult(totalMessagesDelivered, !continueAllowed);
    }

    private static DeliveryStage newStage(ShardIndex index, int deliveredInBatch) {
        return DeliveryStage
                .newBuilder()
                .setIndex(index)
                .setMessagesDelivered(deliveredInBatch)
                .vBuild();
    }

    private boolean monitorTellsToContinue(@Nullable DeliveryStage stage) {
        if (stage == null) {
            return true;
        }
        return monitor.shouldContinueAfter(stage);
    }

    /**
     * Takes the messages classified as those to deliver and performs the actual delivery.
     *
     * <p>If an exception is thrown during delivery, this method propagates it. If many exceptions
     * are thrown, all of them are added to the first one as {@code suppressed}, and the first one
     * is propagated.
     *
     * <p>In case of an exception, the messages are marked as delivered, in order to avoid
     * repetitive delivery. However, if a JVM {@link Error} is thrown, only the messages which were
     * delivered successfully are marked as delivered. Moreover, an JVM {@link Error} halts delivery
     * for all the subsequent messages in the batch. However, this is not true for
     * {@link ModelError}s, which are treated in the same way as exceptions.
     *
     * @return the number of messages delivered, {@code 0} if no messages are classified for
     *         delivery
     */
    private int deliverClassified(MessageClassifier classifier) {
        ImmutableList<InboxMessage> toDeliver = classifier.toDeliver();
        if (!toDeliver.isEmpty()) {
            ImmutableList<InboxMessage> idempotenceSource = classifier.idempotenceSource();
            DeliveryErrors observedExceptions = deliverByType(toDeliver, idempotenceSource);
            observedExceptions.throwIfAny();
            return toDeliver.size();
        } else {
            return 0;
        }
    }

    private DeliveryErrors
    deliverByType(ImmutableList<InboxMessage> toDeliver, ImmutableList<InboxMessage> idmptSource) {

        Map<String, List<InboxMessage>> messagesByType = groupByTargetType(toDeliver);
        Map<String, List<InboxMessage>> idmptSourceByType = groupByTargetType(idmptSource);

        DeliveryErrors.Builder errors = DeliveryErrors.newBuilder();
        for (String typeUrl : messagesByType.keySet()) {
            ShardedMessageDelivery<InboxMessage> delivery = inboxDeliveries.get(typeUrl);
            List<InboxMessage> deliveryPackage = messagesByType.get(typeUrl);
            List<InboxMessage> idempotenceWnd = idmptSourceByType.getOrDefault(typeUrl,
                                                                               ImmutableList.of());
            try {
                delivery.deliver(deliveryPackage, idempotenceWnd);
            } catch (RuntimeException exception) {
                errors.addException(exception);
            } catch (@SuppressWarnings("ErrorNotRethrown") /* False positive */ ModelError error) {
                errors.addError(error);
            }
        }
        inboxStorage.markDelivered(toDeliver);
        return errors.build();
    }

    /**
     * Notifies that the contents of the shard with the given index have been updated
     * with some message.
     *
     * @param message
     *         a message that was written into the shard
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    private void onNewMessage(InboxMessage message) {
        for (ShardObserver observer : shardObservers) {
            try {
                observer.onMessage(message);
            } catch (Exception e) {
                _error().withCause(e)
                        .log("Error calling a shard observer with the message %s.",
                             lazy(() -> Stringifiers.toString(message)));
            }
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
     * Returns a listener of the dispatching operations occurring in the
     * {@link io.spine.server.bus.MulticastBus MulticastBus}es.
     */
    @Internal
    public MulticastDispatchListener dispatchListener() {
        return dispatchListener;
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
     * @param entityStateType
     *         the state type of the entity, to which the message is heading
     * @return the index of the shard for the message
     */
    ShardIndex whichShardFor(Object entityId, TypeUrl entityStateType) {
        return strategy.indexFor(entityId, entityStateType);
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
                Delivery.this.dispatchListener.notifyOf(message);
            }
        };
    }

    private static Map<String, List<InboxMessage>> groupByTargetType(List<InboxMessage> messages) {
        return messages.stream()
                       .collect(groupingBy(m -> m.getInboxId()
                                                 .getTypeUrl()));
    }

    /**
     * Listens to the signals dispatched via the {@code MulticastBus}es and notifies the shard
     * observers of a new {@code InboxMessage} only when it has been dispatched to all of
     * its multicast targets.
     */
    private class DeliveryDispatchListener implements MulticastDispatchListener {

        private final Multimap<SignalId, InboxMessage> pending =
                synchronizedListMultimap(MultimapBuilder.hashKeys()
                                                        .arrayListValues()
                                                        .build());


        private final Set<SignalId> currentlyDispatching = synchronizedSet(new HashSet<>());

        @Override
        public void onStarted(SignalId signal) {
            currentlyDispatching.add(signal);
        }

        @Override
        public void onCompleted(SignalId signal) {
            boolean removed = currentlyDispatching.remove(signal);
            if (removed) {
                Collection<InboxMessage> messages = pending.removeAll(signal);
                for (InboxMessage message : messages) {
                    propagateMessage(message);
                }
            }
        }

        private void notifyOf(InboxMessage message) {
            SignalId id = message.hasEvent()
                          ? message.getEvent()
                                   .getId()
                          : message.getCommand()
                                   .getId();
            if (currentlyDispatching.contains(id)) {
                pending.put(id, message);
            } else {
                propagateMessage(message);
            }
        }

        private void propagateMessage(InboxMessage message) {
            TenantId tenant =
                    message.hasEvent() ? message.getEvent()
                                                .tenant()
                                       : message.getCommand()
                                                .tenant();
            TenantAwareRunner
                    .with(tenant)
                    .run(() -> onNewMessage(message));
        }
    }
}
