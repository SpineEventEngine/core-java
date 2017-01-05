/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.Statuses;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.server.event.error.InvalidEventException;
import org.spine3.server.event.error.UnsupportedEventException;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.type.EventClass;
import org.spine3.server.validate.MessageValidator;
import org.spine3.validate.ConstraintViolation;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Events.getMessage;

/**
 * Dispatches incoming events to subscribers, and provides ways for registering those subscribers.
 *
 * <h2>Receiving Events</h2>
 * <p>To receive event messages a subscriber object should:
 * <ol>
 *    <li>Expose a public method that accepts an event message as the first parameter
 *        and an {@link EventContext} as the second (optional) parameter;
 *    <li>Mark the method with the {@link Subscribe} annotation;
 *    <li>Register with an instance of EventBus using {@link #subscribe(EventSubscriber)}.
 * </ol>
 * Note: Since Protobuf messages are final classes, a subscriber method cannot accept just {@link Message}
 * as the first parameter. It must be an exact type of the event message that needs to be handled.
 *
 * <h2>Posting Events</h2>
 * <p>Events are posted to an EventBus using {@link #post(Event)} method. Normally this
 * is done by an {@link AggregateRepository} in the process of handling a command, or by a {@link ProcessManager}.
 *
 * <p>The passed {@link Event} is stored in the {@link EventStore} associated with the {@code EventBus}
 * <strong>before</strong> it is passed to subscribers.
 *
 * <p>The execution of subscriber methods is performed by an {@link Executor} associated with the instance of
 * the {@code EventBus}.
 *
 * <p>If a subscriber method throws an exception (which in general should be avoided), the exception is logged.
 * No other processing occurs.
 *
 * <p>If there is no subscriber for the posted event, the fact is logged as warning, with no further processing.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyuov
 * @see Subscribe
 */
public class EventBus implements AutoCloseable {

    /**
     * NOTE: Even though, the EventBus has a private constructor and is not supposed to be derived,
     * we do not make this class final in order to be able to spy() on it from Mockito (which cannot spy
     * on final or anonymous classes).
     **/

    /** The registry of event dispatchers. */
    private final DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();

    /** The registry of event subscriber methods. */
    private final SubscriberRegistry subscriberRegistry = new SubscriberRegistry();

    /** The {@code EventStore} to which put events before they get handled. */
    private final EventStore eventStore;

    /** The executor for invoking subscriber methods. */
    private final Executor executor;

    /** The executor for calling the dispatchers. */
    private final DispatcherEventExecutor dispatcherEventExecutor;

    /** The validator for events posted to the bus. */
    private final MessageValidator eventValidator;

    /** The enricher for posted events or {@code null} if the enrichment is not supported. */
    @Nullable
    private final EventEnricher enricher;

    /**
     * Creates new instance by the passed builder.
     */
    private EventBus(Builder builder) {
        this.eventStore = builder.eventStore;
        this.executor = builder.executor;
        this.eventValidator = builder.eventValidator;
        this.enricher = builder.enricher;
        this.dispatcherEventExecutor = builder.dispatcherEventExecutor;
        injectDispatcherProvider();
    }

    /**
     * Sets up the {@code DispatcherProvider} with an ability to obtain matching {@link EventDispatcher}s
     * by a given {@link EventClass} instance.
     */
    private void injectDispatcherProvider() {
        dispatcherEventExecutor.setDispatcherProvider(new Function<EventClass, Set<EventDispatcher>>() {
            @Nullable
            @Override
            public Set<EventDispatcher> apply(@Nullable EventClass eventClass) {
                checkNotNull(eventClass);
                final Set<EventDispatcher> dispatchers = dispatcherRegistry.getDispatchers(eventClass);
                return dispatchers;
            }
        });
    }

    /** Creates a builder for new {@code EventBus}. */
    public static Builder newBuilder() {
        return new Builder();
    }

    @VisibleForTesting
    /* package */ Executor getExecutor() {
        return executor;
    }

