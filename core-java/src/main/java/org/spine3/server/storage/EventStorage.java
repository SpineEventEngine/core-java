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

package org.spine3.server.storage;

import com.google.protobuf.Any;
import org.spine3.TypeName;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;
import org.spine3.server.Entity;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * A storage used by {@link org.spine3.server.EventStore} for keeping event data.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EventStorage {

    @SuppressWarnings("TypeMayBeWeakened")
    public void store(EventRecord record) {
        final Any event = record.getEvent();
        final EventContext context = record.getContext();
        final TypeName typeName = TypeName.ofEnclosed(event);
        final EventId eventId = context.getEventId();
        final String eventIdStr = Entity.idToString(eventId);
        EventStoreRecord.Builder builder = EventStoreRecord.newBuilder()
                .setTimestamp(eventId.getTimestamp())
                .setEventType(typeName.nameOnly())
                .setAggregateId(context.getAggregateId().toString())
                .setEventId(eventIdStr)
                .setEvent(event)
                .setContext(context);

        write(builder.build());
    }

    /**
     * Returns iterator through all the event records in the storage sorted by timestamp.
     *
     * @return iterator instance
     */
    public abstract Iterator<EventRecord> allEvents();

    /**
     * Reads event record from the storage by event id.
     *
     * @param eventId ID of the event to load
     * @return a record instance or {@code null} if there is no record with such event ID
     */
    @Nullable
    protected abstract EventStoreRecord read(EventId eventId);

    /**
     * Writes record into the storage.
     *
     * @param r the record to write
     */
    protected abstract void write(EventStoreRecord r);

}
