/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.server.type.EventClass;

import java.util.Optional;

/**
 * An {@link EventFilter} which allows only events of given types.
 *
 * <p>All the other events are discarded by this filter by default.
 *
 * <p><b>Caution:</b> when using this filter, make sure you're aware of consequences of discarding
 * system events posted by a repository.
 *
 * @see EventBlackList
 */
public final class EventWhiteList implements EventFilter {

    private final ImmutableSet<EventClass> allowedEvents;

    private EventWhiteList(ImmutableSet<EventClass> allowedEvents) {
        this.allowedEvents = allowedEvents;
    }

    /**
     * Creates a new instance of {@code EventWhiteList} allowing events of the given types.
     *
     * @param eventClasses
     *         the allowed event classes
     * @return new instance of the white-list filter
     */
    @SuppressWarnings("WeakerAccess") // Public API of the framework.
    @SafeVarargs
    public static EventWhiteList allowEvents(Class<? extends EventMessage>... eventClasses) {
        ImmutableSet<EventClass> classes = EventClass.setOf(eventClasses);
        return new EventWhiteList(classes);
    }

    @Override
    public Optional<? extends EventMessage> filter(EventMessage event) {
        EventClass type = EventClass.of(event);
        return allowedEvents.contains(type)
               ? Optional.of(event)
               : Optional.empty();
    }
}
