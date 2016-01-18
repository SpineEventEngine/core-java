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
package org.spine3.eventbus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.internal.EventHandlerMethod;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.stream.EventStore;
import org.spine3.server.util.EventRecords;
import org.spine3.type.EventClass;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches incoming events to handlers, and provides ways for registering those handlers.
 *
 * <h2>Receiving Events</h2>
 * <p>To receive events a handler object should:
 * <ol>
 *    <li>Expose a public method that accepts the type of the event as the first parameter,
 *        and {@link EventContext} as the second parameter;
 *    <li>Mark the method with {@link Subscribe} annotation;</li>
 * <li>Register with an instance of EventBus using {@link #register(Object)}.</li>
 * </ol>
 * Note: Since Protobuf messages are final classes, a handler method cannot accept just {@link Message}
 * as the first parameter. It must be an exact type of the event that needs to be handled.
 *
 * <h2>Posting Events</h2>
 * <p>Events are posted to an EventBus using {@link #post(EventRecord)} method. Normally this
 * is done by an {@link AggregateRepository} in the process of handling a command, or by a {@link ProcessManager}.
 *
 * <p>The passed {@link EventRecord} is stored in the {@link EventStore} associated with the {@code EventBus}
 * <strong>before</strong> it is passed to handlers.
 *
 * <p>The execution of handler methods is performed by an {@link Executor} associated with the instance of
 * the {@code EventBus}.
 *
 * <p>If a handler method throws an exception (which in general should be avoided), the exception is logged.
 * No other processing occurs.
 *
 * <p>If there is no handler for the posted event, the fact is logged, with no further processing.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyuov
 */
public class EventBus implements AutoCloseable {

    /**
     * The registry of handler methods.
     */
    private final Registry registry = new Registry();

    /**
     * The {@code EventStore} to which put events before they get handled.
     */
    private final EventStore eventStore;

    /**
     * The executor for invoking handler methods.
     */
    private final Executor executor;

    /**
     * Creates new instance.
     *
     * @param eventStore the event store to put posted events
     * @param executor the executor for invoking event handlers
     */
    protected EventBus(EventStore eventStore, Executor executor) {
        this.eventStore = eventStore;
        this.executor = checkNotNull(executor);
    }

    /**
     * Creates a new instance configured with the direct executor for invoking handlers.
     *
     * @param eventStore the {@code EventStore} to put posted events
     * @return new EventBus instance
     */
    public static EventBus newInstance(EventStore eventStore) {
        final EventBus result = new EventBus(eventStore, MoreExecutors.directExecutor());
        return result;
    }

    /**
     * Creates a new instance with the passed executor for invoking handlers.
     *
     * @param eventStore the {@code EventStore} to put posted events
     * @param executor the executor for invoking event handlers
     * @return a new EventBus instance
     */
    public static EventBus newInstance(EventStore eventStore, Executor executor) {
        final EventBus result = new EventBus(eventStore, executor);
        return result;
    }

    /**
     * Registers all subscriber methods on {@code object} to receive events.
     *
     * @param object the event applier object whose subscriber methods should be registered
     */
    public void register(Object object) {
        checkNotNull(object);
        final Map<EventClass, EventHandlerMethod> handlers = EventHandlerMethod.scan(object);
        subscribe(handlers);
    }

    private void subscribe(Map<EventClass, EventHandlerMethod> handlers) {
        registry.subscribe(handlers);
    }

    /**
     * Unregisters all subscriber methods on a registered {@code object}.
     *
     * @param object the object whose methods should be unregistered
     * @throws IllegalArgumentException if the object was not previously registered
     */
    public void unregister(Object object) {
        checkNotNull(object);
        final Map<EventClass, EventHandlerMethod> handlers = EventHandlerMethod.scan(object);
        unsubscribe(handlers);
    }

    /**
     * Removes passed event handlers from the bus.
     *
     * @param handlers a map of the event handlers to remove
     */
    private void unsubscribe(Map<EventClass, EventHandlerMethod> handlers) {
        registry.unsubscribe(handlers);
    }

    private Collection<EventHandlerMethod> getHandlers(EventClass c) {
        return registry.getHandlers(c);
    }

    /**
     * Posts the event and its context passed as the record to be processed by registered handlers.
     *
     * <p>The record is stored in the associated {@link EventStore} <strong>before</strong> any handling occurs.
     *
     * @param record the record with the event and its context to be handled
     */
    public void post(EventRecord record) {
        store(record);

        final Message event = EventRecords.getEvent(record);
        final EventContext context = record.getContext();

        invokeHandlers(event, context);
    }

    private void invokeHandlers(Message event, EventContext context) {
        final Collection<EventHandlerMethod> handlers = getHandlers(EventClass.of(event));

        if (handlers.isEmpty()) {
            handleDeadEvent(event);
            return;
        }

        for (EventHandlerMethod handler : handlers) {
            invokeHandler(handler, event, context);
        }
    }

    private void store(EventRecord record) {
        eventStore.append(record);
    }

    private void invokeHandler(final EventHandlerMethod handler, final Message event, final EventContext context) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    handler.invoke(event, context);
                } catch (InvocationTargetException e) {
                    processHandlerException(handler, e);
                }
            }
        });
    }

    private static void handleDeadEvent(Message event) {
        log().warn("No handler defined for event class: " + event.getClass().getName());
    }

    private static void processHandlerException(EventHandlerMethod handler, InvocationTargetException e) {
        log().error("Exception invoking method: " + handler.getFullName(), e);
    }

    @Override
    public void close() throws IOException {
        registry.unsubscribeAll();
        eventStore.close();
    }

    /**
     * A wrapper over a map from {@code EventClass} to one or more {@code EventHandlerMethod}
     * that handle events of this class.
     */
    private static class Registry {

        private final Multimap<EventClass, EventHandlerMethod> handlersByClass = HashMultimap.create();
        private final ReadWriteLock lockOnHandlersByClass = new ReentrantReadWriteLock();

        private void subscribe(Map<EventClass, EventHandlerMethod> handlers) {
            lockOnHandlersByClass.writeLock().lock();
            try {
                for (Map.Entry<EventClass, EventHandlerMethod> entry : handlers.entrySet()) {
                    handlersByClass.put(entry.getKey(), entry.getValue());
                }
            } finally {
                lockOnHandlersByClass.writeLock().unlock();
            }
        }

        private void unsubscribe(Map<EventClass, EventHandlerMethod> handlers) {
            for (Map.Entry<EventClass, EventHandlerMethod> entry : handlers.entrySet()) {

                final EventClass c = entry.getKey();
                final EventHandlerMethod handler = entry.getValue();

                unsubscribe(c, handler);
            }
        }

        private void unsubscribe(EventClass c, EventHandlerMethod handler) {
            lockOnHandlersByClass.writeLock().lock();
            try {
                final Collection<EventHandlerMethod> currentSubscribers = handlersByClass.get(c);
                if (!currentSubscribers.contains(handler)) {
                    throw handlerMethodWasNotRegistered(handler);
                }
                currentSubscribers.remove(handler);
            } finally {
                lockOnHandlersByClass.writeLock().unlock();
            }
        }

        private void unsubscribeAll() {
            handlersByClass.clear();
            log().info("All subscribers cleared.");
        }

        private static IllegalArgumentException handlerMethodWasNotRegistered(EventHandlerMethod handler) {
            return new IllegalArgumentException(
                    "Cannot un-subscribe the event handler, which was not subscribed before:" + handler.getFullName());
        }

        private Collection<EventHandlerMethod> getHandlers(EventClass c) {
            return ImmutableList.copyOf(handlersByClass.get(c));
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventBus.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
