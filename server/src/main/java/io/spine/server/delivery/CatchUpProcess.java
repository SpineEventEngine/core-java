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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.ServerEnvironment;
import io.spine.server.catchup.CatchUp;
import io.spine.server.catchup.CatchUpId;
import io.spine.server.catchup.CatchUpStatus;
import io.spine.server.catchup.event.CatchUpCompleted;
import io.spine.server.catchup.event.CatchUpRequested;
import io.spine.server.catchup.event.CatchUpStarted;
import io.spine.server.catchup.event.HistoryEventsRecalled;
import io.spine.server.catchup.event.HistoryFullyRecalled;
import io.spine.server.catchup.event.LiveEventsPickedUp;
import io.spine.server.delivery.event.ShardProcessingRequested;
import io.spine.server.entity.Repository;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.EventFactory;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.React;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.type.EventEnvelope;
import io.spine.string.Stringifiers;
import io.spine.type.TypeUrl;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.Validate;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.server.delivery.CatchUpMessages.catchUpCompleted;
import static io.spine.server.delivery.CatchUpMessages.fullyRecalled;
import static io.spine.server.delivery.CatchUpMessages.limitOf;
import static io.spine.server.delivery.CatchUpMessages.liveEventsPickedUp;
import static io.spine.server.delivery.CatchUpMessages.recalled;
import static io.spine.server.delivery.CatchUpMessages.started;
import static io.spine.server.delivery.CatchUpMessages.targetOf;
import static io.spine.server.delivery.CatchUpMessages.toFilters;
import static io.spine.server.delivery.CatchUpMessages.withWindow;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A process that performs a projection catch-up.
 */
public final class CatchUpProcess<I> extends AbstractEventReactor {

    //TODO:2019-11-29:alex.tymchenko: consider making this configurable via the `ServerEnvironment`.
    private static final EventStreamQuery.Limit LIMIT = limitOf(500);
    private static final TypeUrl TYPE = TypeUrl.from(CatchUp.getDescriptor());

    private final Object endpointLock = new Object();

    //TODO:2019-12-13:alex.tymchenko: make this configurable.
    private final Duration turbulencePeriod = Durations.fromMillis(500);
    private final Class<I> idClass;
    private final TypeUrl projectionStateType;
    private final Supplier<EventStore> eventStore;
    private final RepositoryIndex<I> repositoryIndex;
    private final DispatchCatchingUp<I> dispatchOperation;
    private final Inbox<CatchUpId> inbox;
    private final CatchUpStorage storage;

    private CatchUp.Builder builder = CatchUp.newBuilder();

    CatchUpProcess(CatchUpProcessBuilder<I> builder) {
        this.idClass = builder.idClass();
        this.projectionStateType = builder.projectionStateType();
        this.eventStore = builder.eventStore();
        this.repositoryIndex = builder.index();
        this.dispatchOperation = builder.dispatchOp();
        this.storage = builder.storage();
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        this.inbox = configureInbox(delivery);
    }

    public static <I> CatchUpProcessBuilder<I> newBuilder(ProjectionRepository<I, ?, ?> repo) {
        checkNotNull(repo);
        return new CatchUpProcessBuilder<>(repo.idClass(), repo.entityStateType());
    }

    private CatchUp.Builder builder() {
        return builder;
    }

    private Inbox<CatchUpId> configureInbox(Delivery delivery) {
        Inbox.Builder<CatchUpId> builder = delivery.newInbox(TYPE);
        builder.addEventEndpoint(InboxLabel.REACT_UPON_EVENT, EventEndpoint::new);
        return builder.build();
    }

    @React
    CatchUpStarted handle(CatchUpRequested e, EventContext ctx) {
        CatchUpId id = e.getId();

        CatchUp.Request request = e.getRequest();
        System.out.println('[' + idAsString() + "] `CatchUpRequested` received.");

        Timestamp sinceWhen = request
                .getSinceWhen();
        builder().setWhenLastRead(withWindow(sinceWhen))
                 .setRequest(request);
        CatchUpStarted started = started(id);
        builder().setStatus(CatchUpStatus.STARTED);
        commitState();

        Set<I> ids;
        Event event = wrapAsEvent(started, ctx);
        ids = targetsForCatchUpSignals(request);
        dispatchAll(ImmutableList.of(event), ids);

        System.out.println('[' + idAsString() + "] Returning `CatchUpStarted`.");
        builder().vBuild();

        return started;
    }

