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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.spine3.base.EventClass;
import org.spine3.server.reflect.EventSubscriberMethod;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The registry of event subscribers by event class.
 *
 * <p>There can be multiple subscribers per event class.
 *
 * @author Alexander Yevsyukov
 */
class SubscriberRegistry {

    private final Multimap<EventClass, EventSubscriber> subscribers =
            Multimaps.synchronizedMultimap(HashMultimap.<EventClass, EventSubscriber>create());

    void subscribe(EventSubscriber object) {
        checkNotNull(object);
        final ImmutableSet<EventClass> eventClasses = getEventClasses(object);
        for (EventClass eventClass : eventClasses) {
            subscribers.put(eventClass, object);
        }
    }

    void unsubscribe(EventSubscriber object) {
        final ImmutableSet<EventClass> eventClasses = getEventClasses(object);
        for (EventClass eventClass : eventClasses) {
            subscribers.remove(eventClass, object);
        }
    }

    /**
     * Ensures that the passed object subscribes to one or more events
     * and returns their classes.
     */
    private static ImmutableSet<EventClass> getEventClasses(EventSubscriber object) {
        final ImmutableSet<EventClass> eventClasses =
                EventSubscriberMethod.getEventClasses(object.getClass());
        checkSubscribersNotEmpty(object, eventClasses.isEmpty());
        return eventClasses;
    }

    void unsubscribeAll() {
        subscribers.clear();
        EventBus.log().info("All subscribers cleared.");
    }

    Set<EventSubscriber> getSubscribers(EventClass c) {
        return ImmutableSet.copyOf(subscribers.get(c));
    }

    boolean hasSubscribers(EventClass eventClass) {
        final Set<EventSubscriber> subscribers = getSubscribers(eventClass);
        return !subscribers.isEmpty();
    }

    private static void checkSubscribersNotEmpty(Object object, boolean subscribersEmpty) {
        checkArgument(!subscribersEmpty,
                      "No event subscriber methods found in %s", object);
    }
}
