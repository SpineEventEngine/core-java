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

package io.spine.server.type;

import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.core.Events;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static io.spine.core.Events.isRejection;

/**
 * A set of combined event and rejection classes.
 *
 * <p>In the {@link io.spine.type.MessageClass} hierarchy events and rejections are independent,
 * but in Spine Model {@linkplain io.spine.base.EventMessage event messages} include both of these.
 *
 * <p>This class thus offers a convenient way for working with a combined event and rejection
 * set.
 */
public final class EventClassSet implements Serializable {

    private static final long serialVersionUID = 0L;

    private final Set<EventClass> eventClasses;
    private final Set<RejectionClass> rejectionClasses;

    public EventClassSet() {
        this.eventClasses = new HashSet<>();
        this.rejectionClasses = new HashSet<>();
    }

    public void addAll(Iterable<Class<? extends EventMessage>> classes) {
        stream(classes)
                .filter(cls -> !RejectionMessage.class.isAssignableFrom(cls))
                .map(EventClass::from)
                .forEach(eventClasses::add);
        stream(classes)
                .filter(RejectionMessage.class::isAssignableFrom)
                .map(RejectionMessage.class::cast)
                .map(RejectionClass::of)
                .forEach(rejectionClasses::add);
    }

    /**
     * Checks if any of the specified events or rejections are present in this set.
     */
    public boolean containsAnyOf(Iterable<Event> events) {
        return containsAnyEvent(events) || containsAnyRejection(events);
    }

    private boolean containsAnyEvent(Iterable<Event> events) {
        Optional<EventClass> matchedEvent =
                stream(events)
                        .filter(event -> !isRejection(event))
                        .map(EventClass::from)
                        .filter(eventClasses::contains)
                        .findAny();
        return matchedEvent.isPresent();
    }

    private boolean containsAnyRejection(Iterable<Event> events) {
        Optional<RejectionClass> matchedRejection =
                stream(events)
                        .filter(Events::isRejection)
                        .map(RejectionClass::from)
                        .filter(rejectionClasses::contains)
                        .findAny();
        return matchedRejection.isPresent();
    }

    /**
     * Checks if rejection's enclosed message type is present among the {@code rejectionClasses}.
     */
    public boolean contains(ThrowableMessage rejection) {
        RejectionClass rejectionClass = RejectionClass.of(rejection);
        boolean result = rejectionClasses.contains(rejectionClass);
        return result;
    }
}
