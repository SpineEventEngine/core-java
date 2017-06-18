/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.base;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import io.spine.time.Timestamps2;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;

/**
 * Utility class for working with {@link Event} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public final class Events {

    /** Compares two events by their timestamps. */
    private static final Comparator<Event> eventComparator = new Comparator<Event>() {
        @Override
        public int compare(Event o1, Event o2) {
            final Timestamp timestamp1 = getTimestamp(o1);
            final Timestamp timestamp2 = getTimestamp(o2);
            return Timestamps2.compare(timestamp1, timestamp2);
        }
    };

    /** The stringifier for event IDs. */
    private static final Stringifier<EventId> idStringifier = new EventIdStringifier();

    static {
        StringifierRegistry.getInstance()
                           .register(idStringifier(), EventId.class);
    }

    private Events() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Sorts the given event record list by the event timestamps.
     *
     * @param events the event record list to sort
     */
    public static void sort(List<Event> events) {
        checkNotNull(events);
        Collections.sort(events, eventComparator());
    }

    /**
     * Returns comparator which compares events by their timestamp
     * in chronological order.
     */
    public static Comparator<Event> eventComparator() {
        return eventComparator;
    }

    /**
     * Obtains the timestamp of the event.
     */
    public static Timestamp getTimestamp(Event event) {
        checkNotNull(event);
        final Timestamp result = event.getContext()
                                      .getTimestamp();
        return result;
    }

    /**
     * Extracts the event message from the passed event.
     *
     * @param event an event to get message from
     */
    public static <M extends Message> M getMessage(Event event) {
        checkNotNull(event);
        final Any any = event.getMessage();
        final M result = unpack(any);
        return result;
    }

    /**
     * Obtains the actor user ID from the passed {@code EventContext}.
     *
     * <p>The 'actor' is the user who sent the command, which generated the event which context is
     * passed to this method.
     *
     * <p>This is a convenience method for obtaining actor in event subscriber methods.
     */
    public static UserId getActor(EventContext context) {
        checkNotNull(context);
        final CommandContext commandContext = checkNotNull(context).getCommandContext();
        return commandContext.getActorContext()
                             .getActor();
    }

    /**
     * Obtains event producer ID from the passed {@code EventContext} and casts it to the
     * {@code <I>} type.
     *
     * @param context the event context to to get the event producer ID
     * @param <I>     the type of the producer ID wrapped in the passed {@code EventContext}
     * @return the producer ID
     */
    public static <I> I getProducer(EventContext context) {
        checkNotNull(context);
        final Object aggregateId = Identifier.unpack(context.getProducerId());
        @SuppressWarnings("unchecked")
        // It is the caller's responsibility to know the type of the wrapped ID.
        final I id = (I) aggregateId;
        return id;

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
     * @param id an ID to check
     * @throws IllegalArgumentException if the ID string value is empty or blank
     */
    public static EventId checkValid(EventId id) {
        checkNotNull(id);
        checkNotEmptyOrBlank(id.getValue(), "event ID");
        return id;
    }

    /**
     * Obtains type name from the message of the passed event.
     */
    public static TypeName typeNameOf(Event event) {
        checkNotNull(event);
        return TypeName.from(TypeUrl.ofEvent(event));
    }

    /**
     * The stringifier of event IDs.
     */
    static class EventIdStringifier extends Stringifier<EventId> {
        @Override
        protected String toString(EventId eventId) {
            final String result = eventId.getValue();
            return result;
        }

        @Override
        protected EventId fromString(String str) {
            final EventId result = EventId.newBuilder()
                                          .setValue(str)
                                          .build();
            return result;
        }
    }
}
