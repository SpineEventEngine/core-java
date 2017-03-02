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
package org.spine3.server.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventClass;
import org.spine3.base.EventEnvelope;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.Statuses;
import org.spine3.server.bus.DispatcherRegistry;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.server.event.error.InvalidEventException;
import org.spine3.server.event.error.UnsupportedEventException;
import org.spine3.server.outbus.CommandOutputBus;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.validate.MessageValidator;
import org.spine3.validate.ConstraintViolation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
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
 *    <li>Expose a {@code public} method that accepts an event message as the first parameter
 *        and an {@link org.spine3.base.EventContext EventContext} as the second
 *        (optional) parameter.
 *    <li>Mark the method with the {@link Subscribe @Subscribe} annotation.
 *    <li>{@linkplain #subscribe(EventSubscriber) Register} with an instance of
 *    {@code EventBus} directly, or rely on message delivery from an {@link EventDispatcher}.
 *    An example of such a dispatcher is
 *    {@link org.spine3.server.projection.ProjectionRepository ProjectionRepository}
 * </ol>
 *
 * <p><strong>Note:</strong> A subscriber method cannot accept just {@link Message} as
 * the first parameter. It must be an <strong>exact type</strong> of the event message
 * that needs to be handled.
 *
 * <h2>Posting Events</h2>
 * <p>Events are posted to an EventBus using {@link #post(Event, StreamObserver)} method.
 * Normally this is done by an
 * {@link org.spine3.server.aggregate.AggregateRepository AggregateRepository} in the process
 * of handling a command, or by a {@link org.spine3.server.procman.ProcessManager ProcessManager}.
 *
 * <p>The passed {@link Event} is stored in the {@link EventStore} associated with
 * the {@code EventBus} <strong>before</strong> it is passed to subscribers.
 *
 * <p>The delivery of the events to the subscribers and dispatchers is performed by
 * an {@link SubscriberEventDelivery} and {@link DispatcherEventDelivery} strategies
 * associated with the instance of the {@code EventBus}, respectively.
 *
 * <p>If there is no subscribers or dispatchers for the posted event, the fact is
 * logged as warning, with no further processing.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyuov
 * @author Alex Tymchenko
 * @see org.spine3.server.projection.Projection Projection
 * @see Subscribe @Subscribe
 */
public class EventBus extends CommandOutputBus<Event, EventEnvelope, EventClass, EventDispatcher> {

    /*
     * NOTE: Even though, the EventBus has a private constructor and
     * is not supposed to be derived, we do not make this class final
     * in order to be able to spy() on it from Mockito (which cannot
     * spy on final or anonymous classes).
     */

    /** The registry of event dispatchers. */

    /** The registry of event subscriber methods. */
    private final SubscriberRegistry subscriberRegistry = new SubscriberRegistry();

    /** The {@code EventStore} to which put events before they get handled. */
    private final EventStore eventStore;

    /** The strategy to deliver events to the subscribers. */
    private final SubscriberEventDelivery subscriberEventDelivery;

    /** The strategy to deliver events to the dispatchers. */
    private final DispatcherEventDelivery dispatcherEventDelivery;

    /** The validator for events posted to the bus. */
    private final MessageValidator eventValidator;

    /** The enricher for posted events or {@code null} if the enrichment is not supported. */
    @Nullable
    private EventEnricher enricher;

    /**
     * Creates new instance by the passed builder.
     */
    private EventBus(Builder builder) {
        this.eventStore = builder.eventStore;
        this.eventValidator = builder.eventValidator;
        this.enricher = builder.enricher;
        this.subscriberEventDelivery = builder.subscriberEventDelivery;
        this.dispatcherEventDelivery = builder.dispatcherEventDelivery;

        injectProviders();
    }

    /**
     * Sets up the {@code DispatcherEventDelivery} and {@code SubscriberEventDelivery}
     * with an ability to obtain {@link EventDispatcher}s and {@link EventSubscriber}s
     * by a given {@link EventClass} instance.
     *
     * <p>Such an approach allows to query for an actual state of the
     * {@code dispatcherRegistry} and {@code subscriberRegistry}, keeping both of
     * the registries private to the {@code EventBus}.
     */
    private void injectProviders() {
        injectDispatcherProvider();
        injectSubscriberProvider();
    }

    private void injectDispatcherProvider() {
        dispatcherEventDelivery.setConsumerProvider(new Function<EventClass, Set<EventDispatcher>>() {
            @Nullable
            @Override
            public Set<EventDispatcher> apply(@Nullable EventClass eventClass) {
                checkNotNull(eventClass);
                final Set<EventDispatcher> dispatchers =
                        registry().getDispatchers(eventClass);
                return dispatchers;
            }
        });
    }

    private void injectSubscriberProvider() {
        subscriberEventDelivery.setConsumerProvider(new Function<EventClass, Set<EventSubscriber>>() {
            @Nullable
            @Override
            public Set<EventSubscriber> apply(@Nullable EventClass eventClass) {
                checkNotNull(eventClass);
                final Set<EventSubscriber> subscribers =
                        subscriberRegistry.getSubscribers(eventClass);
                return subscribers;
            }
        });
    }

    /** Creates a builder for new {@code EventBus}. */
    public static Builder newBuilder() {
        return new Builder();
    }

    @VisibleForTesting
    MessageValidator getEventValidator() {
        return eventValidator;
    }

    @VisibleForTesting
    @Nullable
    EventEnricher getEnricher() {
        return enricher;
    }

    @VisibleForTesting
    @Nullable
    DispatcherEventDelivery getDispatcherEventDelivery() {
        return dispatcherEventDelivery;
    }

    @VisibleForTesting
    @Nullable
    SubscriberEventDelivery getSubscriberEventDelivery() {
        return subscriberEventDelivery;
    }

    /**
     * Subscribes the event subscriber to receive events from the bus.
     *
     * <p>The event subscriber must expose at least one event subscriber method. If it is not the
     * case, {@code IllegalArgumentException} will be thrown.
     *
     * @param object the event subscriber object
     * @throws IllegalArgumentException if the object does not have event subscriber methods
     */
    public void subscribe(EventSubscriber object) {
        checkNotNull(object);
        subscriberRegistry.subscribe(object);
    }

    @VisibleForTesting
    boolean hasSubscribers(EventClass eventClass) {
        final boolean result = subscriberRegistry.hasSubscribers(eventClass);
        return result;
    }

    @VisibleForTesting
    Set<EventSubscriber> getSubscribers(EventClass eventClass) {
        final Set<EventSubscriber> result = subscriberRegistry.getSubscribers(eventClass);
        return result;
    }

    @VisibleForTesting
    Set<EventDispatcher> getDispatchers(EventClass eventClass) {
        return registry().getDispatchers(eventClass);
    }

    /**
     * Unregisters all subscriber methods on a registered {@code object}.
     *
     * @param object the object whose methods should be unregistered
     * @throws IllegalArgumentException if the object was not previously registered
     */
    public void unsubscribe(EventSubscriber object) {
        checkNotNull(object);
        subscriberRegistry.unsubscribe(object);
    }

    @Override
    public void handleDeadMessage(EventEnvelope message,
                                  StreamObserver<Response> responseObserver) {
        final Event event = message.getOuterObject();
        log().warn("No subscriber or dispatcher defined for the event class: {}",
                   event.getClass()
                        .getName());
        //TODO:3/2/17:alex.tymchenko: should we notify the observer?
    }

    @Override
    protected DispatcherRegistry<EventClass, EventDispatcher> createRegistry() {
        return new EventDispatcherRegistry();
    }

    /** Returns {@link EventStore} associated with the bus. */
    public EventStore getEventStore() {
        return eventStore;
    }

    /**
     * Validates and posts the event for handling.
     *
     * <p>Does performs the same as the {@link #post(Event, StreamObserver) overloaded method}, but
     * does not require any response observer.
     *
     * <p>This method should be used if the callee does not care about the event acknowledgement.
     *
     * @param event the event to be handled
     * @see #post(Event, StreamObserver)
     */
    public void post(Event event) {
        post(event, emptyObserver());
    }

    /**
     * Validates and posts the event for handling.
     *
     * <p>If the event is invalid, the {@code responseObserver} is notified of an error.
     *
     * <p>If the event is valid, it is stored in the associated {@link EventStore} before
     * passing it to dispatchers and subscribers.
     *
     * <p>The {@code responseObserver} is then notified of a successful acknowledgement of the
     * passed event.
     *
     * @param event            the event to be handled
     * @param responseObserver the observer to be notified
     * @see #validate(Message, StreamObserver)
     */
    @Override
    public void post(Event event, StreamObserver<Response> responseObserver) {
        checkNotNull(responseObserver);

        final boolean validationPassed = doValidate(event, responseObserver);

        if (validationPassed) {
            responseObserver.onNext(Responses.ok());
            store(event);
            final Event enriched = enrich(event);
            final int dispatchersCalled = callDispatchers(enriched);
            final int subscribersInvoked = invokeSubscribers(enriched);

            if (dispatchersCalled == 0 && subscribersInvoked == 0) {
                handleDeadMessage(EventEnvelope.of(event), responseObserver);
            }
            responseObserver.onCompleted();
        }
    }

    private Event enrich(Event event) {
        if (enricher == null ||
                !enricher.canBeEnriched(event)) {
            return event;
        }
        final Event enriched = enricher.enrich(event);
        return enriched;
    }

    /**
     * Call the dispatchers for the {@code event}.
     *
     * @param event the event to pass to the dispatchers.
     * @return the number of the dispatchers called, or {@code 0} if there weren't any.
     */
    private int callDispatchers(Event event) {
        final EventClass eventClass = EventClass.of(event);
        final Collection<EventDispatcher> dispatchers = registry().getDispatchers(eventClass);
        dispatcherEventDelivery.deliver(event);
        return dispatchers.size();
    }

    /**
     * Invoke the subscribers for the {@code event}.
     *
     * @param event the event to pass to the subscribers.
     * @return the number of the subscribers invoked, or {@code 0}
     *         if no subscribers were invoked.
     */
    private int invokeSubscribers(Event event) {
        final EventClass eventClass = EventClass.of(event);
        final Set<EventSubscriber> subscribers = subscriberRegistry.getSubscribers(eventClass);
        subscriberEventDelivery.deliver(event);
        return subscribers.size();
    }

    private void store(Event event) {
        eventStore.append(event);
    }

    /**
     * Verifies that an event can be posted to this {@code EventBus}.
     *
     * <p>An event can be posted if its message has either dispatcher or handler registered with
     * this {@code EventBus}.
     * The message also must satisfy validation constraints defined in its Protobuf type.
     *
     * @param event            the event message to check
     * @param responseObserver the observer to obtain the result of the call;
     *                         {@link StreamObserver#onError(Throwable)} is called if
     *                         an event is unsupported or invalid
     * @return {@code true} if event is supported and valid and can be posted,
     *         {@code false} otherwise
     */
    public boolean validate(Message event, StreamObserver<Response> responseObserver) {
        if (!doValidate(event, responseObserver)) {
            return false;
        }
        responseObserver.onNext(Responses.ok());
        responseObserver.onCompleted();
        return true;
    }

    /**
     * Validates the event message and notifies the observer of those (if any).
     */
    private boolean doValidate(Message event, StreamObserver<Response> responseObserver) {
        final EventClass eventClass = EventClass.of(event);
        if (isUnsupportedEvent(eventClass)) {
            final UnsupportedEventException unsupportedEvent = new UnsupportedEventException(event);
            responseObserver.onError(Statuses.invalidArgumentWithCause(unsupportedEvent));
            return false;
        }
        final List<ConstraintViolation> violations = eventValidator.validate(event);
        if (!violations.isEmpty()) {
            final InvalidEventException invalidEvent =
                    InvalidEventException.onConstraintViolations(event, violations);
            responseObserver.onError(Statuses.invalidArgumentWithCause(invalidEvent));
            return false;
        }
        return true;
    }

    /**
     * Add a new field enrichment translation function.
     *
     * @param eventFieldClass      a class of the field in the event message
     * @param enrichmentFieldClass a class of the field in the enrichment message
     * @param function             a function which converts fields
     * @see EventEnricher
     */
    public <S, T> void addFieldEnrichment(Class<S> eventFieldClass,
                                          Class<T> enrichmentFieldClass,
                                          Function<S, T> function) {
        checkNotNull(eventFieldClass);
        checkNotNull(enrichmentFieldClass);
        checkNotNull(function);

        if (enricher == null) {
            enricher = EventEnricher.newBuilder()
                                    .addFieldEnrichment(eventFieldClass,
                                                        enrichmentFieldClass,
                                                        function)
                                    .build();
        } else {
            enricher.registerFieldEnrichment(eventFieldClass, enrichmentFieldClass, function);
        }
    }

    private boolean isUnsupportedEvent(EventClass eventClass) {
        final boolean noDispatchers = !registry().hasDispatchersFor(eventClass);
        final boolean noSubscribers = !hasSubscribers(eventClass);
        final boolean isUnsupported = noDispatchers && noSubscribers;
        return isUnsupported;
    }

    @Override
    public void close() throws Exception {
        registry().unregisterAll();
        subscriberRegistry.unsubscribeAll();
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

    private static StreamObserver<Response> emptyObserver() {
        return emptyObserver;
    }

    /**
     * The {@code StreamObserver} which does nothing.
     *
     * @see #emptyObserver()
     */
    private static final StreamObserver<Response> emptyObserver = new StreamObserver<Response>() {
        @Override
        public void onNext(Response value) {
            // Do nothing.
        }

        @Override
        public void onError(Throwable t) {
            // Do nothing.
        }

        @Override
        public void onCompleted() {
            // Do nothing.
        }
    };

    /** The {@code Builder} for {@code EventBus}. */
    public static class Builder {

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
         * Optional {@code SubscriberEventDelivery} for executing subscriber methods.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        @Nullable
        private SubscriberEventDelivery subscriberEventDelivery;

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
         * <p>If not set, the enrichments will NOT be supported in the {@code EventBus} instance built.
         */
        @Nullable
        private EventEnricher enricher;

        private Builder() {
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
         * <p>If an {@code Executor} is not set in the builder, {@link MoreExecutors#directExecutor()}
         * will be used.
         *
         * @see #setEventStore(EventStore)
         */
        @SuppressWarnings("MethodParameterNamingConvention")
        public Builder setEventStoreStreamExecutor(Executor eventStoreStreamExecutor) {
            checkState(eventStore == null, MSG_EVENT_STORE_CONFIGURED);
            this.eventStoreStreamExecutor = eventStoreStreamExecutor;
            return this;
        }

        public Optional<Executor> getEventStoreStreamExecutor() {
            return Optional.fromNullable(eventStoreStreamExecutor);
        }

        /**
         * Sets a {@code SubscriberEventDelivery} to be used for the event delivery
         * to the subscribers in the {@code EventBus} we build.
         *
         * <p>If the {@code SubscriberEventDelivery} is not set,
         * {@link SubscriberEventDelivery#directDelivery()} will be used.
         */
        public Builder setSubscriberEventDelivery(SubscriberEventDelivery delivery) {
            this.subscriberEventDelivery = checkNotNull(delivery);
            return this;
        }

        public Optional<SubscriberEventDelivery> getSubscriberEventDelivery() {
            return Optional.fromNullable(subscriberEventDelivery);
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
         * <p>If the {@code Enricher} is not set, the enrichments
         * will <strong>NOT</strong> be supported for the {@code EventBus} instance built.
         *
         * @param enricher the {@code Enricher} for events or
         *                 {@code null} if enrichment is not supported
         */
        public Builder setEnricher(EventEnricher enricher) {
            this.enricher = enricher;
            return this;
        }

        public Optional<EventEnricher> getEnricher() {
            return Optional.fromNullable(enricher);
        }

        public EventBus build() {
            checkState(storageFactory != null || eventStore != null,
                       "Either storageFactory or eventStore must be set to build the EventBus instance");

            if (eventStoreStreamExecutor == null) {
                this.eventStoreStreamExecutor = MoreExecutors.directExecutor();
            }

            if (eventStore == null) {
                eventStore = EventStore.newBuilder()
                                       .setStreamExecutor(eventStoreStreamExecutor)
                                       .setStorage(storageFactory.createEventStorage())
                                       .setLogger(EventStore.log())
                                       .build();
            }

            if (subscriberEventDelivery == null) {
                subscriberEventDelivery = SubscriberEventDelivery.directDelivery();
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
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventBus.class);
    }

    static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
