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

import com.google.common.collect.HashMultimap;
import org.spine3.server.type.EventClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The registry of objects that dispatch event to handlers.
 *
 * <p>There can be multiple dispatchers per event class.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class DispatcherRegistry {

    private final HashMultimap<EventClass, EventDispatcher> dispatchers = HashMultimap.create();

    /* package */ void register(EventDispatcher dispatcher) {
        checkNotNull(dispatcher);
        final Set<EventClass> eventClasses = dispatcher.getEventClasses();
        checkNotEmpty(dispatcher, eventClasses);

        for (EventClass eventClass : eventClasses) {
            dispatchers.put(eventClass, dispatcher);
        }
    }

    /* package */ Set<EventDispatcher> getDispatchers(EventClass eventClass) {
        final Set<EventDispatcher> result = this.dispatchers.get(eventClass);
        return result;
    }

    /* package */ void unregister(EventDispatcher dispatcher) {
        final Set<EventClass> eventClasses = dispatcher.getEventClasses();
        checkNotEmpty(dispatcher, eventClasses);
        for (EventClass eventClass : eventClasses) {
            dispatchers.remove(eventClass, dispatcher);
        }
    }

    /* package */ void unregisterAll() {
        dispatchers.clear();
    }

    /* package */ boolean hasDispatchersFor(EventClass eventClass) {
        final Set<EventDispatcher> dispatchers = getDispatchers(eventClass);
        final boolean result = !dispatchers.isEmpty();
        return result;
    }

    /**
     * Ensures that the dispatcher forwards at least one event.
     *
     * @throws IllegalArgumentException if the dispatcher returns empty set of event classes
     * @throws NullPointerException if the dispatcher returns null set
     */
    private static void checkNotEmpty(EventDispatcher dispatcher, Set<EventClass> eventClasses) {
        checkArgument(!eventClasses.isEmpty(),
                "No event classes are forwarded by this dispatcher: %s", dispatcher);
    }
}
