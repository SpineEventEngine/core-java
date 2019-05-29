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
package io.spine.testing.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.TransactionListener;
import io.spine.server.procman.PmCommandEndpoint;
import io.spine.server.procman.PmEventEndpoint;
import io.spine.server.procman.PmTransaction;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.MessageEnvelope;
import io.spine.testing.server.NoOpLifecycle;

import java.util.List;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A test utility for dispatching commands and events to a {@code ProcessManager} in test purposes.
 */
@VisibleForTesting
@CanIgnoreReturnValue
public final class PmDispatcher {

    private static final
    ImmutableMap<Class<? extends MessageEnvelope>, EndpointFn> endpoints =
            ImmutableMap.<Class<? extends MessageEnvelope>, EndpointFn>
                    builder()
                    .put(CommandEnvelope.class,
                         (p, m) -> TestPmCommandEndpoint.dispatch(p, (CommandEnvelope) m))
                    .put(EventEnvelope.class,
                         (p, m) -> TestPmEventEndpoint.dispatch(p, (EventEnvelope) m))
                    .build();

    /** Prevents this utility class from instantiation. */
    private PmDispatcher() {
    }

    /**
     * Dispatches the {@linkplain CommandEnvelope envelope} to the given {@code ProcessManager}.
     *
     * @return the list of {@linkplain Event events}, being the envelope output.
     */
    @CanIgnoreReturnValue
    public static List<Event> dispatch(ProcessManager<?, ?, ?> pm, MessageEnvelope envelope) {
        checkNotNull(pm);
        checkNotNull(envelope);
        EndpointFn fn = endpoints.get(envelope.getClass());
        List<Event> events = fn.apply(pm, envelope);
        return events;
    }

    /**
     * Functional interface for an entry in the map matching message envelope
     * class with a test endpoint which dispatches such envelopes.
     * @see #endpoints
     */
    private interface EndpointFn
            extends BiFunction<ProcessManager<?, ?, ?>, MessageEnvelope, List<Event>> {
    }

    /**
     * A test-only implementation of an {@link PmCommandEndpoint}, that dispatches
     * commands to an instance of {@code ProcessManager} and returns the list of events.
     *
     * @param <I> the type of {@code ProcessManager} identifier
     * @param <P> the type of {@code ProcessManager}
     * @param <S> the type of {@code ProcessManager} state object
     */
    private static class TestPmCommandEndpoint<I,
                                               P extends ProcessManager<I, S, ?>,
                                               S extends Message>
            extends PmCommandEndpoint<I, P> {

        private TestPmCommandEndpoint(CommandEnvelope envelope) {
            super(mockRepository(), envelope);
        }

        private static <I, P extends ProcessManager<I, S, ?>, S extends Message>
        List<Event> dispatch(P manager, CommandEnvelope envelope) {
            TestPmCommandEndpoint<I, P, S> endpoint = new TestPmCommandEndpoint<>(envelope);
            List<Event> events = endpoint.runTransactionFor(manager);
            return events;
        }
    }

    /**
     * A test-only implementation of an {@link PmEventEndpoint}, that dispatches
     * events to an instance of {@code ProcessManager} and returns the list of events.
     *
     * @param <I> the type of {@code ProcessManager} identifier
     * @param <P> the type of {@code ProcessManager}
     * @param <S> the type of {@code ProcessManager} state object
     */
    private static class TestPmEventEndpoint<I,
                                             P extends ProcessManager<I, S, ?>,
                                             S extends Message>
            extends PmEventEndpoint<I, P> {

        private TestPmEventEndpoint(EventEnvelope event) {
            super(mockRepository(), event);
        }

        private static <I, P extends ProcessManager<I, S, ?>, S extends Message>
        List<Event> dispatch(P manager, EventEnvelope event) {
            TestPmEventEndpoint<I, P, S> endpoint = new TestPmEventEndpoint<>(event);
            List<Event> events = endpoint.runTransactionFor(manager);
            return events;
        }
    }

    @SuppressWarnings("unchecked") // It is OK when mocking
    private static <I, P extends ProcessManager<I, S, ?>, S extends Message>
    ProcessManagerRepository<I, P, S> mockRepository() {
        TestPmRepository mockedRepo = mock(TestPmRepository.class);
        when(mockedRepo.lifecycleOf(any())).thenCallRealMethod();
        when(mockedRepo.idClass()).thenReturn(Object.class);
        when(mockedRepo.beginTransactionFor(any())).thenCallRealMethod();
        return mockedRepo;
    }

    /**
     * Test-only process manager repository that exposes {@code Repository.Lifecycle} class.
     */
    private static class TestPmRepository<I, P extends ProcessManager<I, S, ?>, S extends Message>
            extends ProcessManagerRepository<I, P, S> {

        @SuppressWarnings("unchecked") // OK for this test implementation.
        @Override
        protected PmTransaction<?, ?, ?> beginTransactionFor(P manager) {
            PmTransaction<?, ?, ?> tx = new TestPmTransaction(manager);
            TransactionListener listener = EntityLifecycleMonitor.newInstance(this, manager.id());
            tx.setListener(listener);
            return tx;
        }

        @Override
        protected EntityLifecycle lifecycleOf(I id) {
            return NoOpLifecycle.instance();
        }
    }

    /**
     * A test-only implementation of a {@link PmTransaction} that can set the given
     * {@code state} and {@code version} as a starting point for the transaction.
     */
    static final class TestPmTransaction<I, S extends Message, B extends ValidatingBuilder<S>>
            extends PmTransaction<I, S, B> {

        TestPmTransaction(ProcessManager<I, S, B> processManager) {
            super(processManager);
        }

        TestPmTransaction(ProcessManager<I, S, B> processManager, S state, Version version) {
            super(processManager, state, version);
        }

        @Override
        protected void commit() {
            super.commit();
        }
    }
}
