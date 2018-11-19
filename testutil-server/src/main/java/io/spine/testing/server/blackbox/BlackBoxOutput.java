/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.client.QueryResponse;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.QueryService;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventStreamQuery;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.server.blackbox.verify.state.VerifyState;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.testing.client.TestActorRequestFactory.newInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An output of a {@link BlackBoxBoundedContext}, which provides emitted domain messages.
 */
@VisibleForTesting
public final class BlackBoxOutput {

    private final BoundedContext boundedContext;
    private final EventBus eventBus;
    private final CommandMemoizingTap commandTap;
    private final MemoizingObserver<Ack> observer;

    BlackBoxOutput(BoundedContext boundedContext,
                   CommandMemoizingTap commandTap,
                   MemoizingObserver<Ack> observer) {
        this.boundedContext = checkNotNull(boundedContext);
        this.eventBus = checkNotNull(boundedContext.getEventBus());
        this.commandTap = checkNotNull(commandTap);
        this.observer = checkNotNull(observer);
    }

    /** Obtains all commands posted to a command bus. */
    public EmittedCommands emittedCommands() {
        List<Command> commands = allCommands();
        return new EmittedCommands(commands);
    }

    /** Obtains acknowledgements of {@linkplain #emittedCommands() emitted commands}. */
    public Acknowledgements commandAcks() {
        return new Acknowledgements(observer.responses());
    }

    /**
     * Reads all events emitted in a bounded context.
     */
    public EmittedEvents emittedEvents() {
        MemoizingObserver<Event> queryObserver = memoizingObserver();
        eventBus.getEventStore()
                .read(allEventsQuery(), queryObserver);
        List<Event> responses = queryObserver.responses();
        return new EmittedEvents(responses);
    }

    /**
     * Reads entities of the specified type for the tenant using {@link #queryService()}.
     */
    public <T extends Message> List<T> entities(Class<T> entityType, TenantId tenantId) {
        QueryFactory queries = newInstance(VerifyState.class, tenantId).query();
        Query query = queries.all(entityType);
        return entities(entityType, query);
    }

    private <T extends Message> List<T> entities(Class<T> entityType, Query query) {
        MemoizingObserver<QueryResponse> observer = memoizingObserver();
        queryService().read(query, observer);
        assertTrue(observer.isCompleted());
        QueryResponse response = observer.firstResponse();
        ImmutableList<T> entities = response.getMessagesList()
                                            .stream()
                                            .map(state -> AnyPacker.unpack(state, entityType))
                                            .collect(toImmutableList());
        return entities;
    }

    private QueryService queryService() {
        return QueryService
                .newBuilder()
                .add(boundedContext)
                .build();
    }

    private List<Command> allCommands() {
        return commandTap.commands();
    }

    /**
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }
}
