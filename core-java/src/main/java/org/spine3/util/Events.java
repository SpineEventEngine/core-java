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
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.*;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.storage.EventStoreRecord;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;

/**
 * Utility class for working with {@link EventId} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Events {

    private Events() {
    }

    static {
        Identifiers.IdConverterRegistry.instance().register(EventId.class, new EventIdToStringConverter());
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
    public static Timestamp getTimestamp(EventIdOrBuilder eventId) {
        final Timestamp commandTimestamp = eventId.getCommandId().getTimestamp();
        final Duration delta = TimeUtil.createDurationFromNanos(eventId.getDeltaNanos());
        Timestamp result = TimeUtil.add(commandTimestamp, delta);
        return result;
    }

    public static CommandResult toCommandResult(Iterable<EventRecord> eventRecords, Iterable<Any> errors) {
        return CommandResult.newBuilder()
                .addAllEventRecord(eventRecords)
                .addAllError(errors)
                .build();
    }

    public static Predicate<EventRecord> getEventPredicate(final int sinceVersion) {
        return new Predicate<EventRecord>() {
            @Override
            public boolean apply(@Nullable EventRecord record) {
                checkNotNull(record);
                int version = record.getContext().getVersion();
                return version > sinceVersion;
            }
        };
    }

    public static Predicate<EventRecord> getEventPredicate(final Timestamp from) {
        return new Predicate<EventRecord>() {
            @Override
            public boolean apply(@Nullable EventRecord record) {
                checkNotNull(record);
                Timestamp timestamp = getTimestamp(record.getContext().getEventId());
                return Timestamps.isAfter(timestamp, from);
            }
        };
    }

    public static Predicate<EventRecord> getEventPredicate(final Timestamp from, final Timestamp to) {
        return new Predicate<EventRecord>() {
            @Override
            public boolean apply(@Nullable EventRecord record) {
                checkNotNull(record);
                Timestamp timestamp = getTimestamp(record.getContext().getEventId());
                return Timestamps.isBetween(timestamp, from, to);
            }
        };
    }

    /**
     * Sorts the given event record list by the event timestamp value.
     *
     * @param eventRecords the event record list to sort
     */
    public static void sort(List<EventRecord> eventRecords) {
        Collections.sort(eventRecords, new Comparator<EventRecord>() {
            @Override
            public int compare(EventRecord o1, EventRecord o2) {
                Timestamp timestamp1 = getTimestamp(o1.getContext().getEventId());
                Timestamp timestamp2 = getTimestamp(o2.getContext().getEventId());
                return Timestamps.compare(timestamp1, timestamp2);
            }
        });
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
        EventRecord result = EventRecord.newBuilder()
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
    @SuppressWarnings("TypeMayBeWeakened")
    public static EventRecord toEventRecord(EventStoreRecord storeRecord) {
        final EventRecord.Builder builder = EventRecord.newBuilder()
                .setEvent(storeRecord.getEvent())
                .setContext(storeRecord.getContext());
        return builder.build();
    }

    @SuppressWarnings({"MethodWithMoreThanThreeNegations", "StringBufferWithoutInitialCapacity", "ConstantConditions"})
    public static class EventIdToStringConverter implements Function<EventId, String> {
        @Nullable
        @Override
        public String apply(@Nullable EventId eventId) {

            if (eventId == null) {
                return Identifiers.NULL_ID_OR_FIELD;
            }

            final StringBuilder builder = new StringBuilder();

            final CommandId commandId = eventId.getCommandId();

            String userId = Identifiers.NULL_ID_OR_FIELD;

            if (commandId != null && commandId.getActor() != null) {
                userId = commandId.getActor().getValue();
            }

            final String commandTime = (commandId != null) ? TimeUtil.toString(commandId.getTimestamp()) : "";

            builder.append(userId)
                    .append(Identifiers.USER_ID_AND_TIME_DELIMITER)
                    .append(commandTime)
                    .append(Identifiers.TIME_DELIMITER)
                    .append(eventId.getDeltaNanos());

            return builder.toString();
        }
    }
}
