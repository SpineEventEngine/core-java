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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import io.spine.core.Event;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

/**
 * The list of uncommitted events of an {@link Aggregate}.
 *
 * @author Dmytro Dashenkov
 */
@Immutable
final class UncommittedEvents {

    private static final UncommittedEvents EMPTY =
            new UncommittedEvents(ImmutableList.of());

    private final ImmutableList<Event> events;

    private UncommittedEvents(ImmutableList<Event> events) {
        this.events = events;
    }

    /**
     * @return empty list of events
     */
    static UncommittedEvents ofNone() {
        return EMPTY;
    }

    /**
     * Tells whether or not this {@code UncommittedEvents} list is empty.
     *
     * @return {@code true} if the list is not empty, {@code false} otherwise
     */
    boolean nonEmpty() {
        return !events.isEmpty();
    }

    /**
     * Obtains the list of events.
     *
     * @return the list of uncommitted events
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
        return new UncommittedEvents(newList);
    }
}
