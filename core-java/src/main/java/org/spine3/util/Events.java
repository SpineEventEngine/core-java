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

import com.google.common.base.Predicate;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandId;
import org.spine3.base.CommandResult;
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;
import org.spine3.protobuf.JsonFormat;
import org.spine3.protobuf.Timestamps;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@link EventId} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Events {

    /**
     * Generates new {@link EventId} by the passed {@link CommandId} and current system time.
     * <p>
     * It is assumed tha the passed command ID is of the command that originates the event
     * for which we generate the ID.
     *
     * @param commandId ID of the command, which originated the event
     * @return new event ID
     */
    public static EventId generateId(CommandId commandId) {
        checkNotNull(commandId);

        return EventId.newBuilder()
                .setCommandId(commandId)
                .setTimestamp(Timestamps.now())
                .build();
    }

    private Events() {
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
                Timestamp timestamp = record.getContext().getEventId().getTimestamp();
                return Timestamps.isAfter(timestamp, from);
            }
        };
    }

    public static Predicate<EventRecord> getEventPredicate(final Timestamp from, final Timestamp to) {
        return new Predicate<EventRecord>() {
            @Override
            public boolean apply(@Nullable EventRecord record) {
                checkNotNull(record);
                Timestamp timestamp = record.getContext().getEventId().getTimestamp();
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
                Timestamp timestamp1 = o1.getContext().getEventId().getTimestamp();
                Timestamp timestamp2 = o2.getContext().getEventId().getTimestamp();
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
        final String result = JsonFormat.printToString(id);
        return result;
    }
}
