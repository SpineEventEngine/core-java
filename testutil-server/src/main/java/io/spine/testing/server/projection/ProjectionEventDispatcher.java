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
package io.spine.testing.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionEndpoint;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.NoOpLifecycle;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.Any.pack;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A test utility for dispatching events to a {@code Projection} in test purposes.
 */
@VisibleForTesting
public class ProjectionEventDispatcher {

    /** Prevents instantiation from outside. */
    private ProjectionEventDispatcher() {
    }

    /**
     * Dispatches the {@code Event} to the given {@code Projection}.
     */
    public static void dispatch(Projection<?, ?, ?> projection, Event event) {
        checkNotNull(projection);
        checkNotNull(event);
        EventEnvelope envelope = EventEnvelope.of(event);
        TestProjectionEndpoint.dispatch(projection, envelope);
    }

    /**
     * Dispatches the passed {@code Event} message along with its context
     * to the given {@code Projection}.
     */
    public static void dispatch(Projection<?, ?, ?> projection,
                                EventMessage eventMessage,
                                EventContext eventContext) {
        checkNotNull(projection);
        checkNotNull(eventMessage);
        checkNotNull(eventContext);

        Event event = Event.newBuilder()
                           .setId(Events.generateId())
                           .setMessage(pack(eventMessage))
                           .setContext(eventContext)
                           .build();
        TestProjectionEndpoint.dispatch(projection, EventEnvelope.of(event));
    }

    private static class TestProjectionEndpoint<I, P extends Projection<I, S, ?>, S extends Message>
            extends ProjectionEndpoint<I, P> {

        private TestProjectionEndpoint(EventEnvelope event) {
            super(mockRepository(), event);
        }

        private static <I, P extends Projection<I, S, ?>, S extends Message> void
        dispatch(P projection, EventEnvelope event) {
            TestProjectionEndpoint<I, P, S> endpoint = new TestProjectionEndpoint<>(event);
            endpoint.runTransactionFor(projection);
        }

        @SuppressWarnings("unchecked") // It is OK when mocking
        private static <I, P extends Projection<I, S, ?>, S extends Message>
        ProjectionRepository<I, P, S> mockRepository() {
            TestProjectionRepository mockedRepo = mock(TestProjectionRepository.class);
            when(mockedRepo.lifecycleOf(any())).thenCallRealMethod();
            when(mockedRepo.idClass()).thenReturn(Object.class);
            return mockedRepo;
        }
    }

    /**
     * Test-only projection repository that exposes {@code Repository.Lifecycle} class.
     */
    private static class TestProjectionRepository<I,
                                                  P extends Projection<I, S, ?>,
                                                  S extends Message>
            extends ProjectionRepository<I, P, S> {

        @Override
        protected EntityLifecycle lifecycleOf(I id) {
            return NoOpLifecycle.instance();
        }
    }
}
