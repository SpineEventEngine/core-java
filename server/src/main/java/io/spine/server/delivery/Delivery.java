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
import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import io.spine.annotation.Internal;
import io.spine.logging.Logging;
import io.spine.server.BoundedContext;
import io.spine.server.NodeId;
import io.spine.server.ServerEnvironment;
import io.spine.server.bus.MulticastDispatchListener;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
import io.spine.server.projection.ProjectionRepository;
import io.spine.string.Stringifiers;
import io.spine.type.TypeUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.flogger.LazyArgs.lazy;
import static java.util.Collections.synchronizedList;

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
 * <h1>Configuration</h1>
 *
 * <h2>Delivery Strategy</h2>
 *
 * <p>By default, a shard is assigned according to the identifier of the target entity. The
 * messages heading to a single entity will always reside in a single shard. However,
 * the framework users may {@linkplain DeliveryBuilder#setStrategy(DeliveryStrategy) customize}
 * this behavior.
 *
 * <p>The typical customization would be to specify the same shard index for the related targets.
 * E.g. if there is an {@code OrderAggregate}, {@code OrderItemAggregate}
 * and {@code OrderItemProjection}, they could share the same shard index. In this way the messages
 * headed to these entities will be dispatched and processed together. In turn, that will reduce
 * the eventual consistency lag between {@code C} side (i.e. aggregate state updates)
 * and {@code Q} side (i.e. the respective updates in projections).
 *
 * <h2>Idempotence</h2>
 *
 * <p>As long as the underlying storage and transport mechanisms are restricted by the CAP theorem,
 * there may be duplicates in the messages written, read or dispatched. The {@code Delivery}
 * responds to it by storing some of the already delivered messages for longer and using them as
 * a source for de-duplication.
 *
 * <p>{@linkplain DeliveryBuilder#setIdempotenceWindow(Duration) Provides} the time-based
 * de-duplication capabilities to eliminate the messages, which may have been already delivered
 * to their targets. The duplicates will be detected among the messages, which are not older, than
 * {@code now - [idempotence window]}.
 *
 * <h2>Customizing {@code InboxStorage}</h2>
 *
 * <p>{@code Delivery} is responsible for providing the {@link InboxStorage} for every inbox
 * registered. Framework users may {@linkplain DeliveryBuilder#setInboxStorage(InboxStorage)
 * configure} the storage, taking into account that it is typically multi-tenant. By default,
 * the {@code InboxStorage} for the delivery is provided by the environment-specific
 * {@linkplain ServerEnvironment#storageFactory() storage factory} and is multi-tenant.
 *
 * <h2>Catch-up</h2>
 *
 * <p>In addition to delivering the messages sent in a real-time, {@code Delivery} dispatches
 * the historical events sent to the catching-up projections. These events are dispatched through
 * the same shards as the live messages. A special {@link CatchUpStation} is responsible for
 * handling this use-case. See more on that in the respective section.
 *
 * <p>To control how many historical events are read and put into shards, the end-users may
 * configure the {@linkplain DeliveryBuilder#setCatchUpPageSize(int) maximum number of messages}
 * read from the history per at a time. This is helpful to balance the per-shard throughput, so
 * that the live messages are still dispatched through the same shards in a reasonable time.
 *
 * <p>The statuses of the ongoing catch-up processes are stored in a dedicated
 * {@link CatchUpStorage}. The {@code DeliveryBuilder} {@linkplain
 * DeliveryBuilder#setCatchUpStorage(CatchUpStorage)} exposes an API for the customization of this
 * storage.
 *
 * <h2>Observers</h2>
 *
 * <p>Once a message is written to the {@code Inbox},
 * the {@linkplain Delivery#subscribe(ShardObserver) pre-configured shard observers} are
 * {@linkplain ShardObserver#onMessage(InboxMessage) notified}. In this way any third-party
 * environment planners, load balancers, and schedulers may plug into the delivery and perform
 * various routines to enable the further processing of the sharded messages. In a distributed
 * environment a message queue may be used to notify the node cluster of a shard that has some
 * messages pending for the delivery.
 *
 * <h2>Work registry</h2>
 *
 * <p>Once an application node picks the shard to deliver the messages from it, it registers itself
 * in a {@link ShardedWorkRegistry}. It serves as a list of locks-per-shard that only allows
 * to pick a shard to a single node at a time. The framework users may configure the implementation
 * of the registry by calling {@link DeliveryBuilder#setWorkRegistry(ShardedWorkRegistry)}.
 *
 * <h2>Dispatching messages</h2>
 *
 * <h3>Delivery stages</h3>
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
 * <h3>Conveyor and stations</h3>
 *
 * <p>In a scope of {@code DeliveryStage} the page of the {@code InboxMessage}s is placed
 * to the {@link Conveyor} responsible for tracking the status of each message.
 * The conveyor is run through the pipeline of stations, each modifying the state of the messages.
 * At the end of the pipeline, the changed made to the messages are committed to the underlying
 * {@code InboxStorage} in a bulk. Such an approach allows to minimize the number of the requests
 * sent to the storage.
 *
 * <p>As long as the new {@code DeliveryStage} is started, the new instance of the {@code Conveyor}
 * is created.
 *
 * <p>Below is the list of the conveyor stations in the pipeline.
 *
 * <b>1. Catch-up station</b>
 *
 * <p>This station is responsible for dispatching the historical events in
 * {@link InboxMessageStatus#TO_CATCH_UP TO_CATCH_UP} status to the respective targets. Also,
 * while the target entity is under a catch-up, all the live messages headed to it are ignored.
 * Once the catch-up is completed, this station handles the transition period, in which the last
 * batch of the historical events and live messages are dispatched together.
 * See {@link CatchUpStation} for more details.
 *
 * <b>2. Live delivery station</b>
 *
 * <p>This station is responsible for dispatching the messages sent in a real-time. It ignores
 * the messages in {@link InboxMessageStatus#TO_CATCH_UP TO_CATCH_UP} status. Another responsibility
 * of this station is to set for how long the delivered messages should be kept according to the
 * {@linkplain DeliveryBuilder#setIdempotenceWindow(Duration) idempotence window} settings.
 * See {@link LiveDeliveryStation} for more details.
 *
 * <b>3. Cleanup station</b>
 *
 * <p>This station removes the messages which are already delivered and are no longer needed for the
 * de-duplication. See {@link CleanupStation} for the description.
 *
 * <b>De-duplication</b>
 *
 * <p>During the dispatching, {@code Conveyor} keeps track of the delivered messages. The stations
 * performing the actual message dispatching rely onto this knowledge and de-duplicate
 * the messages prior to calling the target's endpoint.
 *
 * <p>Additionally, the {@code Delivery} provides a {@linkplain DeliveredMessages cache of recently
 * delivered messages}. Each instance of the {@code Conveyor} has an access to it and uses it
 * in de-duplication procedures.
 *
 * <h2>Local environment</h2>
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
 *
 * <h2>Shard maintenance</h2>
 *
 * //TODO:2020-02-10:alex.tymchenko: describe.
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
    private final InboxDeliveries deliveries;

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
     * The storage of ongoing catch-up process states.
     */
    private final CatchUpStorage catchUpStorage;

    /**
     * How many messages to read per query when recalling the historical events from the event log
     * during the catch-up.
     */
    private final int catchUpPageSize;

    /**
     * The monitor of delivery stages.
     */
    private final DeliveryMonitor monitor;

    /**
     * The cache of the locally delivered messages.
     */
    private final DeliveredMessages deliveredMessages;

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
    private final DeliveryDispatchListener dispatchListener =
            new DeliveryDispatchListener(this::onNewMessage);

    Delivery(DeliveryBuilder builder) {
        this.strategy = builder.getStrategy();
        this.workRegistry = builder.getWorkRegistry();
        this.idempotenceWindow = builder.getIdempotenceWindow();
        this.inboxStorage = builder.getInboxStorage();
        this.catchUpStorage = builder.getCatchUpStorage();
        this.catchUpPageSize = builder.getCatchUpPageSize();
        this.monitor = builder.getMonitor();
        this.pageSize = builder.getPageSize();
        this.deliveries = new InboxDeliveries();
        this.shardObservers = synchronizedList(new ArrayList<>());
        this.deliveredMessages = new DeliveredMessages();
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
                runResult = runDelivery(session);
                totalDelivered += runResult.deliveredCount();
            } while (runResult.shouldRunAgain());
        } finally {
            session.complete();
        }
        DeliveryStats stats = new DeliveryStats(index, totalDelivered);
        monitor.onDeliveryCompleted(stats);
        Optional<InboxMessage> lateMessage = inboxStorage.newestMessageToDeliver(index);
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
     * @return the results of the run
     */
    private RunResult runDelivery(ShardProcessingSession session) {
        ShardIndex index = session.shardIndex();

        Page<InboxMessage> startingPage = inboxStorage.readAll(index, pageSize);
        Optional<Page<InboxMessage>> maybePage = Optional.of(startingPage);

        boolean continueAllowed = true;
        List<DeliveryStage> stages = new ArrayList<>();
        while (continueAllowed && maybePage.isPresent()) {
            Page<InboxMessage> currentPage = maybePage.get();
            ImmutableList<InboxMessage> messages = currentPage.contents();
            if (!messages.isEmpty()) {
                DeliveryAction action = new GroupByTargetAndDeliver(deliveries);
                Conveyor conveyor = new Conveyor(messages, deliveredMessages);
                Iterable<CatchUp> catchUpJobs = catchUpStorage.readAll();
                List<Station> stations = conveyorStationsFor(catchUpJobs, action);
                DeliveryStage stage = launch(conveyor, stations, index);
                continueAllowed = monitorTellsToContinue(stage);
                stages.add(stage);
            }
            if (continueAllowed) {
                maybePage = currentPage.next();
            }
        }

        int totalMessagesDelivered = stages.stream()
                                       .map(DeliveryStage::getMessagesDelivered)
                                       .reduce(0, Integer::sum);
        return new RunResult(totalMessagesDelivered, !continueAllowed);
    }

    /**
     * Launches the conveyor, running it through the passed stations and processing the messages
     * in the specified shard.
     *
     * <p>Once all the stations complete their routine, this {@code DeliveryStage} is considered
     * completed.
     *
     * @return the delivery stage results
     */
    private DeliveryStage launch(Conveyor conveyor, Iterable<Station> stations, ShardIndex index) {
        int deliveredInBatch = 0;

        for (Station station : stations) {
            Station.Result result = station.process(conveyor);
            result.errors()
                  .throwIfAny();
            deliveredInBatch += result.deliveredCount();
        }
        notifyOfDuplicatesIn(conveyor);
        conveyor.flushTo(inboxStorage);

        return newStage(index, deliveredInBatch);
    }

    private ImmutableList<Station> conveyorStationsFor(Iterable<CatchUp> catchUpJobs,
                                                       DeliveryAction action) {
        return ImmutableList.of(
                    new CatchUpStation(action, catchUpJobs),
                    new LiveDeliveryStation(action, idempotenceWindow),
                    new CleanupStation()
        );
    }

    private void notifyOfDuplicatesIn(Conveyor conveyor) {
        Stream<InboxMessage> streamOfDuplicates = conveyor.recentDuplicates();
        streamOfDuplicates.forEach((message) -> {
            ShardedMessageDelivery<InboxMessage> delivery = deliveries.get(message);
            delivery.onDuplicate(message);
        });
    }

    private static DeliveryStage newStage(ShardIndex index, int deliveredInBatch) {
        return DeliveryStage
                .newBuilder()
                .setIndex(index)
                .setMessagesDelivered(deliveredInBatch)
                .vBuild();
    }

    private boolean monitorTellsToContinue(DeliveryStage stage) {
        return monitor.shouldContinueAfter(stage);
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
     * Creates a new instance of the builder for {@link CatchUpProcess}.
     *
     * @param repo
     *         projection repository for which the catch-up process will be created
     * @param <I>
     *         the type of identifiers of entities managed by the projection repository
     * @return new builder for the {@code CatchUpProcess}
     */
    public <I> CatchUpProcessBuilder<I> newCatchUpProcess(ProjectionRepository<I, ?, ?> repo) {
        CatchUpProcessBuilder<I> builder = CatchUpProcess.newBuilder(repo);
        return builder.withStorage(catchUpStorage).withPageSize(catchUpPageSize);
    }

    /**
     * Registers the internal {@code Delivery} message dispatchers
     * in the given {@code BoundedContext}.
     *
     * <p>The registration of the dispatchers allows to handle the {@code Delivery}-specific events.
     *
     * @param context Bounded Context in which the message dispatchers should be registered
     */
    @Internal
    public void registerDispatchersIn(BoundedContext context) {
        context.registerEventDispatcher(new ShardMaintenanceProcess(this));
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
        deliveries.register(inbox);
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
        return strategy.determineIndex(entityId, entityStateType);
    }

    /**
     * Unregisters the given {@code Inbox} and removes all the {@linkplain Inbox#delivery()
     * delivery callbacks} previously registered by this {@code Inbox}.
     */
    void unregister(Inbox<?> inbox) {
        deliveries.unregister(inbox);
    }

    @VisibleForTesting
    InboxStorage inboxStorage() {
        return inboxStorage;
    }

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
}
