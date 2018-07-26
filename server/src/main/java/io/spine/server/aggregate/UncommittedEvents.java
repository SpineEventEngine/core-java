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

package io.spine.server.aggregate;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import io.spine.core.Event;
import io.spine.core.Events;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

/**
 * The list of uncommitted events of an {@link Aggregate}.
 *
 * <p>This type is immutable. To add more events to {@code UncommittedEvents}, see
 * {@link #append(Iterable)}.
 *
 * @author Dmytro Dashenkov
 */
final class UncommittedEvents {

    private static final UncommittedEvents EMPTY = new UncommittedEvents(ImmutableList.of(), false);

    private final ImmutableList<Event> events;
    private final boolean rejection;

    private UncommittedEvents(ImmutableList<Event> events, boolean rejection) {
        this.events = events;
        this.rejection = rejection;
    }

    /**
     * @return empty list of events
     */
    static UncommittedEvents ofNone() {
        return EMPTY;
    }

    /**
     * Creates a new instance of {@code UncommittedEvents} with the given events.
     *
     * @param events the events produces by an {@link Aggregate}
     * @return new instance of {@code UncommittedEvents}
     */
    static UncommittedEvents of(Iterable<Event> events) {
        ImmutableList<Event> eventList = copyOf(events);
        boolean rejection = rejection(eventList);
        if (rejection) {
            eventList = ImmutableList.of();
        }
        return new UncommittedEvents(eventList, rejection);
    }

    private static boolean rejection(List<Event> events) {
        boolean singleEvent = events.size() == 1;
        boolean result = singleEvent && Events.isRejection(events.get(0));
        return result;
    }

    /**
     * Tells whether or not this {@code UncommittedEvents} list is empty.
     *
     * @return {@code true} if the list is not empty, {@code false} otherwise
     */
    boolean nonEmpty() {
        return !events.isEmpty() || rejection;
    }

    /**
     * Obtains the list of events.
     *
     * <p>The returned list DOES NOT contain rejection events.
     *
     * @return the list of non-rejection uncommitted events
     */
    List<Event> list() {
        return copyOf(events);
    }

    /**
     * Creates a new instance of {@code UncommittedEvents} with this {@link #list() list} appended
     * with the given {@code events}.
     *
     * <p>If this {@code UncommittedEvents} list is {@link #nonEmpty()} or the given
     * {@code Iterable} is not empty, then the resulting instance is {@link #nonEmpty()} as well.
     *
     * @param events the events to append
     * @return new {@code UncommittedEvents} instance
     */
    UncommittedEvents append(Iterable<Event> events) {
        ImmutableList<Event> newEvents = copyOf(events);
        ImmutableList<Event> newList = ImmutableList
                .<Event>builder()
                .addAll(list())
                .addAll(newEvents)
                .build();
        boolean rejection = rejection(newEvents) || this.rejection;
        return new UncommittedEvents(newList, rejection);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UncommittedEvents events1 = (UncommittedEvents) o;
        return rejection == events1.rejection &&
                Objects.equal(events, events1.events);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(events, rejection);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common terms used all over the core.
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("events", events)
                          .add("rejection", rejection)
                          .toString();
    }
}
