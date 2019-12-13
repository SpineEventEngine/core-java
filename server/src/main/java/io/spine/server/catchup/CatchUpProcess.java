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

package io.spine.server.catchup;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.ServerEnvironment;
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
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.server.catchup.CatchUpMessages.catchUpCompleted;
import static io.spine.server.catchup.CatchUpMessages.fullyRecalled;
import static io.spine.server.catchup.CatchUpMessages.limitOf;
import static io.spine.server.catchup.CatchUpMessages.liveEventsPickedUp;
import static io.spine.server.catchup.CatchUpMessages.recalled;
import static io.spine.server.catchup.CatchUpMessages.started;
import static io.spine.server.catchup.CatchUpMessages.targetIdsFrom;
import static io.spine.server.catchup.CatchUpMessages.targetOf;
import static io.spine.server.catchup.CatchUpMessages.toFilters;
import static io.spine.server.catchup.CatchUpMessages.withWindow;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * A process that performs a projection catch-up.
 */
public class CatchUpProcess extends AbstractEventReactor {

    //TODO:2019-11-29:alex.tymchenko: consider making this configurable via the `ServerEnvironment`.
    private static final EventStreamQuery.Limit LIMIT = limitOf(500);
    private static final TypeUrl TYPE = TypeUrl.from(CatchUp.getDescriptor());

    //TODO:2019-12-13:alex.tymchenko: make this configurable.
    private final Duration turbulencePeriod = Durations.fromMillis(500);
    private final EventStore eventStore;
    private final RepositoryLocator repositoryLocator;
    private final Inbox<CatchUpId> inbox;
    private final CatchUpStorage storage;

    private CatchUp.Builder builder = null;

    public CatchUpProcess(EventStore eventStore, RepositoryLocator locator) {
        this.eventStore = eventStore;
        this.repositoryLocator = locator;
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        this.storage = delivery.catchUpStorage();
        this.inbox = configureInbox(delivery);
    }

