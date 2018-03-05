/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import io.spine.core.Event;

import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

import static com.google.common.collect.Queues.newArrayDeque;

/**
 * Recent history of an {@linkplain io.spine.server.entity.EventPlayingEntity event-sourced entity}.
 *
 * @author Mykhailo Drachuk
 * @author Alexander Yevsyukov
 */
public final class RecentHistory {

    /**
     * Holds the history of all events which happened to the aggregate since the last snapshot.
     *
     * <p>Most recent event comes first.
     */
    private final Deque<Event> history = newArrayDeque();

    /**
     * Creates new instance.
     */
    RecentHistory() {
        super();
    }

    /**
     * Returns {@code true} if there are no events in the recent history, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return history.isEmpty();
    }

    /**
     * Removes all events from the recent history.
     */
    public void clear() {
        history.clear();
    }

    /**
     * Creates a new iterator over the recent history items.
     *
     * @return an events iterator
     */
    public Iterator<Event> iterator() {
        final ImmutableList<Event> events = ImmutableList.copyOf(history);
        return events.iterator();
    }

    /**
     * Adds events to the aggregate history.
     *
     * @param events events in the chronological order
     */
    void addAll(Iterable<Event> events) {
        for (Event event : events) {
            history.addFirst(event);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(history);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RecentHistory other = (RecentHistory) obj;
        return Objects.equals(this.history, other.history);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("history", history)
                          .toString();
    }
}
