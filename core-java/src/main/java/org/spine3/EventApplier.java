/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3;

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.AggregateRoot;
import org.spine3.engine.MessageSubscriber;
import org.spine3.lang.EventApplierAlreadyRegisteredException;
import org.spine3.lang.MissingEventApplierException;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Methods.scanForEventSubscribers;

/**
 * Dispatches the incoming events to the corresponding applier method of an aggregate root.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
class EventApplier {

    private final Map<Class<? extends Message>, MessageSubscriber> subscribersByType = Maps.newConcurrentMap();

    /**
     * This method is used to register an aggregated root.
     *
     * @param aggregateRoot the aggregate root object
     */
    public void register(AggregateRoot aggregateRoot) {
        checkNotNull(aggregateRoot);

        Map<Class<? extends Message>, MessageSubscriber> subscribers = getEventSubscribers(aggregateRoot);
        checkSubscribers(subscribers);

        putAll(subscribers);
    }

    private static Map<Class<? extends Message>, MessageSubscriber> getEventSubscribers(AggregateRoot aggregateRoot) {
        Map<Class<? extends Message>, MessageSubscriber> result = scanForEventSubscribers(aggregateRoot);
        return result;
    }

    private void checkSubscribers(Map<Class<? extends Message>, MessageSubscriber> subscribers) {
        for (Map.Entry<Class<? extends Message>, MessageSubscriber> entry : subscribers.entrySet()) {
            Class<? extends Message> eventClass = entry.getKey();

            if (subscriberRegistered(eventClass)) {
                final MessageSubscriber alreadyAddedApplier = getSubscriber(eventClass);
                throw new EventApplierAlreadyRegisteredException(eventClass, alreadyAddedApplier, entry.getValue());
            }
        }
    }

    /**
     * Directs the event to the corresponding applier.
     *
     * @param event the event to be applied
     * @throws InvocationTargetException if an exception occurs during event applying
     */
    public void apply(Message event) throws InvocationTargetException {

        checkNotNull(event);

        Class<? extends Message> eventClass = event.getClass();
        if (!subscriberRegistered(eventClass)) {
            throw new MissingEventApplierException(event);
        }

        MessageSubscriber subscriber = getSubscriber(eventClass);
        subscriber.handle(event);
    }

    private void putAll(Map<Class<? extends Message>, MessageSubscriber> subscribers) {
        subscribersByType.putAll(subscribers);
    }

    private MessageSubscriber getSubscriber(Class<? extends Message> eventClass) {
        return subscribersByType.get(eventClass);
    }

    private boolean subscriberRegistered(Class<? extends Message> eventClass) {
        return subscribersByType.containsKey(eventClass);
    }

}
