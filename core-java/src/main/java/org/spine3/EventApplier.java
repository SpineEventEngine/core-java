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
