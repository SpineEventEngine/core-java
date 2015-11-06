/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.EventClass;
import org.spine3.base.EventContext;
import org.spine3.internal.EventHandlerMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches incoming events to the appropriate registered handler according to the type of incoming event.
 *
 * //TODO:2015-11-06:alexander.yevsyukov: Document handler methods.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyuov
 */
public class EventBus {

    /* This code is based on Guava {@link com.google.common.eventbus.EventBus} class. */

    //TODO:2015-11-06:alexander.yevsyukov: Wrap keeping handlers into a HandlerRegistry package access class.
    private final Multimap<EventClass, EventHandlerMethod> handlersByClass = HashMultimap.create();
    private final ReadWriteLock lockOnHandlersByClass = new ReentrantReadWriteLock();

    private EventBus() {
        // Prevent instantiation from outside.
        // This constructor is supposed to be called only by singleton implementation.
    }

    /**
     * Registers all subscriber methods on {@code object} to receive events.
     *
     * @param object the event applier object whose subscriber methods should be registered
     */
    public void register(Object object) {

        checkNotNull(object);
        final Map<EventClass, EventHandlerMethod> handlers = EventHandlerMethod.scan(object);
        putHandlersToBus(handlers);
    }

    private void putHandlersToBus(Map<EventClass, EventHandlerMethod> handlers) {

        lockOnHandlersByClass.writeLock().lock();
        try {
            for (Map.Entry<EventClass, EventHandlerMethod> handler : handlers.entrySet()) {
                handlersByClass.put(handler.getKey(), handler.getValue());
            }
        } finally {
            lockOnHandlersByClass.writeLock().unlock();
        }
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
     * @param handlers a map of the event handlers to remove
     */
    private void unsubscribe(Map<EventClass, EventHandlerMethod> handlers) {

        for (Map.Entry<EventClass, EventHandlerMethod> entry : handlers.entrySet()) {

            final EventClass c = entry.getKey();
            final EventHandlerMethod handler = entry.getValue();

            lockOnHandlersByClass.writeLock().lock();
            try {
                final Collection<EventHandlerMethod> currentSubscribers = handlersByClass.get(c);
                if (!currentSubscribers.contains(handler)) {
                    throw new IllegalArgumentException(
                            "Missing event handler for the annotated method. Is " + handler.getFullName() + " registered?");
                }
                currentSubscribers.remove(handler);
            } finally {
                lockOnHandlersByClass.writeLock().unlock();
            }
        }
    }

    /**
     * Posts an event and its context to be processed by registered handlers.
     *
     * @param event the event to be handled
     * @param context the context of the event
     */
    public void post(Message event, EventContext context) {

        final Collection<EventHandlerMethod> handlers = getHandlers(EventClass.of(event));

        //TODO:2015-11-06:alexander.yevsyukov: Don't we want to have DeadEvent similar to Guava's EventBus?
        // The logger would be one of the handlers for this.
        if (handlers.isEmpty()) {
            log().warn("No handler defined for event class: " + event.getClass().getName());
            return;
        }

        //TODO:2015-11-06:alexander.yevsyukov: Make async execution. See Guava EventBus.
        for (EventHandlerMethod handler : handlers) {
            try {
                handler.invoke(event, context);
            } catch (InvocationTargetException e) {
                //TODO:2015-11-06:alexander.yevsyukov: Allow configurable exception handlers similarly to Guava.
                log().error("Exception invoking method: " + handler.getFullName(), e);
            }
        }
    }

    private Collection<EventHandlerMethod> getHandlers(EventClass c) {
        return handlersByClass.get(c);
    }

    //TODO:2015-11-06:alexander.yevsyukov: Since we are likely to allow configurable executors to support
    // async execution and distributed event handling, the bus cannot be a singleton.
    // We can require only one instance of the bus per server application, and have its instance
    // passed to Engine. Note that there's Engine.getEventBus() method already.

    /**
     * Returns an singleton instance of the event bus.
     *
     * @return the event bus instance
     */
    public static EventBus getInstance() {
        // on demand holder pattern
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final EventBus value = new EventBus();
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