    private Set<I> targetsForCatchUpSignals(CatchUp.Request request) {
        Set<I> ids;
        List<Any> rawTargets = request.getTargetList();
        if (rawTargets.isEmpty()) {
            ids = ImmutableSet.copyOf(repositoryIndex.get());
        } else {
            ids = unpack(rawTargets);
        }
        return ids;
    }

    private Timestamp turbulenceStart() {
        return subtract(Time.currentTime(), turbulencePeriod);
    }

    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> handle(CatchUpStarted event) {
        System.out.println(idTag() + " `CatchUpStarted` received.");
        return recallMoreEvents(event.getId());
    }

    private String idTag() {
        return Thread.currentThread()
                     .getName() + " - [" + idAsString() + ']';
    }

    private String idAsString() {
        List<Any> targets = builder().getRequest()
                                     .getTargetList();
        if (targets.isEmpty()) {
            return "ALL";
        }
        return Identifier.unpack(targets.get(0))
                         .toString();
    }

    private Event wrapAsEvent(CatchUpSignal event, EventContext context) {
        Event firstEvent;
        EventFactory factory = EventFactory.forImport(context.actorContext(), producerId());
        firstEvent = factory.createEvent(event, null);
        return firstEvent;
    }

    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> handle(HistoryEventsRecalled event) {
        System.out.println(idTag() + " `HistoryEventsRecalled` received.");
        return recallMoreEvents(event.getId());
    }

