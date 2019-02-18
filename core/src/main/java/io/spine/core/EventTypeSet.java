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

package io.spine.core;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static io.spine.core.Events.isRejection;

/**
 * A set of combined event and rejection classes.
 */
public final class EventTypeSet {

    private final ImmutableSet<EventClass> eventClasses;
    private final ImmutableSet<RejectionClass> rejectionClasses;

    private EventTypeSet(ImmutableSet<EventClass> eventClasses,
                         ImmutableSet<RejectionClass> rejectionClasses) {
        this.eventClasses = eventClasses;
        this.rejectionClasses = rejectionClasses;
    }

    public static EventTypeSet of(ImmutableSet<EventClass> eventClasses,
                                  ImmutableSet<RejectionClass> rejectionClasses) {
        checkNotNull(eventClasses);
        checkNotNull(rejectionClasses);
        return new EventTypeSet(eventClasses, rejectionClasses);
    }

    public boolean matchesAnyOf(Iterable<Event> events) {
        return matchesAnyEvent(events) || matchesAnyRejection(events);
    }

    private boolean matchesAnyEvent(Iterable<Event> events) {
        Optional<EventClass> matchedEvent =
                stream(events)
                        .filter(event -> !isRejection(event))
                        .map(EventClass::from)
                        .filter(eventClasses::contains)
                        .findAny();
        return matchedEvent.isPresent();
    }

    private boolean matchesAnyRejection(Iterable<Event> events) {
        Optional<RejectionClass> matchedRejection =
                stream(events)
                        .filter(Events::isRejection)
                        .map(RejectionClass::from)
                        .filter(rejectionClasses::contains)
                        .findAny();
        return matchedRejection.isPresent();
    }
}
