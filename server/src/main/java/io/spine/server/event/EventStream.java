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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.server.aggregate.AggregateStateRecord;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

/**
 * A sequence of {@linkplain Event events}.
 *
 * <p>A stream is limited to a number of events (i.e. is not infinite).
 *
 * <p>This type is immutable. All the operations either return a view on this stream or create
 * a new stream based on the given one.
 *
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

    /**
     * Creates a new {@code EventStream} from the given events.
     *
     * <p>The ordering of the events in the stream corresponds to the ordering in the given
     * {@code Iterable}.
     *
     * @param events the events to wrap into a stream
     * @return new {@code EventStream}
     */
    public static EventStream from(Iterable<? extends Event> events) {
        final List<Event> stream = copyOf(events);
        return new EventStream(stream);
    }

    /**
     * Creates a new {@code EventStream} from the {@link AggregateStateRecord}.
     *
     * <p>The ordering of the events in the stream corresponds to the ordering in the given
     * record.
     *
     * @param record the aggregate state record to convert into a stream of events
     * @return new {@code EventStream}
     */
    public static EventStream from(AggregateStateRecord record) {
        final List<Event> events = record.getEventList();
        final EventStream result = new EventStream(events);
        return result;
    }

    /**
     * Creates a new {@code EventStream} from the given events.
     *
     * <p>The ordering of the events in the stream corresponds to their ordering as the method
     * arguments.
     *
     * @param events the events to wrap into a stream
     * @return new {@code EventStream}
     */
    public static EventStream of(Event... events) {
        final List<Event> eventList = ImmutableList.copyOf(events);
        return new EventStream(eventList);
    }

    /**
     * Retrieves an empty {@code EventStream}.
     *
     * @return an empty {@code EventStream}
     */
    public static EventStream empty() {
        return Empty.INSTANCE.value;
    }

    /**
     * Concatenates this stream with the given one.
     *
     * <p>The resulting stream outputs the events from current stream and then the events from
     * the {@code other} one.
     *
     * <p>Neither current instance nor the {@code other} one is changed. Instead, the concatenation
     * result is returned as a new object.
     *
     * @param other the stream to concat with
     * @return concatenated stream
     */
    public EventStream concat(EventStream other) {
        checkNotNull(other);
        if (this.isEmpty()) {
            return other;
        } else if (other.isEmpty()) {
            return this;
        } else {
            final int expectedSize = this.events.size() + other.events.size();
            final List<Event> events = newArrayListWithCapacity(expectedSize);
            events.addAll(this.events);
            events.addAll(other.events);
            final EventStream result = new EventStream(events);
            return result;
        }
    }

    /**
     * Retrieves the number of events in this stream.
     *
     * @return the stream count
     */
    public int count() {
        return events.size();
    }

    /**
     * Indicates whether or not this stream is empty.
     *
     * @return {@code true} if the stream contain no events, {@code false} otherwise
     */
    public boolean isEmpty() {
        return count() == 0;
    }

    /**
     * Retrieves the events of the current stream.
     *
     * @return an immutable list of the events of this stream
     */
    public List<Event> events() {
        return unmodifiableList(events);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventStream that = (EventStream) o;
        return Objects.equal(events, that.events);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(events);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String template = "EventStream[%s]";
        final String eventTypes = events.stream()
                                        .map(Event::getMessage)
                                        .map(Any::getTypeUrl)
                                        .collect(joining(", "));
        final String result = format(template, eventTypes);
        return result;
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

        /**
         * Adds the given event to the built stream.
         *
         * <p>The order of events added to the stream is the order of events in the stream.
         *
         * @param event the event to add
         * @return self for method chaining
         */
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

    /**
     * The holder of the empty stream singleton value.
     *
     * <p>Note that it's still possible to create another instance of an event stream which would be
     * empty. Do not rely on this object identity.
     */
    private enum Empty {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final EventStream value = new EventStream(emptyList());
    }
}
