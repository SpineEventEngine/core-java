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

package org.spine3.util;

import com.google.common.base.Predicate;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;
import org.spine3.base.EventStorageRecordFilter;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.storage.EventStorageRecord;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Timestamps.isBetween;

/**
 * Utilities for working with event records.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class EventRecords {

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
    public static Message getEvent(EventRecord eventRecord) {
        final Any any = eventRecord.getEvent();
        final Message result = Messages.fromAny(any);
        return result;
    }

    /**
     * The predicate to filter event records after some point in time.
     */
    public static class IsAfter implements Predicate<EventStorageRecord> {

        private final Timestamp timestamp;

        public IsAfter(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable EventStorageRecord record) {
            if (record == null) {
                return false;
            }
            final Timestamp time = record.getTimestamp();
            final boolean result = Timestamps.compare(time, this.timestamp) > 0;
            return result;
        }
    }

    /**
     * The predicate to filter event records before some point in time.
     */
    public static class IsBefore implements Predicate<EventStorageRecord> {

        private final Timestamp timestamp;

        public IsBefore(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable EventStorageRecord record) {
            if (record == null) {
                return false;
            }
            final Timestamp time = record.getTimestamp();
            final boolean result = Timestamps.compare(time, this.timestamp) < 0;
            return result;
        }
    }

    /**
     * The predicate to filter event records within a given time range.
     */
    public static class IsBetween implements Predicate<EventStorageRecord> {

        private final Timestamp start;
        private final Timestamp finish;

        public IsBetween(Timestamp start, Timestamp finish) {
            checkNotNull(start, "after");
            checkNotNull(finish, "before");
            checkArgument(Timestamps.compare(start, finish) < 0, "`start` must be before `finish`");
            this.start = start;
            this.finish = finish;
        }

        @Override
        public boolean apply(@Nullable EventStorageRecord record) {
            if (record == null) {
                return false;
            }
            final Timestamp time = record.getTimestamp();
            final boolean result = isBetween(time, start, finish);
            return result;
        }
    }

    /**
     * The predicate for filtering event records by {@link EventStorageRecordFilter}.
     */
    public static class MatchesFilter implements Predicate<EventStorageRecord> {

        private final EventStorageRecordFilter filter;
        private final TypeName eventType;

        public MatchesFilter(EventStorageRecordFilter filter) {
            this.filter = filter;
            this.eventType = TypeName.of(filter.getEventType());
        }

        @Override
        public boolean apply(@Nullable EventStorageRecord record) {
            if (record == null) {
                return false;
            }

            final Message event = record.getEvent();

            if (!eventType.equals(TypeName.of(event))) {
                return false;
            }

            final EventContext context = record.getContext();

            final Any aggregateId = context.getAggregateId();
            final List<Any> aggregateIdList = filter.getAggregateIdList();
            if (!aggregateIdList.isEmpty()) {
                if (!aggregateIdList.contains(aggregateId)) {
                    return false;
                }
            }

            return true;
        }
    }

    // @formatter:off
    private EventRecords() {}
}
