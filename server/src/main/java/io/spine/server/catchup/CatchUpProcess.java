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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.ServerEnvironment;
import io.spine.server.catchup.command.FinalizeCatchUp;
import io.spine.server.catchup.event.CatchUpCompleted;
import io.spine.server.catchup.event.CatchUpFinalized;
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
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.protobuf.util.Durations.fromMillis;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A process that performs a projection catch-up.
 */
public class CatchUpProcess extends AbstractEventReactor {

    //TODO:2019-11-29:alex.tymchenko: consider making this configurable via the `ServerEnvironment`.
    private static final EventStreamQuery.Limit LIMIT = limitOf(500);
    private static final TypeUrl TYPE = TypeUrl.from(CatchUp.getDescriptor());

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
    CatchUpStarted handle(CatchUpRequested event) {
        Timestamp when = event.getRequest()
                              .getSinceWhen();
        builder.setStatus(CatchUpStatus.STARTED)
               .setWhenLastRead(withWindow(when))
               .setRequest(event.getRequest());
        return started(event.getId());
    }

    private static Timestamp withWindow(Timestamp when) {
        return subtract(when, fromMillis(1));
    }

    //TODO:2019-11-28:alex.tymchenko: maintain inclusiveness.
    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled>
    handle(CatchUpStarted event, EventContext context) {
        Event firstEvent = null;
        int currentRound = builder.getCurrentRound();
        if (currentRound == 0) {
            EventFactory factory = EventFactory.forImport(context.actorContext(), producerId());
            firstEvent = factory.createEvent(event, null);
        }
        return recallMoreEvents(event.getId(), firstEvent);
    }

    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> handle(HistoryEventsRecalled event) {
        return recallMoreEvents(event.getId(), null);
    }

    private EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled>
    recallMoreEvents(CatchUpId id, @Nullable Event toPostFirst) {
        CatchUp.Request request = builder.getRequest();
        List<Event> events = new ArrayList<>();
        if(toPostFirst != null) {
            events.add(toPostFirst);
        }
        List<Event> readInThisRound = readMore(request, builder.getLastRead());
        events.addAll(readInThisRound);

        if (events.isEmpty()) {
            return EitherOf2.withB(fullyRecalled(id));
        }
        //TODO:2019-12-04:alex.tymchenko: handle the events with the same timestamp.
        Event lastEvent = events.get(events.size() - 1);
        Timestamp lastEventTimestamp = lastEvent.getContext()
                                                .getTimestamp();
        builder.setWhenLastRead(withWindow(lastEventTimestamp));
        builder.setLastRead(lastEvent.getId());

        dispatchAll(request, events);

        int nextRound = builder.getCurrentRound() + 1;
        builder.setCurrentRound(nextRound);

        return EitherOf2.withA(recalled(id));
    }

    private static HistoryEventsRecalled recalled(CatchUpId id) {
        return HistoryEventsRecalled.newBuilder()
                                    .setId(id)
                                    .vBuild();
    }

    private static HistoryFullyRecalled fullyRecalled(CatchUpId id) {
        return HistoryFullyRecalled.newBuilder()
                                   .setId(id)
                                   .vBuild();
    }

    @React
    EitherOf2<LiveEventsPickedUp, CatchUpCompleted> handle(HistoryFullyRecalled command) {
        CatchUpId id = command.getId();
        builder.setStatus(CatchUpStatus.FINALIZING);

        CatchUp.Request request = builder.getRequest();
        List<Event> events = readMore(request, builder.getLastRead());

        if (events.isEmpty()) {
            return EitherOf2.withB(completeProcess(id));
        }
        dispatchAll(request, events);
        return EitherOf2.withA(liveEventsPickedUp(id));
    }

    @React
    CatchUpCompleted on(CatchUpFinalized event) {
        return completeProcess(event.getId());
    }

    private CatchUpCompleted completeProcess(CatchUpId id) {
        builder.setStatus(CatchUpStatus.COMPLETED);
        return catchUpCompleted(id);
    }

    private void dispatchAll(CatchUp.Request request, List<Event> events) {
        Set<Object> ids = targetIdsFrom(request);
        ProjectionRepository<Object, ?, ?> targetRepo = projectionRepoFor(request);

        for (Event event : events) {
            targetRepo.dispatchCatchingUp(event, ids);
        }
    }

    private List<Event> readMore(CatchUp.Request request, EventId afterEvent) {
        EventStreamQuery query = toEventQuery(request);
        MemoizingObserver<Event> observer = new MemoizingObserver<>();
        eventStore.read(query, observer);
        List<Event> allEvents = observer.responses();
        for (int index = 0; index < allEvents.size(); index++) {
            Event event = allEvents.get(index);
            if (event.getId()
                     .equals(afterEvent)) {
                int lastIndex = allEvents.size() - 1;
                if(index == lastIndex) {
                    return ImmutableList.of();
                }
                return allEvents.subList(index + 1, lastIndex);
            }
        }

        return allEvents;
    }

    private static LiveEventsPickedUp liveEventsPickedUp(CatchUpId id) {
        return LiveEventsPickedUp.newBuilder()
                                 .setId(id)
                                 .vBuild();
    }

    private static CatchUpCompleted catchUpCompleted(CatchUpId id) {
        return CatchUpCompleted.newBuilder()
                               .setId(id)
                               .vBuild();
    }

    private static CatchUpId targetOf(Message message) {
        return ((CatchUpSignal) message).getId();
    }

    private static Set<Object> targetIdsFrom(CatchUp.Request request) {
        List<Any> packedIds = request.getTargetList();
        return packedIds.stream()
                        .map(Identifier::unpack)
                        .collect(Collectors.toSet());
    }

    private ProjectionRepository<Object, ?, ?> projectionRepoFor(CatchUp.Request request) {
        TypeUrl projectionStateType = TypeUrl.parse(request.getProjectionType());
        return repositoryLocator.apply(projectionStateType);
    }

    private EventStreamQuery toEventQuery(CatchUp.Request request) {
        ImmutableList<EventFilter> filters = toFilters(request.getEventTypeList());
        Timestamp readAfter = builder.getWhenLastRead();
        return EventStreamQuery.newBuilder()
                               .setAfter(readAfter)
                               .addAllFilter(filters)
                               .setLimit(LIMIT)
                               .vBuild();
    }

    private static ImmutableList<EventFilter> toFilters(ProtocolStringList rawEventTypes) {
        return rawEventTypes.stream()
                            .map(type -> EventFilter
                                    .newBuilder()
                                    .setEventType(type)
                                    .build())
                            .collect(toImmutableList());
    }

    private static FinalizeCatchUp finalize(CatchUpId id) {
        return FinalizeCatchUp.newBuilder()
                              .setId(id)
                              .vBuild();
    }

    private static CatchUpStarted started(CatchUpId id) {
        return CatchUpStarted.newBuilder()
                             .setId(id)
                             .vBuild();
    }

    private static EventStreamQuery.Limit limitOf(int value) {
        return EventStreamQuery.Limit.newBuilder()
                                     .setValue(value)
                                     .build();
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
