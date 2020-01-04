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

package io.spine.server.projection;

import com.google.common.collect.ImmutableList;
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
import io.spine.server.delivery.CatchUpReadRequest;
import io.spine.server.delivery.CatchUpStorage;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxLabel;
import io.spine.server.delivery.MessageEndpoint;
import io.spine.server.entity.Repository;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.EventFactory;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.React;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.type.EventEnvelope;
import io.spine.string.Stringifiers;
import io.spine.type.TypeUrl;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.Validate;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.server.projection.CatchUpMessages.catchUpCompleted;
import static io.spine.server.projection.CatchUpMessages.fullyRecalled;
import static io.spine.server.projection.CatchUpMessages.limitOf;
import static io.spine.server.projection.CatchUpMessages.liveEventsPickedUp;
import static io.spine.server.projection.CatchUpMessages.recalled;
import static io.spine.server.projection.CatchUpMessages.started;
import static io.spine.server.projection.CatchUpMessages.targetOf;
import static io.spine.server.projection.CatchUpMessages.toFilters;
import static io.spine.server.projection.CatchUpMessages.withWindow;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * A process that performs a projection catch-up.
 */
final class CatchUpProcess<I> extends AbstractEventReactor {

    //TODO:2019-11-29:alex.tymchenko: consider making this configurable via the `ServerEnvironment`.
    private static final EventStreamQuery.Limit LIMIT = limitOf(500);
    private static final TypeUrl TYPE = TypeUrl.from(CatchUp.getDescriptor());

    private final Object endpointLock = new Object();

    //TODO:2019-12-13:alex.tymchenko: make this configurable.
    private final Duration turbulencePeriod = Durations.fromMillis(500);
    private final Supplier<EventStore> eventStore;
    private final ProjectionRepository<I, ?, ?> repository;
    private final Inbox<CatchUpId> inbox;
    private final CatchUpStorage storage;

    private CatchUp.Builder builder = CatchUp.newBuilder();

    CatchUpProcess(Supplier<EventStore> eventStore, ProjectionRepository<I, ?, ?> repository) {
        this.eventStore = eventStore;
        this.repository = repository;
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        this.storage = delivery.catchUpStorage();
        this.inbox = configureInbox(delivery);
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
    EitherOf2<CatchUpStarted, HistoryFullyRecalled> handle(CatchUpRequested e, EventContext ctx) {
        CatchUpId id = e.getId();

        String idAsString = Stringifiers.toString(Identifier.unpack(e.getRequest()
                                                                     .getTargetList()
                                                                     .get(0)));
        System.out.println('[' + idAsString + "] `CatchUpRequested` received.");

        Timestamp sinceWhen = e.getRequest()
                               .getSinceWhen();
        builder().setWhenLastRead(withWindow(sinceWhen))
                 .setRequest(e.getRequest());
//        Timestamp turbulenceStart = turbulenceStart();
        CatchUpStarted started = started(id);
//        if (Timestamps.compare(sinceWhen, turbulenceStart) >= 0) {
//            Event event = maybeWrap(started, ctx);
//            if(event != null) {
//                dispatchAll(ImmutableList.of(event));
//            }
//            builder().setStatus(CatchUpStatus.FINALIZING);
//            return EitherOf2.withB(fullyRecalled(id));
//        } else {
        builder().setStatus(CatchUpStatus.STARTED);
        commitState();

        Event event = wrapAsEvent(started, ctx);
        dispatchAll(ImmutableList.of(event));

        System.out.println('[' + idAsString + "] Returning `CatchUpStarted`.");
        builder().vBuild();

        return EitherOf2.withA(started);
//        }
    }

    private Timestamp turbulenceStart() {
        return subtract(Time.currentTime(), turbulencePeriod);
    }

    //TODO:2019-11-28:alex.tymchenko: maintain inclusiveness.
    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled>
    handle(CatchUpStarted event, EventContext context) {
        System.out.println(idTag() + " `CatchUpStarted` received.");
        return recallMoreEvents(event.getId(), null);
    }

    private @Nullable Event maybeWrap(CatchUpStarted event, EventContext context) {
        Event firstEvent = null;
        int currentRound = builder().getCurrentRound();
        if (currentRound == 0) {
            firstEvent = wrapAsEvent(event, context);
        }
        return firstEvent;
    }

    private String idTag() {
        return Thread.currentThread()
                     .getName() + " - [" + Identifier.unpack(builder().getRequest()
                                                                      .getTargetList()
                                                                      .get(0)) + ']';
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
        return recallMoreEvents(event.getId(), null);
    }

    private EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled>
    recallMoreEvents(CatchUpId id, @Nullable Event toPostFirst) {
        CatchUp.Request request = builder().getRequest();
        List<Event> events = new ArrayList<>();
        if (toPostFirst != null) {
            events.add(toPostFirst);
        }
        List<Event> readInThisRound = readMore(request, turbulenceStart(), LIMIT);

        if (!readInThisRound.isEmpty()) {
            List<Event> stripped = stripLastTimestamp(readInThisRound);

                Event lastEvent = stripped.get(stripped.size() - 1);
                Timestamp lastEventTimestamp = lastEvent.getContext()
                                                        .getTimestamp();
                builder().setWhenLastRead(lastEventTimestamp);
                events.addAll(stripped);
        }


        dispatchAll(events);

        int nextRound = builder().getCurrentRound() + 1;
        builder().setCurrentRound(nextRound);

        if (events.isEmpty()) {
            return EitherOf2.withB(fullyRecalled(id));
        }

        return EitherOf2.withA(recalled(id));
    }

    private static List<Event> stripLastTimestamp(List<Event> round) {
        int lastIndex = round.size() - 1;
        Event lastEvent = round.get(lastIndex);
        Timestamp lastTimestamp = lastEvent.getContext()
                                           .getTimestamp();
        for (int index = lastIndex; index >= 0; index--) {
            Event event = round.get(index);
            Timestamp timestamp = event.getContext()
                                       .getTimestamp();
            if (!timestamp.equals(lastTimestamp)) {
                List<Event> result = round.subList(0, index + 1);
                return result;
            }
        }
        return round;
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
        dispatchAll(ImmutableList.of(event));
        return completed;
    }

    private void dispatchAll(List<Event> events) {
        System.out.println(format(idTag() + " Dispatching %s events.", events.size()));
        if (events.isEmpty()) {
            return;
        }
        try {
            CatchUp.Request request = builder().getRequest();

            List<Any> packedIds = request.getTargetList();
            Set<I> ids = packedIds.stream()
                                  .map((any) -> Identifier.unpack(any, repository.idClass()))
                                  .collect(Collectors.toSet());

            for (Event event : events) {
                repository.dispatchCatchingUp(event, ids);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
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

//        for (int index = 0; index < allEvents.size(); index++) {
//            Event event = allEvents.get(index);
//            if (event.getContext()
//                     .getOrder() == afterEvent) {
//                int lastIndex = allEvents.size() - 1;
//                if (index == lastIndex) {
//                    return ImmutableList.of();
//                }
//                return allEvents.subList(index + 1, lastIndex + 1);
//            }
//        }

        return allEvents;
    }

    private void printEvents(List<Event> allEvents) {

        for (Event event : allEvents) {
            System.out.println(printEvent(event));
        }
    }

    public static String printEvent(Event event) {
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
        String expectedType = repository.entityStateType()
                                        .value();
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
}
