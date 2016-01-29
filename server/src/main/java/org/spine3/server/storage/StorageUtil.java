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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import org.spine3.SPI;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.server.util.Events;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities for working with storages.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public class StorageUtil {

    /**
     * Converts EventStorageRecord to Event.
     */
    public static Event toEvent(EventStorageRecord record) {
        final Event.Builder builder = Event.newBuilder()
                .setMessage(record.getMessage())
                .setContext(record.getContext());
        return builder.build();
    }

    /**
     * Converts EventStorageRecords to Events.
     */
    public static List<Event> toEventList(List<EventStorageRecord> records) {
        return Lists.transform(records, TO_EVENT);
    }

    /**
     * Converts EventStorageRecords to Events.
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static List<Event> toEventList(EventStorageRecord... records) {
        return Lists.transform(ImmutableList.copyOf(records), TO_EVENT);
    }

    /**
     * Converts {@code EventStorageRecord}s to {@code Event}s.
     */
    public static Iterator<Event> toEventIterator(Iterator<EventStorageRecord> records) {
        return Iterators.transform(records, TO_EVENT);
    }

    private static final Function<EventStorageRecord, Event> TO_EVENT = new Function<EventStorageRecord, Event>() {
        @Override
        public Event apply(@Nullable EventStorageRecord input) {
            if (input == null) {
                return Event.getDefaultInstance();
            }
            final Event result = toEvent(input);
            return result;
        }
    };

    /**
     * Creates storage record for the passed {@link Event}.
     */
    public static EventStorageRecord toEventStorageRecord(Event event) {
        final Any message = event.getMessage();
        final EventContext context = event.getContext();
        final TypeName typeName = TypeName.ofEnclosed(message);
        final EventId eventId = context.getEventId();
        final String eventIdStr = Events.idToString(eventId);

        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                .setTimestamp(context.getTimestamp())
                .setEventType(typeName.nameOnly())
                .setAggregateId(context.getAggregateId().toString())
                .setEventId(eventIdStr)
                .setMessage(message)
                .setContext(context);

        return builder.build();
    }

    private StorageUtil() {}
}
