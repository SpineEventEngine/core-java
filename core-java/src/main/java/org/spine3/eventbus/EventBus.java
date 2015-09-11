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
import org.spine3.EventClass;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.internal.EventHandlerMethod;
import org.spine3.protobuf.Messages;
import org.spine3.server.aggregate.error.MissingEventApplierException;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.spine3.internal.EventHandlerMethod.scan;

/**
 * Manages incoming events to the appropriate registered handler
 * according to the type of incoming event.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyuov
 */
public class EventBus {

    /* This code is based on Guava {@link com.google.common.eventbus.EventBus} class. */

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
        Map<EventClass, EventHandlerMethod> handlers = scan(object);

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
     * Unregisters all subscriber methods on a registered {@code eventHandler}.
     *
     * @param eventHandler object whose subscriber methods should be unregistered
     * @throws IllegalArgumentException if the object was not previously registered
     */
    public void unregister(Object eventHandler) {
        Map<EventClass, EventHandlerMethod> handlers = scan(eventHandler);

        unsubscribe(handlers);
    }

    /**
     * Removes passed event handlers from the bus.
     * @param handlers a map of the event handlers to remove
     */
    private void unsubscribe(Map<EventClass, EventHandlerMethod> handlers) {
        for (Map.Entry<EventClass, EventHandlerMethod> entry : handlers.entrySet()) {
            final EventClass c = entry.getKey();
            EventHandlerMethod handler = entry.getValue();

            lockOnHandlersByClass.writeLock().lock();
            try {
                Collection<EventHandlerMethod> currentSubscribers = handlersByClass.get(c);
                if (!currentSubscribers.contains(handler)) {
                    throw new IllegalArgumentException(
                            "missing event handler for the annotated method. Is " + handler.getFullName() + " registered?");
                }
                currentSubscribers.remove(handler);
            } finally {
                lockOnHandlersByClass.writeLock().unlock();
            }
        }
    }

    /**
     * Posts an event to be processed by registered event appliers.
     *
     * @param eventRecord the event record to be applied by all subscribers
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public void post(EventRecord eventRecord) {
        Message event = Messages.fromAny(eventRecord.getEvent());
        EventContext context = eventRecord.getContext();

        post(event, context);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private void post(Message event, EventContext context) {
        Collection<EventHandlerMethod> handlers = getHandlers(EventClass.of(event));

        if (handlers.isEmpty()) {
            //TODO:2015-09-09:alexander.yevsyukov: This must be missing event handler
            throw new MissingEventApplierException(event);
        }

        for (EventHandlerMethod handler : handlers) {
            try {
                handler.handle(event, context);
            } catch (InvocationTargetException e) {
                //TODO:2015-09-09:alexander.yevsyukov: Don't we want to handle this somehow? At least log?
                //NOP
            }
        }
    }

    private Collection<EventHandlerMethod> getHandlers(EventClass c) {
        return handlersByClass.get(c);
    }

    /**
     * Returns an singleton instance of the event bus.
     *
     * @return the event bus instance
     */
    public static EventBus instance() {
        // on demand holder pattern
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final EventBus value = new EventBus();
    }

}
