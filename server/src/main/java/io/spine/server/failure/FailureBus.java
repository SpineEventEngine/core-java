/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.failure;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.core.Failure;
import io.spine.core.FailureClass;
import io.spine.core.FailureEnvelope;
import io.spine.core.FailureId;
import io.spine.core.IsSent;
import io.spine.grpc.StreamObservers;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageHandler;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.outbus.CommandOutputBus;
import io.spine.server.outbus.OutputDispatcherRegistry;
import io.spine.validate.MessageInvalid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches the business failures that occur during the command processing
 * to the corresponding subscriber.
 *
 * @author Alexander Yevsyuov
 * @author Alex Tymchenko
 * @see io.spine.base.ThrowableMessage
 * @see io.spine.core.Failures
 * @see io.spine.core.Subscribe @Subscribe
 */
public class FailureBus extends CommandOutputBus<Failure,
                                                 FailureEnvelope,
                                                 FailureClass,
                                                 FailureDispatcher> {

    private final Deque<BusFilter<FailureEnvelope>> filters;

    /**
     * Creates a new instance according to the pre-configured {@code Builder}.
     */
    private FailureBus(Builder builder) {
        super(checkNotNull(builder.dispatcherFailureDelivery));
        this.filters = builder.getFilters();
    }

    /**
     * Creates a new builder for the {@code FailureBus}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs no action.
     */
    @Override
    protected void store(Iterable<Failure> message) {
        // do nothing for now.
    }

    /**
     * Always returns the original {@code Failure}, as the enrichment is not supported
     * for the business failures yet.
     *
     * @param originalMessage the business failure to enrich
     * @return the same message
     */
    @Override
    protected Failure enrich(Failure originalMessage) {
        return originalMessage;
    }

    @Override
    protected OutputDispatcherRegistry<FailureClass, FailureDispatcher> createRegistry() {
        return new FailureDispatcherRegistry();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for this method.
    @Override
    protected Deque<BusFilter<FailureEnvelope>> createFilterChain() {
        return filters;
    }

    @Override
    protected IdConverter<FailureEnvelope> getIdConverter() {
        return FailureIdConverter.INSTANCE;
    }

    @Override
    protected FailureEnvelope toEnvelope(Failure message) {
        final FailureEnvelope result = FailureEnvelope.of(message);
        return result;
    }

    @Override
    protected DeadMessageHandler<FailureEnvelope> getDeadMessageHandler() {
        return DeadFailureHandler.INSTANCE;
    }

    @Override
    protected EnvelopeValidator<FailureEnvelope> getValidator() {
        return NoOpValidator.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected FailureDispatcherRegistry registry() {
        return (FailureDispatcherRegistry) super.registry();
    }

    @VisibleForTesting
    Set<FailureDispatcher> getDispatchers(FailureClass failureClass) {
        return registry().getDispatchers(failureClass);
    }

    @VisibleForTesting
    boolean hasDispatchers(FailureClass failureClass) {
        return registry().hasDispatchersFor(failureClass);
    }

    /**
     * Exposes the associated message delivery strategy to tests.
     */
    @VisibleForTesting
    @Override
    protected DispatcherFailureDelivery delivery() {
        return (DispatcherFailureDelivery) super.delivery();
    }

    /**
     * Posts the business failure to this bus instance.
     *
     * <p>This method should be used if the callee does not need to follow the
     * acknowledgement responses. Otherwise, an
     * {@linkplain #post(Message, StreamObserver) alternative method} should be used.
     *
     * @param failure the business failure to deliver to the dispatchers.
     * @see #post(Message, StreamObserver)
     */
    public final void post(Failure failure) {
        post(failure, StreamObservers.<IsSent>noOpObserver());
    }

    /** The {@code Builder} for {@code FailureBus}. */
    public static class Builder extends AbstractBuilder<FailureEnvelope, Failure, Builder> {

        /**
         * Optional {@code DispatcherFailureDelivery} for calling the dispatchers.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        @Nullable
        private DispatcherFailureDelivery dispatcherFailureDelivery;

        private Builder() {
            super();
            // Prevent direct instantiation.
        }

        /**
         * Sets a {@code DispatcherFailureDelivery} to be used for the failure delivery
         * to the dispatchers in the {@code FailureBus} being built.
         *
         * <p>If the {@code DispatcherFailureDelivery} is not set,
         * {@linkplain  DispatcherFailureDelivery#directDelivery() direct delivery} will be used.
         */
        public Builder setDispatcherFailureDelivery(DispatcherFailureDelivery delivery) {
            this.dispatcherFailureDelivery = checkNotNull(delivery);
            return this;
        }

        public Optional<DispatcherFailureDelivery> getDispatcherFailureDelivery() {
            return Optional.fromNullable(dispatcherFailureDelivery);
        }

        @Override
        public FailureBus build() {
            if(dispatcherFailureDelivery == null) {
                dispatcherFailureDelivery = DispatcherFailureDelivery.directDelivery();
            }

            return new FailureBus(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    /**
     * Generates an {@link UnhandledFailureException} upon a dead
     * message.
     */
    private enum DeadFailureHandler implements DeadMessageHandler<FailureEnvelope> {
        INSTANCE;

        @Override
        public UnhandledFailureException handleDeadMessage(FailureEnvelope envelope) {
            final Message message = envelope.getMessage();
            final UnhandledFailureException exception = new UnhandledFailureException(message);
            return exception;
        }
    }

    /**
     * Performs no validation and reports the given message valid.
     */
    private enum NoOpValidator implements EnvelopeValidator<FailureEnvelope> {
        INSTANCE;

        @Override
        public Optional<MessageInvalid> validate(FailureEnvelope envelope) {
            checkNotNull(envelope);
            return absent();
        }
    }

    private enum FailureIdConverter implements Bus.IdConverter<FailureEnvelope> {
        INSTANCE;

        @Override
        public FailureId apply(@Nullable FailureEnvelope input) {
            checkNotNull(input);
            return input.getId();
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FailureBus.class);
    }

    @VisibleForTesting
    static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
