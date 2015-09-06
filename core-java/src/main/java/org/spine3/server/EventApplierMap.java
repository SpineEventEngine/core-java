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
package org.spine3.server;

import com.google.common.collect.Maps;
import org.spine3.Event;
import org.spine3.EventClass;
import org.spine3.error.MissingEventApplierException;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.ServerMethods.scanForEventAppliers;

/**
 * Dispatches the incoming events to the corresponding applier method of an aggregate root.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
class EventApplierMap {

    //TODO:2015-09-06:alexander.yevsyukov: Why concurrent?
    private final Map<EventClass, MessageSubscriber> subscribersByType = Maps.newConcurrentMap();

    /**
     * This method is used to register an aggregated root.
     *
     * @param aggregateRoot the aggregate root object
     */
    public void register(AggregateRoot aggregateRoot) {
        checkNotNull(aggregateRoot);

        Map<EventClass, MessageSubscriber> subscribers = scanForEventAppliers(aggregateRoot);
        checkSubscribers(subscribers);

        putAll(subscribers);
    }

    private void checkSubscribers(Map<EventClass, MessageSubscriber> subscribers) {
        for (Map.Entry<EventClass, MessageSubscriber> entry : subscribers.entrySet()) {
            EventClass eventClass = entry.getKey();

            if (subscriberRegistered(eventClass)) {
                final MessageSubscriber alreadyAddedApplier = getSubscriber(eventClass);
                throw new DuplicateApplierException(eventClass, alreadyAddedApplier, entry.getValue());
            }
        }
    }

    /**
     * Directs the event to the corresponding applier.
     *
     * @param event the event to be applied
     * @throws InvocationTargetException if an exception occurs during event applying
     */
    void apply(Event event) throws InvocationTargetException {
        checkNotNull(event);

        EventClass eventClass = event.getEventClass();
        if (!subscriberRegistered(eventClass)) {
            throw new MissingEventApplierException(event);
        }

        MessageSubscriber subscriber = getSubscriber(eventClass);
        subscriber.handle(event.value());
    }

    private void putAll(Map<EventClass, MessageSubscriber> subscribers) {
        subscribersByType.putAll(subscribers);
    }

    private MessageSubscriber getSubscriber(EventClass eventClass) {
        return subscribersByType.get(eventClass);
    }

    private boolean subscriberRegistered(EventClass eventClass) {
        return subscribersByType.containsKey(eventClass);
    }

}
