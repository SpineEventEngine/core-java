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
package io.spine.testing.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionProxy;
import io.spine.server.projection.ProjectionRepository;
import io.spine.testing.server.NoOpLifecycle;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.Any.pack;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A test utility for dispatching events to a {@code Projection} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class ProjectionEventDispatcher {

    private ProjectionEventDispatcher() {
        // Prevent from instantiation.
    }

    /**
     * Dispatches the {@code Event} to the given {@code Projection}.
     */
    public static void dispatch(Projection<?, ?, ?> projection,
                                Event event) {
        checkNotNull(projection);
        checkNotNull(event);
        EventEnvelope envelope = EventEnvelope.of(event);
        TestProjectionProxy.dispatch(projection, envelope);
    }

    /**
     * Dispatches the passed {@code Event} message along with its context
     * to the given {@code Projection}.
     */
    public static void dispatch(Projection<?, ?, ?> projection,
                                Message eventMessage,
                                EventContext eventContext) {
        checkNotNull(projection);
        checkNotNull(eventMessage);
        checkNotNull(eventContext);

        Event event = Event.newBuilder()
                           .setId(Events.generateId())
                           .setMessage(pack(eventMessage))
                           .setContext(eventContext)
                           .build();
        TestProjectionProxy.dispatch(projection, EventEnvelope.of(event));
    }

    private static class TestProjectionProxy<I, P extends Projection<I, S, ?>, S extends Message>
            extends ProjectionProxy<I, P> {

        private TestProjectionProxy(I entityId) {
            super(mockRepository(), entityId);
        }

        private static <I, P extends Projection<I, S, ?>, S extends Message> void
        dispatch(P projection, EventEnvelope envelope) {
            I id = projection.getId();
            TestProjectionProxy<I, P, S> proxy = new TestProjectionProxy<>(id);
            proxy.dispatchInTx(projection, envelope);
        }
    }

    @SuppressWarnings("unchecked") // It is OK when mocking
    private static <I, P extends Projection<I, S, ?>, S extends Message>
    ProjectionRepository<I, P, S> mockRepository() {
        TestProjectionRepository mockedRepo = mock(TestProjectionRepository.class);
        when(mockedRepo.lifecycleOf(any())).thenCallRealMethod();
        return mockedRepo;
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
