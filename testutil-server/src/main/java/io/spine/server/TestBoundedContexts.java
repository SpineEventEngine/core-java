/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.util.Exceptions;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;

/**
 * Utilities for tests that deal with {@link BoundedContext Bounded Context}.
 * 
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public class TestBoundedContexts {

    /**
     * Wraps the provided domain command message in a test command with a provided tenant ID.
     *
     * @param commandMessage a domain command
     * @param tenantId       an ID of a tenant in multitenant bounded context
     * @return a newly created command instance
     */
    public static Command command(Message commandMessage, TenantId tenantId) {
        return requestFactory(tenantId).command()
                                       .create(commandMessage);
    }

    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(TestBoundedContexts.class, tenantId);
    }

    /**
     * Wraps the provided domain command message in a test command.
     *
     * @param commandMessage a domain command
     * @return a new command instance
     */
    public static Command command(Message commandMessage) {
        return requestFactory().command()
                               .create(commandMessage);
    }

    private static TestActorRequestFactory requestFactory() {
        return TestActorRequestFactory.newInstance(TestBoundedContexts.class);
    }

    /**
     * @return a new {@link TenantId Tenant ID} with a random UUID value convenient
     * for test purposes.
     */
    public static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    /**
     * @return a new multitenant bounded context for test purposes
     */
    public static BoundedContext newBoundedContext() {
        return BoundedContext.newBuilder()
                             .setMultitenant(true)
                             .build();
    }

    /**
     * A convenience method for closing the bounded context in tests.
     *
     * <p>Instead of a checked {@link java.io.IOException IOException}, wraps any issues
     * that may occur while closing, into an {@link IllegalStateException}.
     *
     * @param boundedContext a bounded context to close
     */
    public static void closeContext(BoundedContext boundedContext) {
        checkNotNull(boundedContext);
        try {
            boundedContext.close();
        } catch (Exception e) {
            throw Exceptions.illegalStateWithCauseOf(e);
        }
    }

    /**
     * Reads all events from the bounded context for the provided tenant.
     */
    public static List<Event> readAllEvents(final BoundedContext boundedContext,
                                            TenantId tenantId) {
        final MemoizingObserver<Event> queryObserver = memoizingObserver();
        final TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                boundedContext.getEventBus()
                              .getEventStore()
                              .read(allEventsQuery(), queryObserver);
            }
        };
        operation.execute();

        final List<Event> responses = queryObserver.responses();
        return responses;
    }

    /**
     * @return a new {@link EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }
}
