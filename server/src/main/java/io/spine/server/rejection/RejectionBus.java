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
package io.spine.server.rejection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.MessageInvalid;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.grpc.StreamObservers;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.outbus.CommandOutputBus;
import io.spine.server.outbus.OutputDispatcherRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches the business rejections that occur during the command processing
 * to the corresponding subscriber.
 *
 * @author Alexander Yevsyuov
 * @author Alex Tymchenko
 * @see io.spine.base.ThrowableMessage
 * @see Rejections
 * @see io.spine.core.Subscribe @Subscribe
 */
public class RejectionBus extends CommandOutputBus<Rejection,
                                                   RejectionEnvelope,
                                                   RejectionClass,
                                                   RejectionDispatcher<?>> {

    /** Filters applied when a rejection is posted. */
    private final Deque<BusFilter<RejectionEnvelope>> filterChain;

    /** The enricher for posted rejections or {@code null} if the enrichment is not supported. */
    @Nullable
    private final RejectionEnricher enricher;

    /**
     * Creates a new instance according to the pre-configured {@code Builder}.
     */
    private RejectionBus(Builder builder) {
        super(checkNotNull(builder.dispatcherRejectionDelivery));
        this.filterChain = builder.getFilters();
        this.enricher = builder.enricher;
    }

    /**
     * Creates a new builder for the {@code RejectionBus}.
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
    protected void store(Iterable<Rejection> message) {
        // do nothing for now.
    }

    @Override
    protected RejectionEnvelope enrich(RejectionEnvelope rejection) {
        if (enricher == null || !enricher.canBeEnriched(rejection)) {
            return rejection;
        }
        final RejectionEnvelope enriched = enricher.enrich(rejection);
        return enriched;
    }

    @Override
    protected OutputDispatcherRegistry<RejectionClass, RejectionDispatcher<?>> createRegistry() {
        return new RejectionDispatcherRegistry();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for this method.
    @Override
    protected Deque<BusFilter<RejectionEnvelope>> createFilterChain() {
        return filterChain;
    }

    @Override
    protected RejectionEnvelope toEnvelope(Rejection message) {
        final RejectionEnvelope result = RejectionEnvelope.of(message);
        return result;
    }

    @Override
    protected DeadMessageTap<RejectionEnvelope> getDeadMessageHandler() {
        return DeadRejectionTap.INSTANCE;
    }

    @Override
    protected EnvelopeValidator<RejectionEnvelope> getValidator() {
        return NoOpValidator.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected RejectionDispatcherRegistry registry() {
        return (RejectionDispatcherRegistry) super.registry();
    }

    @VisibleForTesting
    Set<RejectionDispatcher<?>> getDispatchers(RejectionClass rejectionClass) {
        return registry().getDispatchers(rejectionClass);
    }

    @VisibleForTesting
    boolean hasDispatchers(RejectionClass rejectionClass) {
        return registry().hasDispatchersFor(rejectionClass);
    }

    /**
     * Exposes the associated message delivery strategy to tests.
     */
    @VisibleForTesting
    @Override
    protected DispatcherRejectionDelivery delivery() {
        return (DispatcherRejectionDelivery) super.delivery();
    }

    /**
     * Posts the rejection to this bus instance.
     *
     * <p>This method should be used if the callee does not need to follow the
     * acknowledgement responses. Otherwise, an
     * {@linkplain #post(Message, StreamObserver) alternative method} should be used.
     *
     * @param rejection the business rejection to deliver to the dispatchers.
     * @see #post(Message, StreamObserver)
     */
    public final void post(Rejection rejection) {
        post(rejection, StreamObservers.<Ack>noOpObserver());
    }

    /** The {@code Builder} for {@code RejectionBus}. */
    public static class Builder extends AbstractBuilder<RejectionEnvelope, Rejection, Builder> {

        /**
         * Optional {@code DispatcherRejectionDelivery} for calling the dispatchers.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        @Nullable
        private DispatcherRejectionDelivery dispatcherRejectionDelivery;

        /**
         * Optional enricher for rejections.
         *
         * <p>If not set, the enrichments will NOT be supported
         * in the {@code RejectionBus} instance built.
         */
        @Nullable
        private RejectionEnricher enricher;

        /** Prevents direct instantiation. */
        private Builder() {
            super();
        }

        /**
         * Sets a {@code DispatcherRejectionDelivery} to be used for the rejection delivery
         * to the dispatchers in the {@code RejectionBus} being built.
         *
         * <p>If the {@code DispatcherRejectionDelivery} is not set,
         * {@linkplain  DispatcherRejectionDelivery#directDelivery() direct delivery} will be used.
         */
        public Builder setDispatcherRejectionDelivery(DispatcherRejectionDelivery delivery) {
            this.dispatcherRejectionDelivery = checkNotNull(delivery);
            return this;
        }

        public Optional<DispatcherRejectionDelivery> getDispatcherRejectionDelivery() {
            return Optional.fromNullable(dispatcherRejectionDelivery);
        }

        /**
         * Sets a custom {@link RejectionEnricher} for events posted to
         * the {@code RejectionBus} which is being built.
         *
         * <p>If the {@code RejectionEnricher} is not set, the enrichments
         * will <strong>NOT</strong> be supported for the {@code RejectionBus} instance built.
         *
         * @param enricher the {@code RejectionEnricher} for events or {@code null} if enrichment is
         *                 not supported
         */
        public Builder setEnricher(RejectionEnricher enricher) {
            this.enricher = enricher;
            return this;
        }

        public Optional<RejectionEnricher> getEnricher() {
            return Optional.fromNullable(enricher);
        }

        @Override
        public RejectionBus build() {
            if(dispatcherRejectionDelivery == null) {
                dispatcherRejectionDelivery = DispatcherRejectionDelivery.directDelivery();
            }

            return new RejectionBus(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    /**
     * Generates an {@link UnhandledRejectionException} upon a dead
     * message.
     */
    private enum DeadRejectionTap implements DeadMessageTap<RejectionEnvelope> {
        INSTANCE;

        @Override
        public UnhandledRejectionException capture(RejectionEnvelope envelope) {
            final Message message = envelope.getMessage();
            final UnhandledRejectionException exception = new UnhandledRejectionException(message);
            return exception;
        }
    }

    /**
     * Performs no validation and reports the given message valid.
     */
    private enum NoOpValidator implements EnvelopeValidator<RejectionEnvelope> {
        INSTANCE;

        @Override
        public Optional<MessageInvalid> validate(RejectionEnvelope envelope) {
            checkNotNull(envelope);
            return absent();
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(RejectionBus.class);
    }

    @VisibleForTesting
    static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
