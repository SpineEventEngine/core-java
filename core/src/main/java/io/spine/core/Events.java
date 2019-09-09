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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.EventMessage;
import io.spine.protobuf.Messages;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;

/**
 * Utility class for working with {@link Event} objects.
 */
public final class Events {

    /** The stringifier for event IDs. */
    private static final Stringifier<EventId> idStringifier = new EventIdStringifier();

    static {
        StringifierRegistry.instance()
                           .register(idStringifier(), EventId.class);
    }

    /** Prevents instantiation of this utility class. */
    private Events() {
    }

    /**
     * Creates a new {@link EventId} based on random UUID.
     *
     * @return new UUID-based event ID
     */
    public static EventId generateId() {
        String value = UUID.randomUUID()
                           .toString();
        return EventId.newBuilder()
                      .setValue(value)
                      .build();
    }

    /**
     * Extracts an event message if the passed instance is an {@link Event} object or {@link Any},
     * otherwise returns the passed message.
     */
    public static EventMessage ensureMessage(Message eventOrMessage) {
        checkNotNull(eventOrMessage);
        if (eventOrMessage instanceof Event) {
            return ((Event) eventOrMessage).enclosedMessage();
        }
        Message unpacked = Messages.ensureMessage(eventOrMessage);
        return (EventMessage) unpacked;
    }

    /**
     * Obtains the stringifier for event IDs.
     */
    public static Stringifier<EventId> idStringifier() {
        return idStringifier;
    }

    /**
     * Ensures that the passed ID is valid.
     *
     * @param id
     *         an ID to check
     * @throws IllegalArgumentException
     *         if the ID string value is empty or blank
     */
    public static EventId checkValid(EventId id) {
        checkNotNull(id);
        checkNotEmptyOrBlank(id.getValue(), "event ID");
        return id;
    }

    /**
     * Creates an empty {@link Iterable} over the messages of the type {@code <M>}.
     *
     * <p>This method is useful for returning empty result from reacting methods.
     *
     * @param <M>
     *         the type of messages
     * @return empty {@link Iterable}
     */
    public static <M extends EventMessage> Iterable<M> nothing() {
        return ImmutableList.of();
    }

    @Internal
    public static ImmutableList<Event> toExternal(List<Event> events) {
        return events
                .stream()
                .map(Events::toExternal)
                .collect(toImmutableList());
    }

    @Internal
    public static Event toExternal(Event event) {
        Event.Builder externalEvent = event.toBuilder();
        externalEvent.getContextBuilder().setExternal(true);
        return externalEvent.build();
    }

    /**
     * The stringifier of event IDs.
     */
    static class EventIdStringifier extends Stringifier<EventId> {

        @Override
        protected String toString(EventId eventId) {
            String result = eventId.getValue();
            return result;
        }

        @Override
        protected EventId fromString(String str) {
            EventId result = EventId.newBuilder()
                                    .setValue(str)
                                    .build();
            return result;
        }
    }
}
