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
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.type.EventClass;
import org.spine3.server.validate.MessageValidator;
import org.spine3.validate.ConstraintViolation;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.spine3.base.Events.getMessage;
import static org.spine3.server.event.EventValidation.invalidEvent;
import static org.spine3.server.event.EventValidation.unsupportedEvent;

/**
 * Dispatches incoming events to subscribers, and provides ways for registering those subscribers.
 *
 * <h2>Receiving Events</h2>
 * <p>To receive events a subscriber object should:
 * <ol>
 *    <li>Expose a public method that accepts the type of the event as the first parameter,
 *        and {@link EventContext} as the second parameter;
 *    <li>Mark the method with {@link Subscribe} annotation;
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
 * @see Subscribe
 * @author Mikhail Melnik
 * @author Alexander Yevsyuov
 */
public class EventBus implements AutoCloseable {

    /**
     * The registry of event dispatchers.
     */
    private final DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();

    /**
     * The registry of event subscriber methods.
     */
    private final SubscriberRegistry subscriberRegistry = new SubscriberRegistry();

    /**
     * The {@code EventStore} to which put events before they get handled.
     */
    private final EventStore eventStore;

    /**
     * The executor for invoking subscriber methods.
     */
    private final Executor executor;

    private MessageValidator messageValidator;

    /**
     * Creates new instance.
     *
     * @param eventStore the event store to put posted events
     * @param executor the executor for invoking event subscribers
     */
    protected EventBus(EventStore eventStore, Executor executor) {
        this.eventStore = eventStore;
        this.executor = checkNotNull(executor);
        this.messageValidator = new MessageValidator();
    }

    /**
     * Creates a new instance configured with the direct executor for invoking subscribers.
     *
     * @param eventStore the {@code EventStore} to put posted events
     * @return new EventBus instance
     */
    public static EventBus newInstance(EventStore eventStore) {
        final EventBus result = new EventBus(eventStore, directExecutor());
        return result;
    }

    /**
     * Creates a new instance with the passed executor for invoking subscribers.
     *
     * @param eventStore the {@code EventStore} to put posted events
     * @param executor the executor for invoking event subscribers
     * @return a new EventBus instance
     */
    public static EventBus newInstance(EventStore eventStore, Executor executor) {
        final EventBus result = new EventBus(eventStore, executor);
        return result;
    }

    /**
     * Determines the class of the event from the passed event record.
     */
    public static EventClass getEventClass(Event event) {
        final Message message = getMessage(event);
        final EventClass result = EventClass.of(message);
        return result;
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

    /**
     * Registers the passed dispatcher with the bus.
     */
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

    /**
     * Removes dispatcher from the bus.
     */
    public void unregister(EventDispatcher dispatcher) {
        dispatcherRegistry.unregister(dispatcher);
    }

    /**
     * @return {@link EventStore} associated with the bus.
     */
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
        callDispatchers(event);
        final Message message = getMessage(event);
        final EventContext context = event.getContext();
        invokeSubscribers(message, context);
    }

    private void callDispatchers(Event event) {
        final EventClass eventClass = getEventClass(event);
        final Collection<EventDispatcher> dispatchers = dispatcherRegistry.getDispatchers(eventClass);
        for (EventDispatcher dispatcher : dispatchers) {
            dispatcher.dispatch(event);
        }
    }

    private void invokeSubscribers(Message event, EventContext context) {
        final Collection<EventSubscriber> subscribers = subscriberRegistry.getSubscribers(EventClass.of(event));
        if (subscribers.isEmpty()) {
            handleDeadEvent(event);
            return;
        }
        for (EventSubscriber subscriber : subscribers) {
            invokeSubscriber(subscriber, event, context);
        }
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
     * @param event the event message to check
     * @return an appropriate response: `OK`, `unsupported` or `invalid` event
     */
    public Response validate(Message event) {
        final EventClass eventClass = EventClass.of(event);
        if (isUnsupportedEvent(eventClass)) {
            return unsupportedEvent(event);
        }
        final List<ConstraintViolation> violations = messageValidator.validate(event);
        if (!violations.isEmpty()) {
            return invalidEvent(event, violations);
        }
        return Responses.ok();
    }

    private boolean isUnsupportedEvent(EventClass eventClass) {
        final boolean noDispatchers = !dispatcherRegistry.hasDispatchersFor(eventClass);
        final boolean noSubscribers = !hasSubscribers(eventClass);
        final boolean isUnsupported = noDispatchers && noSubscribers;
        return isUnsupported;
    }

    private static void handleDeadEvent(Message event) {
        log().warn("No subscriber defined for event class: " + event.getClass().getName());
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

    @VisibleForTesting
    /* package */ void setMessageValidator(MessageValidator messageValidator) {
        this.messageValidator = messageValidator;
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
