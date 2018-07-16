/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.copyOf;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Contains the data on the events emitted in the {@link BlackBoxBoundedContext Bounded Context}.
 * It can be queried for event presence, count and structure.
 *
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public class EmittedEvents {

    private final List<Event> events;
    private final Map<EventClass, Integer> countOfTypes;

    EmittedEvents(List<Event> events) {
        this.events = copyOf(events);
        this.countOfTypes = countEventTypes(events);
    }

    /**
     * Counts the number of times the domain event types are included in the provided list.
     *
     * @param events a list of {@link Event}
     * @return a mapping of Event classes to its count
     */
    private static Map<EventClass, Integer> countEventTypes(List<Event> events) {
        Map<EventClass, Integer> countForType = new HashMap<>();
        for (Event event : events) {
            Message message = unpack(event.getMessage());
            EventClass type = EventClass.of(message);
            int currentCount = countForType.getOrDefault(type, 0);
            countForType.put(type, currentCount + 1);
        }
        return ImmutableMap.copyOf(countForType);
    }

    /**
     * @return total number of executed events
     */
    public int count() {
        return events.size();
    }

    /**
     * Counts the number of times the events of the provided type were executed.
     *
     * @param eventType a class of the fired domain event
     * @return number of times the provided event was executed
     */
    public int count(Class<? extends Message> eventType) {
        return count(EventClass.of(eventType));
    }

    /**
     * Counts the number of times the events of the provided {@link EventClass} were executed.
     *
     * @param eventClass an event class wrapping the event
     * @return number of times the provided event was executed
     */
    public int count(EventClass eventClass) {
        if (!contain(eventClass)) {
            return 0;
        }
        return countOfTypes.get(eventClass);
    }

    /**
     * @param eventType a class representing a domain event
     * @return {@code true} if the event of provided type was emitted in the Bounded Context,
     * {@code false} otherwise
     */
    public boolean contain(Class<? extends Message> eventType) {
        return contain(EventClass.of(eventType));
    }

    /**
     * @param eventClass an {@link EventClass EventClass} instance for a domain event
     * @return {@code true} if the event of provided type was emitted in the Bounded Context,
     * {@code false} otherwise
     */
    public boolean contain(EventClass eventClass) {
        return countOfTypes.containsKey(eventClass);
    }
}