    @VisibleForTesting
    /* package */ MessageValidator getEventValidator() {
        return eventValidator;
    }

    @VisibleForTesting
    @Nullable
    /* package */ EventEnricher getEnricher() {
        return enricher;
    }

    @VisibleForTesting
    @Nullable
    /* package */ DispatcherEventExecutor getDispatcherEventExecutor() {
        return dispatcherEventExecutor;
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

    /** Registers the passed dispatcher with the bus. */
    public void register(EventDispatcher dispatcher) {
        dispatcherRegistry.register(dispatcher);
    }

    @VisibleForTesting
    /* package */ boolean hasSubscribers(EventClass eventClass) {
        final boolean result = subscriberRegistry.hasSubscribers(eventClass);
        return result;
    }

    @VisibleForTesting
    /* package */ Collection<EventSubscriber> getSubscribers(EventClass eventClass) {
        final Collection<EventSubscriber> result = subscriberRegistry.getSubscribers(eventClass);
        return result;
    }

    @VisibleForTesting
    /* package */ Set<EventDispatcher> getDispatchers(EventClass eventClass) {
        return dispatcherRegistry.getDispatchers(eventClass);
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

    /** Removes dispatcher from the bus. */
    public void unregister(EventDispatcher dispatcher) {
        dispatcherRegistry.unregister(dispatcher);
    }

    /** Returns {@link EventStore} associated with the bus. */
    public EventStore getEventStore() {
        return eventStore;
    }

    /**
     * Posts the event for handling.
     *
     * <p>The event is stored in the associated {@link EventStore} before passing it to dispatchers and subscribers.
     *
     * @param event the event to be handled
     */
    public void post(Event event) {
        store(event);
        final Event enriched = enrich(event);
        final int dispatchersCalled = callDispatchers(enriched);
        final Message message = getMessage(enriched);
        final EventContext context = enriched.getContext();
        final int subscribersInvoked = invokeSubscribers(message, context);

        if (dispatchersCalled == 0 && subscribersInvoked == 0) {
            handleDeadEvent(event);
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
        final Collection<EventDispatcher> dispatchers = dispatcherRegistry.getDispatchers(eventClass);

        dispatcherEventExecutor.dispatch(event);
        return dispatchers.size();
    }

    /**
     * Invoke the subscribers for the {@code event}.
     *
     * @param event   the event to pass to the subscribers.
     * @param context the event context related to the {@code event}.
     * @return the number of the subscribers invoked, or {@code 0} if no subscribers were invoked.
     */
    private int invokeSubscribers(Message event, EventContext context) {
        final Collection<EventSubscriber> subscribers = subscriberRegistry.getSubscribers(EventClass.of(event));
        for (EventSubscriber subscriber : subscribers) {
            invokeSubscriber(subscriber, event, context);
        }
        return subscribers.size();
    }

    private void store(Event event) {
        eventStore.append(event);
    }

    private void invokeSubscriber(final EventSubscriber target, final Message event, final EventContext context) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    target.handle(event, context);
                } catch (InvocationTargetException e) {
                    handleSubscriberException(e, event, context);
                }
            }
        });
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
     *                         {@link StreamObserver#onError(Throwable)} is called if an event is unsupported or invalid
     * @return {@code true} if event is supported and valid and can be posted, {@code false} otherwise
     */
    public boolean validate(Message event, StreamObserver<Response> responseObserver) {
        final EventClass eventClass = EventClass.of(event);
        if (isUnsupportedEvent(eventClass)) {
            final UnsupportedEventException unsupportedEvent = new UnsupportedEventException(event);
            responseObserver.onError(Statuses.invalidArgumentWithCause(unsupportedEvent));
            return false;
        }
        final List<ConstraintViolation> violations = eventValidator.validate(event);
        if (!violations.isEmpty()) {
            final InvalidEventException invalidEvent = InvalidEventException.onConstraintViolations(event, violations);
            responseObserver.onError(Statuses.invalidArgumentWithCause(invalidEvent));
            return false;
        }
        responseObserver.onNext(Responses.ok());
        responseObserver.onCompleted();
        return true;
    }

    private boolean isUnsupportedEvent(EventClass eventClass) {
        final boolean noDispatchers = !dispatcherRegistry.hasDispatchersFor(eventClass);
        final boolean noSubscribers = !hasSubscribers(eventClass);
        final boolean isUnsupported = noDispatchers && noSubscribers;
        return isUnsupported;
    }

    private static void handleDeadEvent(Message event) {
        log().warn("No subscriber or dispatcher defined for the event class: " + event.getClass()
                                                                                      .getName());
    }

    private static void handleSubscriberException(InvocationTargetException e,
                                                  Message eventMessage,
                                                  EventContext eventContext) {
        log().error("Exception handling event. Event message: {}, context: {}, cause: {}",
                    eventMessage, eventContext, e.getCause());
    }

    @Override
    public void close() throws Exception {
        dispatcherRegistry.unregisterAll();
        subscriberRegistry.unsubscribeAll();
        eventStore.close();
    }

    /** The {@code Builder} for {@code EventBus}. */
    public static class Builder {

        private EventStore eventStore;

        /**
         * Optional {@code Executor} for executing subscriber methods.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        private Executor executor;

        /**
         * Optional {@code DispatcherEventExecutor} for calling the dispatchers.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        private DispatcherEventExecutor dispatcherEventExecutor;

        /**
         * Optional validator for events.
         *
         * <p>If not set, a default value will be set by the builder.
         */
        private MessageValidator eventValidator;

        @Nullable
        private EventEnricher enricher;

        private Builder() {}

        public Builder setEventStore(EventStore eventStore) {
            this.eventStore = checkNotNull(eventStore);
            return this;
        }

        @Nullable
        public EventStore getEventStore() {
            return eventStore;
        }

        /**
         * Sets an {@code Executor} to be used for executing subscriber methods
         * in the {@code EventBus} we build.
         *
         * <p>If the {@code Executor} is not set, {@link MoreExecutors#directExecutor()} will be used.
         */
        public Builder setExecutor(Executor executor) {
            this.executor = checkNotNull(executor);
            return this;
        }

        @Nullable
        public Executor getExecutor() {
            return executor;
        }

        /**
         * Sets an {@code DispatcherEventExecutor} to be used for passing the event to the target dispatchers
         * in the {@code EventBus} we build.
         *
         * <p>If the {@code DispatcherEventExecutor} is not set, {@link DispatcherEventExecutor#directExecutor()}
         * will be used.
         */
        public Builder setDispatcherEventExecutor(DispatcherEventExecutor executor) {
            this.dispatcherEventExecutor = checkNotNull(executor);
            return this;
        }

        @Nullable
        public DispatcherEventExecutor getDispatcherEventExecutor() {
            return dispatcherEventExecutor;
        }

        public Builder setEventValidator(MessageValidator eventValidator) {
            this.eventValidator = checkNotNull(eventValidator);
            return this;
        }

        @Nullable
        public MessageValidator getEventValidator() {
            return eventValidator;
        }

        /**
         * Sets a custom {@link EventEnricher} for events posted to the {@code EventBus} which is being built.
         *
         * <p>If the {@code Enricher} is not set, a default instance will be provided.
         *
         * @param enricher the {@code Enricher} for events or {@code null} if enrichment is not supported
         */
        public Builder setEnricher(EventEnricher enricher) {
            this.enricher = enricher;
            return this;
        }

        @Nullable
        public EventEnricher getEnricher() {
            return enricher;
        }

        public EventBus build() {
            checkNotNull(eventStore, "eventStore must be set");

            if (executor == null) {
                executor = MoreExecutors.directExecutor();
            }

            if (dispatcherEventExecutor == null) {
                dispatcherEventExecutor = DispatcherEventExecutor.directExecutor();
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

    /* package */ static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
