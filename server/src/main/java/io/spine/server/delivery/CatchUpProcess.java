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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.event.CatchUpCompleted;
import io.spine.server.delivery.event.CatchUpRequested;
import io.spine.server.delivery.event.CatchUpStarted;
import io.spine.server.delivery.event.HistoryEventsRecalled;
import io.spine.server.delivery.event.HistoryFullyRecalled;
import io.spine.server.delivery.event.LiveEventsPickedUp;
import io.spine.server.delivery.event.ProjectionStateCleared;
import io.spine.server.delivery.event.ShardProcessed;
import io.spine.server.delivery.event.ShardProcessingRequested;
import io.spine.server.event.AbstractStatefulReactor;
import io.spine.server.event.EventFactory;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.EventStreamQuery.Limit;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.EitherOf3;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Durations.fromMillis;
import static com.google.protobuf.util.Durations.fromNanos;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.server.delivery.CatchUpMessages.catchUpCompleted;
import static io.spine.server.delivery.CatchUpMessages.fullyRecalled;
import static io.spine.server.delivery.CatchUpMessages.limitOf;
import static io.spine.server.delivery.CatchUpMessages.liveEventsPickedUp;
import static io.spine.server.delivery.CatchUpMessages.recalled;
import static io.spine.server.delivery.CatchUpMessages.shardProcessingRequested;
import static io.spine.server.delivery.CatchUpMessages.started;
import static io.spine.server.delivery.CatchUpMessages.toFilters;
import static io.spine.server.delivery.CatchUpStatus.COMPLETED;
import static io.spine.server.delivery.CatchUpStatus.FINALIZING;
import static io.spine.server.delivery.CatchUpStatus.IN_PROGRESS;
import static io.spine.server.delivery.DeliveryStrategy.newIndex;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * //TODO:2020-04-27:alex.tymchenko: update the description.
 * A process that performs a projection catch-up.
 *
 * <p>Starts the catch-up process by emitting the {@link CatchUpRequested} event.
 *
 * <p>Has its own {@link Inbox}, so the messages arriving to it are dispatched by the
 * {@link Delivery}.
 *
 * <p>While recalling the event history, the process relies on the work of {@code Delivery},
 * which follows the status of the process and delivers messages in {@code TO_CATCH_UP} status
 * instead of live {@code TO_DELIVER} messages to the catching-up targets.
 *
 * <p>The tricky part is so called "turbulence" period, i.e. the time when the catch-up comes
 * close to the current time in the event history. In this phase the task is to understand when to
 * stop reading the history and switch back to delivering the live events dispatched to the
 * catching-up projections. To do so, a special {@code FINALIZING} status is introduced. Observed
 * by {@code Delivery} it tells to perform a transition from dispatching {@code TO_CATCH_UP} events
 * back to delivering those {@code TO_DELIVER}. See more on that below.
 *
 * <p>In this version of the process implementation, the turbulence period always equals 500 ms
 * meaning that the catch-up starts its finalization when the event history is read
 * up until {@code now - 500 ms}.
 *
 * <p>In its lifecycle, the process moves through the several statuses.
 *
 * <p><b>{@linkplain CatchUpStatus#CUS_UNDEFINED Not started}</b>
 *
 * <p>The process is created in to this status upon receiving {@code CatchUpRequested} event.
 * The further actions include:
 *
 * <ul>
 *     <li>A {@link CatchUpStarted} event is emitted. The target projection repository listens to
 *     this event and kills the state of the matching entities.
 *
 *     <li>The status if the catch-up process is set to {@link CatchUpStatus#IN_PROGRESS
 *     IN_PROGRESS}.
 * </ul>
 *
 * <p><b>{@link CatchUpStatus#IN_PROGRESS IN_PROGRESS}</b>
 *
 * <p>When the process is in this status, the event history is read and the matching events are sent
 * to the {@code Inbox}es of the corresponding projections
 *
 * <p>The catch-up maintains this status until the history is read till the point in time,
 * which is very close to the {@link Time#currentTime() Time.currentTime()}. When this time comes,
 * the events arriving to the {@code EventStore} are very likely to be just-emitted in a live mode.
 * Therefore, the process needs to decide whether to decide that the history is fully read, or
 * to continue the traversal through the history.
 *
 * <p>At this stage the actions are as follows.
 *
 * <ul>
 *      <li>The historical event messages are read from the {@link EventStore} respecting
 *      the time range requested and the time of the last read operation performed by this process.
 *      The maximum number of the events read is determined by
 *      {@linkplain DeliveryBuilder#setCatchUpPageSize(int) one of the Delivery settings}.
 *
 *      <li>Depending on the targets requested for the catch-up, the events are posted to the
 *      corresponding entities.
 *
 *      <li>Unless the timestamps of the events are getting close to the current time, an
 *      {@link HistoryEventsRecalled} is emitted, leaving the process in the {@code IN_PROGRESS}
 *      status and triggering the next round similar to this one.
 *
 *      <p>If the timestamps of the events read on this step are as close to the current time as
 *      the turbulence period, the {@link HistoryFullyRecalled} is emitted.
 * </ul>
 *
 * <p><b>{@link CatchUpStatus#FINALIZING FINALIZING}</b>
 *
 * <p>The process moves to this status when the event history has been fully recalled and the
 * corresponding {@code HistoryFullyRecalled} is received. At this stage the {@code Delivery} stops
 * the propagation of the events to the catch-up messages, waiting for this process to populate
 * the inboxes with the messages arriving to be dispatched during the turbulence period.
 * Potentially, the inboxes will contain the duplicates produced by both the live users
 * and this process. To deal with it, a deduplication is performed by the {@code Delivery}.
 * See {@link CatchUpStation} for more details.
 *
 * <p>The actions are as follows.
 *
 * <ul>
 *      <li>All the remaining messages of the matching event types are read {@link EventStore}.
 *      In this operation the read limits set by the {@code Delivery} are NOT used, since
 *      the goal is to read the remainder of the events.
 *
 *      <li>If there were no events read, the {@link CatchUpCompleted} is emitted and the process
 *      is moved to the {@link CatchUpStatus#COMPLETED COMPLETED} status.
 *
 *      <li>If there were some events read, the {@link LiveEventsPickedUp} event is emitted.
 *      This allows some time for the posted events to become visible to the {@code Delivery}.
 *      The reacting handler of the {@code LiveEventsPickedUp} completes the process.
 * </ul>
 *
 * <p><b>{@link CatchUpStatus#COMPLETED COMPLETED}</b>
 *
 * <p>Once the process moves to the {@code COMPLETED} status, the corresponding {@code Delivery}
 * routines deduplicate, reorder and dispatch the "paused" events. Then the normal live delivery
 * flow is resumed.
 *
 * <p>To ensure that all the shards in which the "paused" historical events reside are processed,
 * this process additionally emits the "maintenance" {@link ShardProcessingRequested} events for
 * each shard involved. By dispatching them, the system guarantees that the {@code Delivery}
 * observes the {@code COMPLETED} status of this process and delivers the remainer of the messages.
 *
 * @implNote Technically, the instances of this class are not
 *         {@linkplain io.spine.server.procman.ProcessManager process managers}, since it is
 *         impossible to register the process managers with the same state in
 *         a multi-{@code BoundedContext} application.
 */
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods"})    // It does a lot.
public final class CatchUpProcess<I>
        extends AbstractStatefulReactor<CatchUpId, CatchUp, CatchUp.Builder> {

    /**
     * The type URL of the process state.
     *
     * <p>Used for the message routing.
     */
    private static final TypeUrl TYPE = TypeUrl.from(CatchUp.getDescriptor());

    /**
     * The duration of the turbulence period, counting back from the current time.
     */
    private static final Turbulence TURBULENCE = Turbulence.of(fromMillis(500));

    private final ProjectionRepository<I, ?, ?> repository;
    private final DispatchCatchingUp<I> dispatchOperation;
    private final CatchUpStorage storage;
    private final CatchUpStarter.Builder<I> starterTemplate;
    private final Limit queryLimit;

    private @MonotonicNonNull CatchUpStarter<I> catchUpStarter;
    private @MonotonicNonNull Supplier<EventStore> eventStore;

    CatchUpProcess(CatchUpProcessBuilder<I> builder) {
        super(TYPE);
        this.repository = builder.getRepository();
        this.dispatchOperation = builder.getDispatchOp();
        this.storage = builder.getStorage();
        this.queryLimit = limitOf(builder.getPageSize());
        this.starterTemplate = CatchUpStarter.newBuilder(this.repository, this.storage);
    }

    @Internal
    public static <I> CatchUpProcessBuilder<I> newBuilder(ProjectionRepository<I, ?, ?> repo) {
        checkNotNull(repo);
        return new CatchUpProcessBuilder<>(repo);
    }

    @Override
    @Internal
    public void registerWith(BoundedContext context) {
        super.registerWith(context);
        this.eventStore = () -> context.eventBus()
                                       .eventStore();
        this.catchUpStarter = starterTemplate.withContext(context)
                                             .build();
    }

    /**
     * Starts the catch-up for the projection instances selecting them by their identifiers.
     *
     * <p>If no identifiers is passed (i.e. {@code null}) then all of the instances should be
     * caught up
     *
     * @param since
     *         since when the catch-up should be performed
     * @param ids
     *         identifiers of the projections to catch up, or {@code null} if all of the
     *         instances should be caught up
     * @return identifier of the catch-up operation
     * @throws CatchUpAlreadyStartedException
     *         if at least one of the selected instances is already catching up at the moment
     */
    @Internal
    public CatchUpId startCatchUp(Timestamp since, @Nullable Set<I> ids)
            throws CatchUpAlreadyStartedException {
        return catchUpStarter.start(ids, since);
    }

    /**
     * Moves the process from {@code Not Started} to {@code IN_PROGRESS} state.
     *
     * Two important things happen at this stage:
     * <ul>
     *
     *      <li>The reading timestamp of the process is set to the one specified in the request.
     *      However, people are used to say "I want to catch-up since 1 PM" meaning "counting
     *      the events happened at 1 PM sharp". Therefore process always subtracts a single
     *      nanosecond from the specified catch-up start time and then treats this interval as
     *      exclusive.
     *
     *      <li>{@link CatchUpStarted} event is dispatched directly to the inboxes of the
     *      catching-up targets based on the original catch-up request.
     *
     *      <li>The identifiers of the catch-up targets are defined. They are set according to
     *      the actual IDs of the projections to which the {@code CatchUpStarted} has been
     *      dispatched.
     *      It is important to know the target IDs, since their state has to be reset to default
     *      before the dispatching of the first historical event.
     *
     *      <p>The number of the projection instances to catch-up is remembered. The process
     *      then waits for {@link ProjectionStateCleared} events to arrive for each of
     *      the instances before reading the events from the history.
     * </ul>
     *
     * <p>The event handler returns {@code Nothing}, as the results of its work are dispatched
     * directly to the inboxes of the catching-up projections.
     */
    @React
    Nothing handle(CatchUpRequested e, EventContext ctx) {
        CatchUpId id = e.getId();

        CatchUp.Request request = e.getRequest();

        Timestamp sinceWhen = request.getSinceWhen();
        Timestamp withWindow = subtract(sinceWhen, fromNanos(1));
        builder().setWhenLastRead(withWindow)
                 .setRequest(request);
        CatchUpStarted started = started(id);
        builder().setStatus(CatchUpStatus.STARTED);
        flushState();

        dispatchCatchUpStarted(started, ctx);
        return nothing();
    }

    private void dispatchCatchUpStarted(CatchUpStarted started, EventContext ctx) {
        Event event = wrapAsEvent(started, ctx);
        Set<I> ids = targetsForCatchUpSignals(builder().getRequest());
        Set<I> targetIds = dispatchAll(ImmutableList.of(event), ids);
        builder().setInstancesToClear(targetIds.size());
    }

    /**
     * Waits until all the projection instances report their state has been cleared and they
     * are ready for the historical events to be dispatched.
     *
     * <p>Once all the instances are ready, performs the first reading from the event history
     * and dispatches the results to the inboxes of respective projections.
     *
     * <p>If the history has been read fully (i.e. until the start of the {@linkplain Turbulence
     * turbulence period}), emits {@code HistoryFullyRecalled} event. Otherwise, emits
     * {@code HistoryEventsRecalled} by which it triggers the next round of history reading.
     */
    @React
    EitherOf3<HistoryEventsRecalled, HistoryFullyRecalled, Nothing>
    handle(ProjectionStateCleared event) {
        int leftToClear = builder().getInstancesToClear() - 1;
        builder().setInstancesToClear(leftToClear);
        if (leftToClear > 0) {
            return EitherOf3.withC(nothing());
        }
        builder().setStatus(IN_PROGRESS);

        EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> result = recallMoreEvents();
        if (result.hasA()) {
            return EitherOf3.withA(result.getA());
        } else {
            return EitherOf3.withB(result.getB());
        }
    }

    /**
     * Performs the second and all the following reads from the event history and dispatches the
     * historical events to the inboxes of the catching-up projections.
     *
     * <p>If the history has been read fully (i.e. until the start of the {@linkplain Turbulence
     * turbulence period}), emits {@code HistoryFullyRecalled} event. Otherwise,
     * emits {@code HistoryEventsRecalled} by which triggers the next round of history reading.
     */
    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> handle(HistoryEventsRecalled event) {
        return recallMoreEvents();
    }

    /**
     * Reads the event history starting from the last event read and dispatches the events to the
     * inboxes of the target projections.
     *
     * <p>Each read operation is bounded by the {@linkplain CatchUp#getWhenLastRead()
     * timestamp of last recalled event} and the start of the {@linkplain Turbulence
     * turbulence period}.
     *
     * <p>Read operations are also supplied with a query limit configured for this process. Thus,
     * several events stamped with the same time value may be caught in-between query pages. As
     * long as their order is not defined, some of them could have gone lost. To deal with this
     * issue, this method strips the events with the most recent time from the query result and
     * aims to read all of them in the next read round.
     *
     * <p>After reading, the time of the last event is recorded to the process state and is used
     * as a starting point for the next read round.
     *
     * <p>If there were no events read, the history is considered fully recalled. The process
     * will still have to deal with the event potentially emitted during the turbulence.
     */
    private EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> recallMoreEvents() {
        CatchUpId id = builder().getId();
        CatchUp.Request request = builder().getRequest();

        List<Event> readInThisRound = readMore(request, TURBULENCE.whenStarts(), queryLimit);
        if (!readInThisRound.isEmpty()) {
            List<Event> stripped = stripLastTimestamp(readInThisRound);

            Event lastEvent = stripped.get(stripped.size() - 1);
            Timestamp lastEventTimestamp = lastEvent.getContext()
                                                    .getTimestamp();
            builder().setWhenLastRead(lastEventTimestamp);
            dispatchAll(stripped);
        } else {
            return EitherOf2.withB(fullyRecalled(id));
        }
        return EitherOf2.withA(recalled(id));
    }

    /**
     * Sets the process status to {@link CatchUpStatus#FINALIZING FINALIZING} and reads all
     * the remaining events, then dispatching those to the projection inboxes.
     *
     * <p>If there were no events read, sets the process status to {@link CatchUpStatus#COMPLETED
     * COMPLETED}. Otherwise, emits {@code LiveEventsPickedUp} event, which is dispatched to this
     * process in the next round.
     */
    @React
    LiveEventsPickedUp handle(HistoryFullyRecalled event, EventContext context) {
        CatchUpId id = event.getId();
        if (builder().getStatus() != FINALIZING) {
            builder().setStatus(FINALIZING);
            flushState();
        }
        return runFinalization(id);
    }

    private LiveEventsPickedUp runFinalization(CatchUpId id) {
        CatchUp.Request request = builder().getRequest();
        List<Event> events = readMore(request, null, null);
        if (events.size() > 0) {
            Timestamp lastEventTimestamp = events.get(events.size() - 1)
                                                 .timestamp();
            builder().setWhenLastRead(lastEventTimestamp);
            dispatchAll(events);
        }
        return liveEventsPickedUp(id);
    }

    @React
    List<ShardProcessingRequested> handle(LiveEventsPickedUp event, EventContext context) {
        List<Integer> affectedShards = builder().getAffectedShardList();
        int totalShards = builder().getTotalShards();
        List<ShardProcessingRequested> messages = toShardEvents(totalShards, affectedShards);
        return messages;
    }

    @React
    EitherOf3<CatchUpCompleted, ShardProcessingRequested, Nothing> handle(ShardProcessed event) {
        if (builder().getStatus() == COMPLETED) {
            return EitherOf3.withC(nothing());
        }

        CatchUpId id = builder().getId();
        DeliveryContext deliveryContext = event.getContext();
        Optional<CatchUp> stateVisibileToDelivery =
                deliveryContext.getCatchUpJobList()
                               .stream()
                               .filter((job) -> job.getId()
                                                   .equals(id))
                               .findFirst();
        if (stateVisibileToDelivery.isPresent()) {
            CatchUp observedJob = stateVisibileToDelivery.get();
            if (observedJob.getStatus() == FINALIZING) {
                int indexOfFinalized = event.getIndex()
                                            .getIndex();

                if (!builder().getFinalizedShardList()
                              .contains(indexOfFinalized)) {
                    builder().addFinalizedShard(indexOfFinalized);
                }
                List<Integer> affectedShards = builder().getAffectedShardList();
                List<Integer> finalizedShards = builder().getFinalizedShardList();
                if (finalizedShards.containsAll(affectedShards)) {
                    CatchUpCompleted completed = completeProcess(builder().getId());
                    return EitherOf3.withA(completed);
                }
                return EitherOf3.withC(nothing());
            }
        }
        ShardProcessingRequested stillRequested =
                ShardProcessingRequested.newBuilder()
                                        .setIndex(event.getIndex())
                                        .setRequesterId(Identifier.pack(id))
                                        .vBuild();
        return EitherOf3.withB(stillRequested);
    }

    /**
     * Upon the catch-up completion, requests an additional processing of the messages in those
     * shards, which were involved into the dispatching during the catch-up.
     *
     * <p>In this way, the process ensures that any events, which delivery may have been potentially
     * paused during the process finalization, are propagated from their shards to the target
     * projection instances.
     */
    @SuppressWarnings("unused") // We just need the fact of dispatching.
    @React
    List<ShardProcessingRequested> on(CatchUpCompleted ignored) {
        int shardCount = builder().getTotalShards();
        List<Integer> affectedShards = builder().getAffectedShardList();

        List<ShardProcessingRequested> events = toShardEvents(shardCount, affectedShards);
        return events;
    }

    private CatchUpCompleted completeProcess(CatchUpId id) {
        builder().setStatus(COMPLETED);
        flushState();
        CatchUpCompleted completed = catchUpCompleted(id);
        return completed;
    }

    private Set<I> targetsForCatchUpSignals(CatchUp.Request request) {
        Set<I> ids;
        List<Any> rawTargets = request.getTargetList();
        ids = rawTargets.isEmpty()
              ? ImmutableSet.copyOf(repository.index())
              : unpack(rawTargets);
        return ids;
    }

    private Event wrapAsEvent(EventMessage event, EventContext context) {
        Event firstEvent;
        EventFactory factory = EventFactory.forImport(context.actorContext(), producerId());
        firstEvent = factory.createEvent(event, null);
        return firstEvent;
    }

    private static List<Event> stripLastTimestamp(List<Event> events) {
        int lastIndex = events.size() - 1;
        Event lastEvent = events.get(lastIndex);
        Timestamp lastTimestamp = lastEvent.getContext()
                                           .getTimestamp();
        for (int index = lastIndex; index >= 0; index--) {
            Event event = events.get(index);
            Timestamp timestamp = event.getContext()
                                       .getTimestamp();
            if (!timestamp.equals(lastTimestamp)) {
                List<Event> result = events.subList(0, index + 1);
                return result;
            }
        }
        return events;
    }

    private List<ShardProcessingRequested> toShardEvents(int totalShards, List<Integer> indexes) {
        CatchUpId id = builder().getId();
        return indexes
                .stream()
                .map(indexValue -> {
                    ShardIndex shardIndex = newIndex(indexValue, totalShards);
                    ShardProcessingRequested event = shardProcessingRequested(id, shardIndex);
                    return event;
                })
                .collect(toList());
    }

    private void recordAffectedShards(Set<I> actualTargets) {
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        String type = builder().getId()
                               .getProjectionType();
        TypeUrl projectionType = TypeUrl.parse(type);

        Set<Integer> affectedShards =
                actualTargets.stream()
                             .map(t -> delivery.whichShardFor(t, projectionType))
                             .map(ShardIndex::getIndex)
                             .collect(toSet());
        List<Integer> previouslyAffectedShards = builder().getAffectedShardList();
        Set<Integer> newValue = new TreeSet<>(affectedShards);
        newValue.addAll(previouslyAffectedShards);

        int totalShards = delivery.shardCount();
        builder().clearAffectedShard()
                 .addAllAffectedShard(newValue)
                 .setTotalShards(totalShards);
    }

    @CanIgnoreReturnValue
    private Set<I> dispatchAll(List<Event> events) {
        if (events.isEmpty()) {
            return ImmutableSet.of();
        }
        CatchUp.Request request = builder().getRequest();
        List<Any> packedIds = request.getTargetList();
        if (packedIds.isEmpty()) {
            return dispatchAll(events, new HashSet<>());
        } else {
            Set<I> ids = unpack(packedIds);
            return dispatchAll(events, ids);
        }
    }

    /**
     * Dispatches the given list of events to the passed targets and returns the actual set of
     * entity identifiers to which the dispatching has been performed.
     *
     * @param events
     *         the events to dispatch
     * @param targets
     *         the set of identifiers of the targets to dispatch the events to;
     *         if empty, no particular targets are selected, so the target repository will
     *         decide on its own
     * @return the list of the identifiers to which the dispatching has been made in fact
     */
    @CanIgnoreReturnValue
    private Set<I> dispatchAll(List<Event> events, Set<I> targets) {
        if (events.isEmpty()) {
            return ImmutableSet.of();
        }
        Set<I> actualTargets = new HashSet<>();
        @Nullable Set<I> targetsForDispatch = targets.isEmpty()
                                              ? null
                                              : targets;
        for (Event event : events) {
            Set<I> targetsOfThisDispatch = dispatchOperation.perform(event, targetsForDispatch);
            actualTargets.addAll(targetsOfThisDispatch);
        }
        if (!actualTargets.isEmpty()) {
            recordAffectedShards(actualTargets);
        }
        return actualTargets;
    }

    private List<Event> readMore(CatchUp.Request request,
                                 @Nullable Timestamp readBefore,
                                 @Nullable Limit limit) {
        if (readBefore != null
                && Timestamps.compare(readBefore, builder().getWhenLastRead()) <= 0) {
            return ImmutableList.of();
        }
        EventStreamQuery query = toEventQuery(request, readBefore, limit);
        MemoizingObserver<Event> observer = new MemoizingObserver<>();
        eventStore.get()
                  .read(query, observer);
        List<Event> allEvents = observer.responses();
        return allEvents;
    }

    private Set<I> unpack(List<Any> packedIds) {
        return packedIds.stream()
                        .map((any) -> Identifier.unpack(any, repository.idClass()))
                        .collect(toSet());
    }

    private EventStreamQuery toEventQuery(CatchUp.Request request,
                                          @Nullable Timestamp readBefore,
                                          @Nullable Limit limit) {
        ImmutableList<EventFilter> filters = toFilters(request.getEventTypeList());
        Timestamp readAfter = builder().getWhenLastRead();
        EventStreamQuery.Builder builder =
                EventStreamQuery.newBuilder()
                                .setAfter(readAfter)
                                .addAllFilter(filters);
        if (readBefore != null) {
            builder.setBefore(readBefore);
        }
        if (limit != null) {
            builder.setLimit(limit);
        }
        return builder.vBuild();
    }

    /*
     * Event reactor lifecycle
     ************************/

    /**
     * {@inheritDoc}
     *
     * <p>If the passed envelope is a {@link CatchUpSignal}, it is verified that the projection type
     * URL passed with the event matches the one of the projection repository configured
     * for this instance.
     */
    @Override
    public boolean canDispatch(EventEnvelope envelope) {
        EventMessage raw = envelope.message();
        if (raw instanceof CatchUpSignal) {
            CatchUpSignal asSignal = (CatchUpSignal) raw;
            String actualType = asSignal.getId()
                                        .getProjectionType();
            String expectedType = repository.entityStateType()
                                            .value();
            return expectedType.equals(actualType);
        }
        return true;
    }

    @SuppressWarnings("ChainOfInstanceofChecks")    // Handling two distinct cases.
    @Override
    protected ImmutableSet<CatchUpId> route(EventEnvelope event) {
        EventMessage message = event.message();
        if (message instanceof CatchUpSignal) {
            return routeCatchUpSignal(message);
        }
        if (message instanceof ShardProcessed) {
            return routeShardProcessed((ShardProcessed) message);
        }
        return ImmutableSet.of();
    }

    private static ImmutableSet<CatchUpId> routeShardProcessed(ShardProcessed message) {
        Any requester = message.getRequesterId();
        CatchUpId id = Identifier.unpack(requester, CatchUpId.class);
        return ImmutableSet.of(id);
    }

    private static ImmutableSet<CatchUpId> routeCatchUpSignal(EventMessage message) {
        CatchUpSignal signal = (CatchUpSignal) message;
        return ImmutableSet.of(signal.getId());
    }

    @Override
    protected Optional<CatchUp> load(CatchUpId id) {
        return storage.read(id);
    }

    @Override
    protected void store(CatchUp updatedState) {
        storage.write(updatedState);
    }

    @Override
    protected CatchUp.Builder newStateBuilderWith(CatchUpId id) {
        return CatchUp.newBuilder()
                      .setId(id);
    }

    /**
     * A method object dispatching the event to catch-up.
     *
     * @param <I>
     *         the type of the identifiers of the entities to which the event should be dispatched.
     */
    @FunctionalInterface
    public interface DispatchCatchingUp<I> {

        /**
         * Dispatches the given event and optionally narrowing down the entities by the set
         * of entity identifiers to dispatch the event to.
         *
         * <p>If no particular IDs are specified, the event will be dispatched according to the
         * repository routing rules.
         *
         * @param event
         *         event to dispatch
         * @param narrowDownToIds
         *         optional set of identifiers of the targets to narrow down the event targets
         * @return the set of identifiers to which the event was actually dispatched
         */
        Set<I> perform(Event event, @Nullable Set<I> narrowDownToIds);
    }
}