    private Inbox<CatchUpId> configureInbox(Delivery delivery) {
        Inbox.Builder<CatchUpId> builder = delivery.newInbox(TYPE);
        builder.addEventEndpoint(InboxLabel.REACT_UPON_EVENT, EventEndpoint::new);

        return builder.build();
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(EventEnvelope event) {
        CatchUpId target = targetOf(event.message());
        inbox.send(event)
             .toReactor(target);
    }

    @React
    EitherOf2<CatchUpStarted, HistoryFullyRecalled> handle(CatchUpRequested event) {
        CatchUpId id = event.getId();
        Timestamp sinceWhen = event.getRequest()
                                   .getSinceWhen();
        builder.setWhenLastRead(withWindow(sinceWhen))
               .setRequest(event.getRequest());
        Timestamp turbulenceStart = turbulenceStart();
        if (Timestamps.compare(sinceWhen, turbulenceStart) >= 0) {
            builder.setStatus(CatchUpStatus.FINALIZING);
            return EitherOf2.withB(fullyRecalled(id));
        } else {
            builder.setStatus(CatchUpStatus.STARTED);
            return EitherOf2.withA(started(id));
        }
    }

    private Timestamp turbulenceStart() {
        return subtract(Time.currentTime(), turbulencePeriod);
    }

    //TODO:2019-11-28:alex.tymchenko: maintain inclusiveness.
    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled>
    handle(CatchUpStarted event, EventContext context) {
        Event firstEvent = null;
        int currentRound = builder.getCurrentRound();
        if (currentRound == 0) {
            firstEvent = wrapAsEvent(event, context);
        }
        return recallMoreEvents(event.getId(), firstEvent);
    }

    private Event wrapAsEvent(CatchUpSignal event, EventContext context) {
        Event firstEvent;
        EventFactory factory = EventFactory.forImport(context.actorContext(), producerId());
        firstEvent = factory.createEvent(event, null);
        return firstEvent;
    }

    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> handle(HistoryEventsRecalled event) {
        return recallMoreEvents(event.getId(), null);
    }

    private EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled>
    recallMoreEvents(CatchUpId id, @Nullable Event toPostFirst) {
        log("Recalling more events...");
        CatchUp.Request request = builder.getRequest();
        List<Event> events = new ArrayList<>();
        if (toPostFirst != null) {
            events.add(toPostFirst);
        }
        log("Reading the events starting from " + builder.getLastOrderRead());
        List<Event> readInThisRound = readMore(request, builder.getLastOrderRead(),
                                               turbulenceStart()
        );
        events.addAll(readInThisRound);

        log(format("Recalled %s events.", readInThisRound.size()));
        if (events.isEmpty()) {
            return EitherOf2.withB(fullyRecalled(id));
        }
        if (!readInThisRound.isEmpty()) {
            Event lastEvent = events.get(events.size() - 1);
            Timestamp lastEventTimestamp = lastEvent.getContext()
                                                    .getTimestamp();
            builder.setWhenLastRead(withWindow(lastEventTimestamp));
            builder.setLastOrderRead(lastEvent.getContext()
                                              .getOrder());
        }

        log(format("Dispatching %s events.", events.size()));
        dispatchAll(request, events);

        int nextRound = builder.getCurrentRound() + 1;
        builder.setCurrentRound(nextRound);

        return EitherOf2.withA(recalled(id));
    }

    @React
    EitherOf2<LiveEventsPickedUp, CatchUpCompleted>
    handle(HistoryFullyRecalled event, EventContext context) {
        CatchUpId id = event.getId();
        log("Finalizing the catch up.");
        builder.setStatus(CatchUpStatus.FINALIZING);
        commitState();

        CatchUp.Request request = builder.getRequest();
        log("Reading the events since " + builder.getWhenLastRead());
        List<Event> events = readMore(request, builder.getLastOrderRead(), null);

        if (events.isEmpty()) {
            return EitherOf2.withB(completeProcess(id, context));
        }
        dispatchAll(request, events);
        return EitherOf2.withA(liveEventsPickedUp(id));
    }

    private void commitState() {
        storage.write(builder.vBuild());
    }

    @React
    CatchUpCompleted on(LiveEventsPickedUp event, EventContext context) {
        return completeProcess(event.getId(), context);
    }

    //TODO:2019-12-13:alex.tymchenko: consider handling this event later to delete the process.
    private CatchUpCompleted completeProcess(CatchUpId id, EventContext originContext) {
        log("The catch up completed.");
        builder.setStatus(CatchUpStatus.COMPLETED);
        commitState();
        CatchUpCompleted completed = catchUpCompleted(id);
        Event event = wrapAsEvent(completed, originContext);
        dispatchAll(builder.getRequest(), ImmutableList.of(event));
        return completed;
    }

    private void dispatchAll(CatchUp.Request request, List<Event> events) {
        Set<Object> ids = targetIdsFrom(request);
        ProjectionRepository<Object, ?, ?> targetRepo = projectionRepoFor(request);

        for (Event event : events) {

            EventContext context = event.getContext();
            log("Dispatching: "
                        + context.getTimestamp()
                                 .getNanos()
                        + " -> " + context.getOrder());
            targetRepo.dispatchCatchingUp(event, ids);
        }
    }

    private static void log(String value) {
        System.out.println(value);
    }

    private List<Event> readMore(CatchUp.Request request, int afterEvent,
                                 @Nullable Timestamp readBefore) {
        if (readBefore != null) {
            if (Timestamps.compare(readBefore, builder.getWhenLastRead()) >= 0) {
                //TODO:2019-12-13:alex.tymchenko: looks an `IllegalStateException` though.
                return ImmutableList.of();
            }
        }
        EventStreamQuery query = toEventQuery(request, readBefore);
        MemoizingObserver<Event> observer = new MemoizingObserver<>();
        eventStore.read(query, observer);
        List<Event> allEvents = observer.responses();
        printEvents(allEvents);
        for (int index = 0; index < allEvents.size(); index++) {
            Event event = allEvents.get(index);
            if (event.getContext()
                     .getOrder() == afterEvent) {
                int lastIndex = allEvents.size() - 1;
                if (index == lastIndex) {
                    return ImmutableList.of();
                }
                return allEvents.subList(index + 1, lastIndex + 1);
            }
        }

        return allEvents;
    }

    private static void printEvents(List<Event> allEvents) {
        log(format("There were %s events read in total. Props:",
                   allEvents.size()));
        for (Event event : allEvents) {
            EventContext context = event.getContext();
            log(context.getTimestamp()
                       .getNanos() + " -> " + context.getOrder());
        }
    }

    private ProjectionRepository<Object, ?, ?> projectionRepoFor(CatchUp.Request request) {
        TypeUrl projectionStateType = TypeUrl.parse(request.getProjectionType());
        return repositoryLocator.apply(projectionStateType);
    }

    private EventStreamQuery toEventQuery(CatchUp.Request request, @Nullable Timestamp readBefore) {
        ImmutableList<EventFilter> filters = toFilters(request.getEventTypeList());
        Timestamp readAfter = builder.getWhenLastRead();
        EventStreamQuery.Builder builder =
                EventStreamQuery.newBuilder()
                                .setAfter(readAfter)
                                .addAllFilter(filters)
                                .setLimit(LIMIT);
        if (readBefore != null) {
            builder.setBefore(readBefore);
        }
        return builder.vBuild();
    }

    @FunctionalInterface
    public interface RepositoryLocator
            extends Function<TypeUrl, ProjectionRepository<Object, ?, ?>> {

    }

    private final class EventEndpoint implements MessageEndpoint<CatchUpId, EventEnvelope> {

        private final EventEnvelope envelope;

        EventEndpoint(EventEnvelope envelope) {
            this.envelope = envelope;
        }

        @Override
        public void dispatchTo(CatchUpId targetId) {
            load(targetId);
            CatchUpProcess.super.dispatch(envelope);
            store();
        }

        @Override
        public final void onDuplicate(CatchUpId target, EventEnvelope envelope) {
            // do nothing.
        }

        @Override
        public Repository<CatchUpId, ?> repository() {
            throw newIllegalStateException("`AbstractCommander`s have no repository.");
        }

        private void store() {
            CatchUp modifiedState = builder.vBuild();
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
