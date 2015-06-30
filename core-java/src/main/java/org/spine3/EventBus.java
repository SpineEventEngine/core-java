/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.engine.MessageSubscriber;
import org.spine3.lang.MissingEventApplierException;
import org.spine3.util.Messages;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.spine3.util.Methods.scanForEventSubscribers;

/**
 * Manages incoming events to the appropriate registered applier
 * according to the type of incoming event.
 *
 * @author Mikhail Melnik
 */
public class EventBus {

    /** This code bases on Guava {@link com.google.common.eventbus.EventBus} class. */

    private final Multimap<Class<? extends Message>, MessageSubscriber> subscribersByType = HashMultimap.create();
    private final ReadWriteLock subscribersByTypeLock = new ReentrantReadWriteLock();

    /**
     * Registers all subscriber methods on {@code eventApplier} to receive events.
     *
     * @param eventApplier the event applier object whose subscriber methods should be registered
     */
    public void register(Object eventApplier) {
        Map<Class<? extends Message>, MessageSubscriber> subscribers = scanForEventSubscribers(eventApplier);

        putSubscribersToBus(subscribers);
    }

    private void putSubscribersToBus(Map<Class<? extends Message>, MessageSubscriber> subscribers) {
        subscribersByTypeLock.writeLock().lock();
        try {
            for (Map.Entry<Class<? extends Message>, MessageSubscriber> subscriber : subscribers.entrySet()) {
                subscribersByType.put(subscriber.getKey(), subscriber.getValue());
            }
        } finally {
            subscribersByTypeLock.writeLock().unlock();
        }
    }

    /**
     * Unregisters all subscriber methods on a registered {@code eventApplier}.
     *
     * @param eventApplier object whose subscriber methods should be unregistered
     * @throws IllegalArgumentException if the object was not previously registered
     */
    public void unregister(Object eventApplier) {
        Map<Class<? extends Message>, MessageSubscriber> subscribers = scanForEventSubscribers(eventApplier);

        removeSubscribersFromBus(subscribers);
    }

    private void removeSubscribersFromBus(Map<Class<? extends Message>, MessageSubscriber> subscribers) {
        for (Map.Entry<Class<? extends Message>, MessageSubscriber> entry : subscribers.entrySet()) {
            Class<? extends Message> eventClass = entry.getKey();
            MessageSubscriber subscriber = entry.getValue();

            subscribersByTypeLock.writeLock().lock();
            try {
                Collection<MessageSubscriber> currentSubscribers = subscribersByType.get(eventClass);
                if (!currentSubscribers.contains(subscriber)) {
                    throw new IllegalArgumentException(
                            "missing event subscriber for an annotated method. Is " + subscriber.getFullName() + " registered?");
                }
                currentSubscribers.remove(subscriber);
            } finally {
                subscribersByTypeLock.writeLock().unlock();
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
        Collection<MessageSubscriber> subscribers = getSubscribers(event.getClass());

        if (subscribers.isEmpty()) {
            throw new MissingEventApplierException(event);
        }

        for (MessageSubscriber subscriber : subscribers) {
            try {
                subscriber.handle(event, context);
            } catch (InvocationTargetException e) {
                //NOP
            }
        }
    }

    private Collection<MessageSubscriber> getSubscribers(Class<? extends Message> eventClass) {
        return subscribersByType.get(eventClass);
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

    private EventBus() {
        // Disallow creation of instances from outside.
    }

}
