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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;

import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * @author Dmytro Dashenkov
 */
public final class EventWhiteList implements EventFilter {

    private final ImmutableSet<EventClass> allowedEvents;

    private EventWhiteList(ImmutableSet<EventClass> allowedEvents) {
        this.allowedEvents = allowedEvents;
    }

    @SafeVarargs
    public static EventWhiteList allowEvents(Class<? extends Message>... eventClasses) {
        ImmutableSet<EventClass> classes = EventClass.setOf(eventClasses);
        return new EventWhiteList(classes);
    }

    @Override
    public Optional<Event> filter(Event event) {
        EventClass type = EventClass.of(event);
        return allowedEvents.contains(type)
               ? of(event)
               : empty();
    }
}
