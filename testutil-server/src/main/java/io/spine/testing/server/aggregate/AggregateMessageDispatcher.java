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
package io.spine.testing.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.RejectionEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateCommandEndpoint;
import io.spine.server.aggregate.AggregateEventEndpoint;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.EntityLifecycle;
import io.spine.testing.server.NoOpLifecycle;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A test utility to dispatch commands to an {@code Aggregate} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class AggregateMessageDispatcher {

    /** Prevents instantiation of this utility class. */
    private AggregateMessageDispatcher() {
    }

    /**
     * Dispatches the {@linkplain CommandEnvelope command envelope} and applies the resulting events
     * to the given {@code Aggregate}.
     *
     * @return the list of event messages.
     */
    @CanIgnoreReturnValue
    public static List<? extends Message> dispatchCommand(Aggregate<?, ?, ?> aggregate,
                                                          CommandEnvelope envelope) {
        checkNotNull(envelope);

        List<? extends Message> eventMessages =
                TestAggregateCommandEndpoint.dispatch(aggregate, envelope);
        return eventMessages;
    }

    /**
     * Dispatches the {@linkplain EventEnvelope event envelope} and applies the resulting events
     * to the given {@code Aggregate}.
     *
     * @return the list of event messages.
     */
    @CanIgnoreReturnValue
    public static List<? extends Message> dispatchEvent(Aggregate<?, ?, ?> aggregate,
                                                        EventEnvelope envelope) {
        checkNotNull(envelope);

        List<? extends Message> eventMessages =
                TestAggregateEventEndpoint.dispatch(aggregate, envelope);
        return eventMessages;
    }

    /**
     * Dispatches the {@linkplain RejectionEnvelope rejection envelope} and applies the
     * resulting events to the given {@code Aggregate}.
     *
     * @return the list of event messages.
     */
    @CanIgnoreReturnValue
    public static List<? extends Message> dispatchRejection(Aggregate<?, ?, ?> aggregate,
                                                            RejectionEnvelope envelope) {
        throw new UnsupportedOperationException("Method dispatchRejection is not implemented!");
    }

    /**
     * A test-only implementation of an {@link AggregateEventEndpoint}, that dispatches
     * events to an instance of {@code Aggregate} into its reactor methods and returns
     * the list of produced events.
     *
     * @param <I> the type of {@code Aggregate} identifier
     * @param <A> the type of {@code Aggregate}
     */
    private static class TestAggregateEventEndpoint<I, A extends Aggregate<I, ?, ?>>
            extends AggregateEventEndpoint<I, A> {

        @SuppressWarnings("ConstantConditions")     /*  {@code null} is supplied to the ctor,
                                                        since in the workflow of this test endpoint
                                                        the repository is not used. */
        private TestAggregateEventEndpoint(EventEnvelope envelope) {
            super(mockRepository(), envelope);
        }

        private static <I, A extends Aggregate<I, ?, ?>>
        List<? extends Message> dispatch(A aggregate, EventEnvelope envelope) {
            TestAggregateEventEndpoint<I, A> endpoint =
                    new TestAggregateEventEndpoint<>(envelope);
            List<? extends Message> result = endpoint.dispatchInTx(aggregate)
                                                     .stream()
                                                     .map(Events::getMessage)
                                                     .collect(toList());
            return result;
        }
    }

    /**
     * A test-only implementation of an {@link AggregateCommandEndpoint}, that dispatches
     * commands to an instance of {@code Aggregate} and returns the list of produced events.
     *
     * @param <I> the type of {@code Aggregate} identifier
     * @param <A> the type of {@code Aggregate}
     */
    private static class TestAggregateCommandEndpoint<I, A extends Aggregate<I, ?, ?>>
            extends AggregateCommandEndpoint<I, A> {

        @SuppressWarnings("ConstantConditions")     /*  {@code null} is supplied to the ctor,
                                                        since in the workflow of this test endpoint
                                                        the repository is not used. */
        private TestAggregateCommandEndpoint(CommandEnvelope envelope) {
            super(mockRepository(), envelope);
        }

        private static <I, A extends Aggregate<I, ?, ?>>
        List<? extends Message> dispatch(A aggregate, CommandEnvelope envelope) {
            TestAggregateCommandEndpoint<I, A> endpoint =
                    new TestAggregateCommandEndpoint<>(envelope);
            List<? extends Message> result = endpoint.dispatchInTx(aggregate)
                                                     .stream()
                                                     .map(Events::getMessage)
                                                     .collect(toList());
            return result;
        }
    }

    @SuppressWarnings("unchecked") // It is OK when mocking
    private static <I, A extends Aggregate<I, ?, ?>> AggregateRepository<I, A> mockRepository() {
        TestAggregateRepository mockedRepo = mock(TestAggregateRepository.class);
        when(mockedRepo.lifecycleOf(any())).thenCallRealMethod();
        return mockedRepo;
    }

    /**
     * Test-only aggregate repository that exposes {@code Repository.Lifecycle} class.
     */
    private static class TestAggregateRepository<I, A extends Aggregate<I, ?, ?>>
            extends AggregateRepository<I, A> {

        @Override
        protected EntityLifecycle lifecycleOf(I id) {
            return NoOpLifecycle.instance();
        }
    }
}
