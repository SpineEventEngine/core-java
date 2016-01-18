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

package org.spine3.server.storage;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;
import org.spine3.server.util.Events;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Utilities for working with storages.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class StorageUtil {

    /**
     * Converts EventStorageRecord to EventRecord.
     */
    public static EventRecord toEventRecord(EventStorageRecord record) {
        final EventRecord.Builder builder = EventRecord.newBuilder()
                .setEvent(record.getEvent())
                .setContext(record.getContext());

        return builder.build();
    }

    /**
     * Converts EventStorageRecords to EventRecords.
     */
    public static List<EventRecord> toEventRecordsList(List<EventStorageRecord> records) {
        return Lists.transform(records, TO_EVENT_RECORD);
    }

    private static final Function<EventStorageRecord, EventRecord> TO_EVENT_RECORD = new Function<EventStorageRecord, EventRecord>() {
        @Override
        public EventRecord apply(@Nullable EventStorageRecord input) {
            if (input == null) {
                return EventRecord.getDefaultInstance();
            }
            final EventRecord result = toEventRecord(input);
            return result;
        }
    };

    /**
     * Creates storage record for the passed {@link EventRecord}.
     */
    public static EventStorageRecord toEventStorageRecord(EventRecord record) {
        final Any event = record.getEvent();
        final EventContext context = record.getContext();
        final TypeName typeName = TypeName.ofEnclosed(event);
        final EventId eventId = context.getEventId();
        final String eventIdStr = Events.idToString(eventId);

        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                .setTimestamp(context.getTimestamp())
                .setEventType(typeName.nameOnly())
                .setAggregateId(context.getAggregateId().toString())
                .setEventId(eventIdStr)
                .setEvent(event)
                .setContext(context);

        return builder.build();
    }

    //@formatter:off
    private StorageUtil() {}
}
