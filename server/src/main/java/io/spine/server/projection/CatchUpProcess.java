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
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.ServerEnvironment;
import io.spine.server.catchup.CatchUp;
import io.spine.server.catchup.CatchUpId;
import io.spine.server.catchup.CatchUpSignal;
import io.spine.server.catchup.CatchUpStatus;
import io.spine.server.catchup.command.FinalizeCatchUp;
import io.spine.server.catchup.command.RecallMoreHistoryEvents;
import io.spine.server.catchup.event.CatchUpCompleted;
import io.spine.server.catchup.event.CatchUpRequested;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.delivery.CatchUpReadRequest;
import io.spine.server.delivery.CatchUpStorage;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxLabel;
import io.spine.server.delivery.MessageEndpoint;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.SignalEnvelope;
import io.spine.type.TypeUrl;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.server.delivery.InboxLabel.HANDLE_COMMAND;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A process that performs a projection catch-up.
 */
public final class CatchUpProcess extends AbstractCommander {

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
        builder.addCommandEndpoint(HANDLE_COMMAND, CommandEndpoint::new)
               .addEventEndpoint(InboxLabel.REACT_UPON_EVENT, EventEndpoint::new);

        return builder.build();
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(CommandEnvelope command) {
        CatchUpId target = targetOf(command.message());
        inbox.send(command)
             .toHandler(target);
    }

    @Override
    public void dispatchEvent(EventEnvelope event) {
        CatchUpId target = targetOf(event.message());
        inbox.send(event)
             .toReactor(target);
    }

    @Command
    RecallMoreHistoryEvents handle(CatchUpRequested event) {
        builder.setStatus(CatchUpStatus.STARTED)
               .setWhenLastRead(event.getRequest()
                                     .getSinceWhen())
               .setRequest(event.getRequest());

        return recallMore(event.getId());
    }

    //TODO:2019-11-28:alex.tymchenko: maintain inclusiveness.

    @Command
    EitherOf2<RecallMoreHistoryEvents, FinalizeCatchUp> handle(RecallMoreHistoryEvents command) {
        //TODO:2019-11-29:alex.tymchenko: build the event filters once and include it to the state?
        CatchUp.Request request = builder.getRequest();
        List<Event> events = readMore(request);
        if (events.isEmpty()) {
            return EitherOf2.withB(finalize(command.getId()));
        }
        dispatchAll(request, events);

        int nextRound = builder.getCurrentRound() + 1;
        builder.setCurrentRound(nextRound);

        return EitherOf2.withA(recallMore(command.getId()));
    }

    private void dispatchAll(CatchUp.Request request, List<Event> events) {
        Set<Object> ids = targetIdsFrom(request);
        ProjectionRepository<Object, ?, ?> targetRepo = projectionRepoFor(request);

        for (Event event : events) {
            targetRepo.dispatchCatchingUp(event, ids);
        }
    }

    private List<Event> readMore(CatchUp.Request request) {
        EventStreamQuery query = toEventQuery(request);
        MemoizingObserver<Event> observer = new MemoizingObserver<>();
        eventStore.read(query, observer);
        return observer.responses();
    }

    //TODO:2019-12-03:alex.tymchenko: deal with this handler?
    @Assign
    CatchUpCompleted handle(FinalizeCatchUp command) {
        builder.setStatus(CatchUpStatus.FINALIZING);

        CatchUp.Request request = builder.getRequest();
        List<Event> events = readMore(request);
        if (events.isEmpty()) {
            builder.setStatus(CatchUpStatus.COMPLETED);
            return catchUpCompleted(command.getId());
        } else {
            dispatchAll(request, events);
        }

        //TODO:2019-12-03:alex.tymchenko: return `CatchUpFinalized`.
        return catchUpCompleted(command.getId());
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

    private static RecallMoreHistoryEvents recallMore(CatchUpId id) {
        return RecallMoreHistoryEvents.newBuilder()
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

    private abstract class AbstractCommandEndpoint<I, E extends SignalEnvelope<?, ?, ?>>
            implements MessageEndpoint<I, E> {

        private final E envelope;

        AbstractCommandEndpoint(E envelope) {
            this.envelope = envelope;
        }

        @Override
        public final void onDuplicate(I target, E envelope) {
            // do nothing.
        }

        @Override
        public Repository<I, ?> repository() {
            throw newIllegalStateException("`AbstractCommander`s have no repository.");
        }

        protected final E envelope() {
            return envelope;
        }

        protected final void store() {
            CatchUp modifiedState = builder.vBuild();
            storage.write(modifiedState);
        }

        protected final void load(CatchUpId target) {
            CatchUpReadRequest request = new CatchUpReadRequest(target);
            builder = storage.read(request)
                             .orElse(CatchUp.newBuilder()
                                            .setId(target)
                                            .buildPartial())
                             .toBuilder();
        }
    }

    private final class CommandEndpoint extends AbstractCommandEndpoint<CatchUpId, CommandEnvelope> {

        private CommandEndpoint(CommandEnvelope envelope) {
            super(envelope);
        }

        @Override
        public void dispatchTo(CatchUpId targetId) {
            load(targetId);
            CatchUpProcess.super.dispatch(envelope());
            store();
        }
    }

    private final class EventEndpoint extends AbstractCommandEndpoint<CatchUpId, EventEnvelope> {

        EventEndpoint(EventEnvelope envelope) {
            super(envelope);
        }

        @Override
        public void dispatchTo(CatchUpId targetId) {
            load(targetId);
            CatchUpProcess.super.dispatchEvent(envelope());
            store();

        }
    }
}
