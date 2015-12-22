/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.*;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.storage.EventStoreRecord;
import org.spine3.server.storage.EventStoreRecordOrBuilder;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.util.Identifiers.*;

/**
 * Utility class for working with {@link EventId} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"UtilityClass", "TypeMayBeWeakened"})
public class Events {

    private Events() {
    }

    static {
        IdConverterRegistry.getInstance().register(EventId.class, new EventIdToStringConverter());
    }

    /**
     * Generates new {@link EventId} by the passed {@link CommandId} and current system time.
     *
     * @param commandId ID of the command, which originated the event
     * @return new event ID
     */
    public static EventId generateId(CommandId commandId) {
        return createId(commandId, getCurrentTime());
    }

    /**
     * Creates new {@link EventId} by the passed {@link CommandId} and passed timestamp.
     *
     * @param commandId ID of the command, which originated the event
     * @param timestamp the moment of time the event happened
     * @return new event ID
     */
    public static EventId createId(CommandId commandId, Timestamp timestamp) {
        final Duration distance = TimeUtil.distance(commandId.getTimestamp(), checkNotNull(timestamp));
        final long delta = TimeUtil.toNanos(distance);

        final EventId.Builder builder = EventId.newBuilder()
                .setCommandId(checkNotNull(commandId))
                .setDeltaNanos(delta);
        return builder.build();
    }

    /**
     * Obtains the timestamp of event ID generation.
     *
     * <p>The timestamp is calculated as a sum of command ID generation timestamp and
     * delta returned by {@link EventId#getDeltaNanos()}.
     *
     * @param eventId ID of the event
     * @return timestamp of event ID generation
     */
    public static Timestamp getTimestamp(EventId eventId) {
        final Timestamp commandTimestamp = eventId.getCommandId().getTimestamp();
        final Duration delta = TimeUtil.createDurationFromNanos(eventId.getDeltaNanos());
        final Timestamp result = TimeUtil.add(commandTimestamp, delta);
        return result;
    }

    /**
     * Calculates the timestamp of the event from the passed record.
     */
    public static Timestamp getTimestamp(EventRecord record) {
        final Timestamp result = getTimestamp(record.getContext().getEventId());
        return result;
    }

    /**
     * Compares two event records by their timestamps.
     */
    public static final Comparator<EventRecord> EVENT_RECORD_COMPARATOR = new Comparator<EventRecord>() {
        @Override
        public int compare(EventRecord o1, EventRecord o2) {
            final Timestamp timestamp1 = getTimestamp(o1);
            final Timestamp timestamp2 = getTimestamp(o2);
            return Timestamps.compare(timestamp1, timestamp2);
        }
    };

    /**
     * Sorts the given event record list by the event timestamps.
     *
     * @param eventRecords the event record list to sort
     */
    public static void sort(List<EventRecord> eventRecords) {
        Collections.sort(eventRecords, EVENT_RECORD_COMPARATOR);
    }

    /**
     * The predicate to filter event records after some point in time.
     */
    public static class IsAfter implements Predicate<EventRecord> {

        private final Timestamp timestamp;

        public IsAfter(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable EventRecord record) {
            if (record == null) {
                return false;
            }

            final Timestamp ts = getTimestamp(record);
            final boolean result = Timestamps.compare(ts, this.timestamp) > 0;
            return result;
        }

    }
    /**
     * Converts {@code EventId} into Json string.
     *
     * @param id the id to convert
     * @return Json representation of the id
     */
    @SuppressWarnings("TypeMayBeWeakened") // We want to limit the number of types that can be converted to Json.
    public static String idToString(EventId id) {
        return Identifiers.idToString(id);
    }

    /**
     * Creates {@code EventRecord} instance with the passed event and context.
     */
    public static EventRecord createEventRecord(Message event, EventContext context) {
        final EventRecord result = EventRecord.newBuilder()
                .setEvent(Messages.toAny(event))
                .setContext(context)
                .build();
        return result;
    }

    /**
     * Extracts the event instance from the passed record.
     */
    public static Message getEvent(EventRecordOrBuilder eventRecord) {
        final Any any = eventRecord.getEvent();
        final Message result = Messages.fromAny(any);
        return result;
    }

    /**
     * Converts EventStoreRecord to EventRecord.
     */
    public static EventRecord toEventRecord(EventStoreRecordOrBuilder record) {
        final EventRecord.Builder builder = EventRecord.newBuilder()
                .setEvent(record.getEvent())
                .setContext(record.getContext());

        return builder.build();
    }

    /**
     * Converts EventStoreRecords to EventRecords.
     */
    public static List<EventRecord> toEventRecordsList(List<EventStoreRecord> records) {
        return Lists.transform(records, TO_EVENT_RECORD);
    }

    public static EventStoreRecord toEventStoreRecord(EventRecordOrBuilder record) {

        final Any event = record.getEvent();
        final EventContext context = record.getContext();
        final TypeName typeName = TypeName.ofEnclosed(event);
        final EventId eventId = context.getEventId();
        final String eventIdStr = idToString(eventId);

        final EventStoreRecord.Builder builder = EventStoreRecord.newBuilder()
                .setTimestamp(getTimestamp(eventId))
                .setEventType(typeName.nameOnly())
                .setAggregateId(context.getAggregateId().toString())
                .setEventId(eventIdStr)
                .setEvent(event)
                .setContext(context);

        return builder.build();
    }

    private static final Function<EventStoreRecord, EventRecord> TO_EVENT_RECORD = new Function<EventStoreRecord, EventRecord>() {
        @Override
        public EventRecord apply(@Nullable EventStoreRecord input) {
            if (input == null) {
                return EventRecord.getDefaultInstance();
            }
            final EventRecord result = toEventRecord(input);
            return result;
        }
    };
    @SuppressWarnings("StringBufferWithoutInitialCapacity")
    public static class EventIdToStringConverter implements Function<EventId, String> {
        @Override
        public String apply(@Nullable EventId eventId) {

            if (eventId == null) {
                return NULL_ID_OR_FIELD;
            }

            final StringBuilder builder = new StringBuilder();

            final CommandId commandId = eventId.getCommandId();

            String userId = NULL_ID_OR_FIELD;

            if (commandId != null && commandId.getActor() != null) {
                userId = commandId.getActor().getValue();
            }

            final String commandTime = (commandId != null) ? timestampToString(commandId.getTimestamp()) : "";

            builder.append(userId)
                    .append(USER_ID_AND_TIME_DELIMITER)
                    .append(commandTime)
                    .append(TIME_DELIMITER)
                    .append(eventId.getDeltaNanos());

            return builder.toString();
        }

    }
}
