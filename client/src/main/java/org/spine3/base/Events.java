/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.base;

import com.google.common.base.Predicate;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.type.EventClass;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Timestamps.isBetween;

/**
 * Utility class for working with {@link Event} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class Events {

    private Events() {
    }

    /**
     * Compares two events by their timestamps.
     */
    public static final Comparator<Event> EVENT_TIMESTAMP_COMPARATOR = new Comparator<Event>() {
        @Override
        public int compare(Event o1, Event o2) {
            final Timestamp timestamp1 = getTimestamp(o1);
            final Timestamp timestamp2 = getTimestamp(o2);
            return Timestamps.compare(timestamp1, timestamp2);
        }
    };

    /**
     * Generates a new random UUID-based {@code EventId}.
     */
    public static EventId generateId() {
        final String value = UUID.randomUUID().toString();
        return EventId.newBuilder().setUuid(value).build();
    }

    /**
     * Sorts the given event record list by the event timestamps.
     *
     * @param events the event record list to sort
     */
    public static void sort(List<Event> events) {
        Collections.sort(events, EVENT_TIMESTAMP_COMPARATOR);
    }

    /**
     * Obtains the timestamp of the event.
     */
    public static Timestamp getTimestamp(Event event) {
        final Timestamp result = event.getContext().getTimestamp();
        return result;
    }

    /**
     * Creates {@code Event} instance with the passed event and context.
     */
    public static Event createEvent(Message event, EventContext context) {
        final Event result = Event.newBuilder()
                .setMessage(Messages.toAny(event))
                .setContext(context)
                .build();
        return result;
    }

    /**
     * Extracts the event instance from the passed record.
     */
    public static Message getMessage(Event event) {
        final Any any = event.getMessage();
        final Message result = Messages.fromAny(any);
        return result;
    }

    /**
     * Determines the class of the event from the passed event record.
     */
    public static EventClass getEventClass(Event event) {
        final Message message = getMessage(event);
        final EventClass result = EventClass.of(message);
        return result;
    }

    /**
     * The predicate to filter event records after some point in time.
     */
    public static class IsAfter implements Predicate<Event> {

        private final Timestamp timestamp;

        public IsAfter(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable Event record) {
            if (record == null) {
                return false;
            }
            final Timestamp ts = getTimestamp(record);
            final boolean result = Timestamps.compare(ts, this.timestamp) > 0;
            return result;
        }
    }

    /**
     * The predicate to filter event records before some point in time.
     */
    public static class IsBefore implements Predicate<Event> {

        private final Timestamp timestamp;

        public IsBefore(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable Event record) {
            if (record == null) {
                return false;
            }

            final Timestamp ts = getTimestamp(record);
            final boolean result = Timestamps.compare(ts, this.timestamp) < 0;
            return result;
        }
    }

    /**
     * The predicate to filter event records within a given time range.
     */
    public static class IsBetween implements Predicate<Event> {

        private final Timestamp start;
        private final Timestamp finish;

        public IsBetween(Timestamp start, Timestamp finish) {
            checkNotNull(start);
            checkNotNull(finish);
            checkArgument(Timestamps.compare(start, finish) < 0, "`start` must be before `finish`");
            this.start = start;
            this.finish = finish;
        }

        @Override
        public boolean apply(@Nullable Event event) {
            if (event == null) {
                return false;
            }

            final Timestamp ts = getTimestamp(event);
            final boolean result = isBetween(ts, start, finish);
            return result;
        }
    }

}
