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

package io.spine.server.event;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.server.aggregate.AggregateStateRecord;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * @author Dmytro Dashenkov
 */
public final class EventStream {

    private final List<Event> events;

    private EventStream(List<Event> events) {
        this.events = events;
    }

    private EventStream(Builder builder) {
        this(copyOf(builder.events));
    }

    public static EventStream from(Iterable<? extends Event> events) {
        final List<Event> stream = copyOf(events);
        return new EventStream(stream);
    }

    public static EventStream from(AggregateStateRecord record) {
        final List<Event> events = record.getEventList();
        return new EventStream(events);
    }

    public static EventStream empty() {
        return Empty.INSTANCE.value;
    }

    public static EventStream of(Event... events) {
        final List<Event> eventList = ImmutableList.copyOf(events);
        return new EventStream(eventList);
    }

    public EventStream concat(EventStream other) {
        final int expectedSize = this.events.size() + other.events.size();
        final List<Event> events = newArrayListWithCapacity(expectedSize);
        events.addAll(this.events);
        events.addAll(other.events);
        final EventStream result = new EventStream(events);
        return result;
    }

    public int count() {
        return events.size();
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public List<Event> events() {
        return unmodifiableList(events);
    }

    /**
     * Creates a {@code EventStream} builder.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code EventStream} instances.
     */
    public static final class Builder {

        private final List<Event> events = newLinkedList();

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        @CanIgnoreReturnValue
        public Builder add(Event event) {
            events.add(event);
            return this;
        }

        /**
         * Creates a new instance of {@code EventStream}.
         *
         * @return new instance of {@code EventStream}
         */
        public EventStream build() {
            return new EventStream(this);
        }
    }

    private enum Empty {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final EventStream value = new EventStream(emptyList());
    }
}