    private EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> recallMoreEvents(CatchUpId id) {
        CatchUp.Request request = builder().getRequest();

        int nextRound = builder().getCurrentRound() + 1;
        builder().setCurrentRound(nextRound);

        List<Event> readInThisRound = readMore(request, turbulenceStart(), LIMIT);
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

    @React
    EitherOf2<LiveEventsPickedUp, CatchUpCompleted>
    handle(HistoryFullyRecalled event, EventContext context) {
        System.out.println(idTag() + " `HistoryFullyRecalled` received.");
        CatchUpId id = event.getId();
        builder().setStatus(CatchUpStatus.FINALIZING);
        commitState();

        CatchUp.Request request = builder().getRequest();
        List<Event> events = readMore(request, null, null);

        if (events.isEmpty()) {
            return EitherOf2.withB(completeProcess(id, context));
        }
        dispatchAll(events);
        return EitherOf2.withA(liveEventsPickedUp(id));
    }

    @React
    List<ShardProcessingRequested> on(CatchUpCompleted ignored) {
        System.out.println("Sending out the `ShardProcessingRequested` events.");
        int shardCount = builder().getTotalShards();
        List<Integer> affectedShards = builder().getAffectedShardList();
        List<ShardProcessingRequested> events = toShardProcessingEvents(shardCount, affectedShards);
        return events;
    }

    private static List<ShardProcessingRequested>
    toShardProcessingEvents(int shardCount, List<Integer> indexes) {
        return indexes
                .stream()
                .map(indexValue -> {
                    ShardIndex shardIndex =
                            ShardIndex.newBuilder()
                                      .setIndex(indexValue)
                                      .setOfTotal(shardCount)
                                      .vBuild();
                    ShardProcessingRequested event = ShardProcessingRequested.newBuilder()
                                                                             .setId(shardIndex)
                                                                             .vBuild();
                    return event;
                })
                .collect(toList());
    }

    private void commitState() {
        storage.write(builder().vBuild());
    }

    @React
    CatchUpCompleted on(LiveEventsPickedUp event, EventContext context) {
        System.out.println(idTag() + " `LiveEventsPickedUp` received.");
        return completeProcess(event.getId(), context);
    }

    //TODO:2019-12-13:alex.tymchenko: consider handling this event later to delete the process.
    private CatchUpCompleted completeProcess(CatchUpId id, EventContext originContext) {
        builder().setStatus(CatchUpStatus.COMPLETED);
        commitState();
        CatchUpCompleted completed = catchUpCompleted(id);
        Event event = wrapAsEvent(completed, originContext);
        Set<I> targets = targetsForCatchUpSignals(builder.getRequest());
        System.out.println("Going to send `CatchUpCompleted` over to the targets: "
                                   + Joiner.on(", ").join(targets));
        dispatchAll(ImmutableList.of(event), targets);
        return completed;
    }

    private void dispatchAll(List<Event> events, Set<I> targets) {
        System.out.println(format(idTag() + " Dispatching %s events.", events.size()));
        if (events.isEmpty()) {
            return;
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

    private void dispatchAll(List<Event> events) {
        System.out.println(format(idTag() + " Dispatching %s events.", events.size()));
        if (events.isEmpty()) {
            return;
        }
        CatchUp.Request request = builder().getRequest();
        List<Any> packedIds = request.getTargetList();
        if (packedIds.isEmpty()) {
            dispatchAll(events, new HashSet<>());
        } else {
            Set<I> ids = unpack(packedIds);
            dispatchAll(events, ids);
        }
    }

    private Set<I> unpack(List<Any> packedIds) {
        return packedIds.stream()
                        .map((any) -> Identifier.unpack(any, idClass))
                        .collect(toSet());
    }

    private List<Event> readMore(CatchUp.Request request,
                                 @Nullable Timestamp readBefore,
                                 EventStreamQuery.@Nullable Limit limit) {
        System.out.println(idTag() + " Preparing to read more events. "
                                   + "\n `readBefore` is " + readBefore
                                   + "\n the `sinceWhen` is " + request.getSinceWhen()
                                   + "\n the `whenLastRead` is " + builder().getWhenLastRead()
        );
        if (readBefore != null) {
            if (Timestamps.compare(readBefore, builder().getWhenLastRead()) <= 0) {
                System.out.println("Read-before is EARLIER than when-last-read!");
                //TODO:2019-12-13:alex.tymchenko: looks an `IllegalStateException` though.
                return ImmutableList.of();
            }
        }
        EventStreamQuery query = toEventQuery(request, readBefore, limit);
        MemoizingObserver<Event> observer = new MemoizingObserver<>();
        eventStore.get()
                  .read(query, observer);
        List<Event> allEvents = observer.responses();
        System.out.println(
                format(idTag() +
                               " There were %s events read in total. The first one was [ %s ], the last one was [ %s ].",
                       allEvents.size(),
                       allEvents.isEmpty() ? "" : printEvent(allEvents.get(0)),
                       allEvents.isEmpty() ? "" : printEvent(allEvents.get(allEvents.size() - 1))
                )
        );

        return allEvents;
    }

    private static String printEvent(Event event) {
        EventContext context = event.getContext();
        Timestamp timestamp = context.getTimestamp();
        String strRepresentation = timestamp.getSeconds()
                + "." + timestamp.getNanos();
        return strRepresentation;
    }

    private EventStreamQuery toEventQuery(CatchUp.Request request,
                                          @Nullable Timestamp readBefore,
                                          EventStreamQuery.@Nullable Limit limit) {
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

    @Override
    public boolean canDispatch(EventEnvelope envelope) {
        EventMessage raw = envelope.message();
        if (!(raw instanceof CatchUpSignal)) {
            return false;
        }
        CatchUpSignal asSignal = (CatchUpSignal) raw;
        String actualType = asSignal.getId()
                                    .getProjectionType();
        String expectedType = projectionStateType.value();
        return expectedType.equals(actualType);
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(EventEnvelope event) {
        CatchUpId target = targetOf(event.message());
        inbox.send(event)
             .toReactor(target);
    }

    private final class EventEndpoint implements MessageEndpoint<CatchUpId, EventEnvelope> {

        private final EventEnvelope envelope;

        private EventEndpoint(EventEnvelope envelope) {
            this.envelope = envelope;
        }

        @Override
        public void dispatchTo(CatchUpId targetId) {
            synchronized (endpointLock) {
                load(targetId);
                CatchUpProcess.super.dispatch(envelope);
                store();
            }
        }

        @Override
        public final void onDuplicate(CatchUpId target, EventEnvelope envelope) {
            // do nothing.
        }

        @Override
        public Repository<CatchUpId, ?> repository() {
            throw newIllegalStateException("`CatchUpProcess` has no repository.");
        }

        private void store() {
            CatchUp rawMsg = builder().build();
            List<ConstraintViolation> violations = Validate.violationsOf(rawMsg);
            if (!violations.isEmpty()) {
                System.err.println("The message is invalid: " + Stringifiers.toString(rawMsg));
            }
            CatchUp modifiedState = builder().vBuild();
            storage.write(modifiedState);
        }

        private void load(CatchUpId target) {
            CatchUpReadRequest request = new CatchUpReadRequest(target);
            builder = storage.read(request)
                             .orElse(CatchUp.newBuilder()
                                            .setId(target)
                                            .buildPartial())
                             .toBuilder();
        }
    }

    @FunctionalInterface
    public interface RepositoryIndex<I> extends Supplier<Set<I>> {

    }

    @FunctionalInterface
    public interface DispatchCatchingUp<I> {

        Set<I> perform(Event event, @Nullable Set<I> restrictToIds);
    }
}
