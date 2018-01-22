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
package io.spine.server.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.grpc.LoggingObserver;
import io.spine.grpc.LoggingObserver.Level;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.outbus.CommandOutputBus;
import io.spine.server.outbus.OutputDispatcherRegistry;
import io.spine.server.storage.StorageFactory;
import io.spine.validate.MessageValidator;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Dispatches incoming events to subscribers, and provides ways for registering those subscribers.
 *
 * <h2>Receiving Events</h2>
 * <p>To receive event messages a subscriber object should:
 * <ol>
 * <li>Expose a {@code public} method that accepts an event message as the first parameter
 * and an {@link EventContext EventContext} as the second
 * (optional) parameter.
 * <li>Mark the method with the {@link io.spine.core.Subscribe @Subscribe} annotation.
 * <li>{@linkplain #register(io.spine.server.bus.MessageDispatcher)} Register} with an
 * instance of {@code EventBus} directly, or rely on message delivery
 * from an {@link EventDispatcher}. An example of such a dispatcher is
 * {@link io.spine.server.projection.ProjectionRepository ProjectionRepository}
 * </ol>
 *
 * <p><strong>Note:</strong> A subscriber method cannot accept just {@link Message} as
 * the first parameter. It must be an <strong>exact type</strong> of the event message
 * that needs to be handled.
 *
 * <h2>Posting Events</h2>
 * <p>Events are posted to an EventBus using {@link #post(Message, StreamObserver)} method.
 * Normally this is done by an
 * {@linkplain io.spine.server.aggregate.AggregateRepository AggregateRepository} in the process
 * of handling a command,
 * or by a {@linkplain io.spine.server.procman.ProcessManager ProcessManager}.
 *
 * <p>The passed {@link Event} is stored in the {@link EventStore} associated with
 * the {@code EventBus} <strong>before</strong> it is passed to subscribers.
 *
 * <p>The delivery of the events to the subscribers and dispatchers is performed by
 * the {@link DispatcherEventDelivery} strategy associated with
 * this instance of the {@code EventBus}.
 *
 * <p>If there is no subscribers or dispatchers for the posted event, the fact is
 * logged as warning, with no further processing.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyuov
 * @author Alex Tymchenko
 * @see io.spine.server.projection.Projection Projection
 * @see io.spine.core.Subscribe @Subscribe
 */
public class EventBus
        extends CommandOutputBus<Event, EventEnvelope, EventClass, EventDispatcher<?>> {

    /*
     * NOTE: Even though, the EventBus has a private constructor and
     * is not supposed to be derived, we do not make this class final
     * in order to be able to spy() on it from Mockito (which cannot
     * spy on final or anonymous classes).
     */

    /** The {@code EventStore} to which put events before they get handled. */
    private final EventStore eventStore;

    /** The validator for messages of posted events. */
    private final MessageValidator eventMessageValidator;

    /** Filters applied when an event is posted. */
    private final Deque<BusFilter<EventEnvelope>> filterChain;

    /** The observer of post operations. */
    private final StreamObserver<Ack> streamObserver;

    /** The validator for events posted to the bus. */
    @Nullable
    private EventValidator eventValidator;

    /** The enricher for posted events or {@code null} if the enrichment is not supported. */
    @Nullable
    private final EventEnricher enricher;

    /** Creates new instance by the passed builder. */
    private EventBus(Builder builder) {
        super(checkNotNull(builder.dispatcherEventDelivery));
        this.eventStore = builder.eventStore;
        this.enricher = builder.enricher;
        this.eventMessageValidator = builder.eventValidator;
        this.filterChain = builder.getFilters();
        this.streamObserver = LoggingObserver.forClass(getClass(), builder.logLevelForPost);
    }

    /** Creates a builder for new {@code EventBus}. */
    public static Builder newBuilder() {
        return new Builder();
    }

    @VisibleForTesting
    Set<? extends EventDispatcher<?>> getDispatchers(EventClass eventClass) {
        return registry().getDispatchers(eventClass);
    }

    @VisibleForTesting
    boolean hasDispatchers(EventClass eventClass) {
        return registry().hasDispatchersFor(eventClass);
    }

    @Override
    protected DeadMessageTap<EventEnvelope> getDeadMessageHandler() {
        return DeadEventTap.INSTANCE;
    }

    @Override
    protected EnvelopeValidator<EventEnvelope> getValidator() {
        if (eventValidator == null) {
            eventValidator = new EventValidator(eventMessageValidator);
        }
        return eventValidator;
    }

    @Override
    protected OutputDispatcherRegistry<EventClass, EventDispatcher<?>> createRegistry() {
        return new EventDispatcherRegistry();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for this method.
    @Override
    protected Deque<BusFilter<EventEnvelope>> createFilterChain() {
        return filterChain;
    }

    @Override
    protected EventEnvelope toEnvelope(Event message) {
        return EventEnvelope.of(message);
    }

    @VisibleForTesting
    MessageValidator getMessageValidator() {
        return eventMessageValidator;
    }

    /** Returns {@link EventStore} associated with the bus. */
    public EventStore getEventStore() {
        return eventStore;
    }

    /**
     * Posts the event for handling.
     *
     * <p>Performs the same action as the
     * {@linkplain CommandOutputBus#post(Message, StreamObserver)} parent method}, but does not
     * require any response observer.
     *
     * @param event the event to be handled
     * @see CommandOutputBus#post(Message, StreamObserver)
     */
    public final void post(Event event) {
        post(event, streamObserver);
    }

    /**
     * Posts the events for handling.
     *
     * <p>Performs the same action as the
     * {@linkplain CommandOutputBus#post(Iterable, StreamObserver)} parent method}, but does not
     * require any response observer.
     *
     * <p>This method should be used if the callee does not care about the events acknowledgement.
     *
     * @param events the events to be handled
     * @see CommandOutputBus#post(Message, StreamObserver)
     */
    public final void post(Iterable<Event> events) {
        post(events, streamObserver);
    }

    @Override
    protected EventEnvelope enrich(EventEnvelope event) {
        if (enricher == null || !enricher.canBeEnriched(event)) {
            return event;
        }
        final EventEnvelope enriched = enricher.enrich(event);
        return enriched;
    }

    @Override
    protected void store(Iterable<Event> events) {
        eventStore.appendAll(events);
    }

    @Override
    public void close() throws Exception {
        super.close();
        eventStore.close();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected EventDispatcherRegistry registry() {
        return (EventDispatcherRegistry) super.registry();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected DispatcherEventDelivery delivery() {
        return (DispatcherEventDelivery) super.delivery();
    }

    /** The {@code Builder} for {@code EventBus}. */
    public static class Builder extends AbstractBuilder<EventEnvelope, Event, Builder> {

        private static final String MSG_EVENT_STORE_CONFIGURED = "EventStore already configured.";

        /**
         * A {@code StorageFactory} for configuring the {@code EventStore} instance
         * for this {@code EventBus}.
         *
         * <p>If the {@code EventStore} is passed to this {@code Builder} explicitly
         * via {@link #setEventStore(EventStore)}, the {@code storageFactory} field
         * value is not used.
         *
         * <p>Either a {@code StorageFactory} or an {@code EventStore} are mandatory
         * to create an instance of {@code EventBus}.
         */
        @Nullable
        private StorageFactory storageFactory;

        /**
         * A {@code EventStore} for storing all the events passed through the {@code EventBus}.
         *
         * <p>If not set, a default instance will be created by the builder
         * with the help of the {@code StorageFactory}.
         *
         * <p>Either a {@code StorageFactory} or an {@code EventStore} are mandatory
         * to create an instance of {@code EventBus}.
         */
        @Nullable
        private EventStore eventStore;

        /**
         * Optional {@code Executor} for returning event stream from the {@code EventStore}.
         *
         * <p>Used only if the {@code EventStore} is NOT set explicitly.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        @Nullable
        private Executor eventStoreStreamExecutor;

        /**
         * Optional {@code DispatcherEventDelivery} for calling the dispatchers.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        @Nullable
        private DispatcherEventDelivery dispatcherEventDelivery;

        /**
         * Optional validator for events.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        @Nullable
        private MessageValidator eventValidator;

        /**
         * Optional enricher for events.
         *
         * <p>If not set, the enrichments will NOT be supported
         * in the {@code EventBus} instance built.
         */
        @Nullable
        private EventEnricher enricher;

        /** Logging level for posted events.  */
        private LoggingObserver.Level logLevelForPost = Level.TRACE;

        /** Prevents direct instantiation. */
        private Builder() {
            super();
        }

        /**
         * Specifies an {@code StorageFactory} to configure this {@code EventBus}.
         *
         * <p>This {@code StorageFactory} instance will be used to create
         * an instance of {@code EventStore} for this {@code EventBus},
         * <em>if</em> {@code EventStore} was not explicitly set in the builder.
         *
         * <p>Either a {@code StorageFactory} or an {@code EventStore} are mandatory
         * to create an {@code EventBus}.
         *
         * @see #setEventStore(EventStore)
         */
        public Builder setStorageFactory(StorageFactory storageFactory) {
            checkState(eventStore == null, MSG_EVENT_STORE_CONFIGURED);
            this.storageFactory = checkNotNull(storageFactory);
            return this;
        }

        public Optional<StorageFactory> getStorageFactory() {
            return Optional.fromNullable(storageFactory);
        }

        /**
         * Specifies {@code EventStore} to be used when creating new {@code EventBus}.
         *
         * <p>This method can be called if neither {@link #setEventStoreStreamExecutor(Executor)}
         * nor {@link #setStorageFactory(StorageFactory)} were called before.
         *
         * <p>Either a {@code StorageFactory} or an {@code EventStore} must be set
         * to create an {@code EventBus}.
         *
         * @see #setEventStoreStreamExecutor(Executor)
         * @see #setStorageFactory(StorageFactory)
         */
        public Builder setEventStore(EventStore eventStore) {
            checkState(storageFactory == null, "storageFactory already set.");
            checkState(eventStoreStreamExecutor == null, "eventStoreStreamExecutor already set.");
            this.eventStore = checkNotNull(eventStore);
            return this;
        }

        public Optional<EventStore> getEventStore() {
            return Optional.fromNullable(eventStore);
        }

        /**
         * Specifies an {@code Executor} for returning event stream from {@code EventStore}.
         *
         * <p>This {@code Executor} instance will be used for creating
         * new {@code EventStore} instance when building {@code EventBus}, <em>if</em>
         * {@code EventStore} was not explicitly set in the builder.
         *
         * <p>If an {@code Executor} is not set in the builder,
         * {@link MoreExecutors#directExecutor()} will be used.
         *
         * @see #setEventStore(EventStore)
         */

        public Builder setEventStoreStreamExecutor(Executor eventStoreStreamExecutor) {
            checkState(eventStore == null, MSG_EVENT_STORE_CONFIGURED);
            this.eventStoreStreamExecutor = eventStoreStreamExecutor;
            return this;
        }

        public Optional<Executor> getEventStoreStreamExecutor() {
            return Optional.fromNullable(eventStoreStreamExecutor);
        }

        /**
         * Sets a {@code DispatcherEventDelivery} to be used for the event delivery
         * to the dispatchers in the {@code EventBus} we build.
         *
         * <p>If the {@code DispatcherEventDelivery} is not set,
         * {@link DispatcherEventDelivery#directDelivery()} will be used.
         */
        public Builder setDispatcherEventDelivery(DispatcherEventDelivery delivery) {
            this.dispatcherEventDelivery = checkNotNull(delivery);
            return this;
        }

        public Optional<DispatcherEventDelivery> getDispatcherEventDelivery() {
            return Optional.fromNullable(dispatcherEventDelivery);
        }

        public Builder setEventValidator(MessageValidator eventValidator) {
            this.eventValidator = checkNotNull(eventValidator);
            return this;
        }

        public Optional<MessageValidator> getEventValidator() {
            return Optional.fromNullable(eventValidator);
        }

        /**
         * Sets a custom {@link EventEnricher} for events posted to
         * the {@code EventBus} which is being built.
         *
         * <p>If the {@code EventEnricher} is not set, the enrichments
         * will <strong>NOT</strong> be supported for the {@code EventBus} instance built.
         *
         * @param enricher the {@code EventEnricher} for events or {@code null} if enrichment is
         *                 not supported
         */
        public Builder setEnricher(EventEnricher enricher) {
            this.enricher = enricher;
            return this;
        }

        public Optional<EventEnricher> getEnricher() {
            return Optional.fromNullable(enricher);
        }

        /**
         * Sets logging level for post operations.
         *
         * <p>If not set directly, {@link io.spine.grpc.LoggingObserver.Level#TRACE Level.TRACE}
         * will be used.
         */
        public Builder setLogLevelForPost(Level level) {
            this.logLevelForPost = level;
            return this;
        }

        /**
         * Obtains the logging level for {@linkplain EventBus#post(Event) post} operations.
         */
        public Level getLogLevelForPost() {
            return this.logLevelForPost;
        }

        /**
         * Builds an instance of {@link EventBus}.
         *
         * <p>This method is supposed to be called internally when building an enclosing
         * {@code BoundedContext}.
         */
        @Override
        @Internal
        public EventBus build() {
            final String message = "Either storageFactory or eventStore must be " +
                                   "set to build the EventBus instance";
            checkState(storageFactory != null || eventStore != null, message);

            if (eventStoreStreamExecutor == null) {
                eventStoreStreamExecutor = MoreExecutors.directExecutor();
            }
            checkNotNull(eventStoreStreamExecutor);

            if (eventStore == null) {
                eventStore = EventStore.newBuilder()
                                       .setStreamExecutor(eventStoreStreamExecutor)
                                       .setStorageFactory(storageFactory)
                                       .setLogger(EventStore.log())
                                       .build();
            }

            if (dispatcherEventDelivery == null) {
                dispatcherEventDelivery = DispatcherEventDelivery.directDelivery();
            }

            if (eventValidator == null) {
                eventValidator = MessageValidator.newInstance();
            }

            final EventBus result = new EventBus(this);
            return result;
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    /**
     * Produces an {@link UnsupportedEventException} upon a dead event.
     */
    private enum DeadEventTap implements DeadMessageTap<EventEnvelope> {
        INSTANCE;

        @Override
        public UnsupportedEventException capture(EventEnvelope envelope) {
            final Message message = envelope.getMessage();
            final UnsupportedEventException exception = new UnsupportedEventException(message);
            return exception;
        }
    }
}
