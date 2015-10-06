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

package org.spine3.server.storage.memory;

import com.google.common.base.Function;
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStoreRecord;
import org.spine3.util.Events;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.util.Events.idToString;

/**
 * In-memory implementation of {@link org.spine3.server.storage.EventStorage}.
 *
 * @author Alexander Litus
 */
class InMemoryEventStorage extends EventStorage {

    private final Map<String, EventStoreRecord> storage = newHashMap();

    @Override
    public Iterator<EventRecord> allEvents() {

        final Collection<EventRecord> result = transform(storage.values(), TO_EVENT_RECORD);
        Events.sort(newArrayList(result));
        final Iterator<EventRecord> iterator = result.iterator();
        return iterator;
    }

    @Nullable
    // TODO:2015.09.24:alexander.litus: this method may be needed later in API
    // @Override
    protected EventStoreRecord read(EventId eventId) {

        final String id = idToString(eventId);
        final EventStoreRecord record = storage.get(id);
        return record;
    }

    @Override
    protected void write(EventStoreRecord record) {
        checkNotNull(record);
        checkNotNull(record.getEventId());
        storage.put(record.getEventId(), record);
    }

    @Override
    protected void releaseResources() {
        // NOP
    }

    protected void clear() {
        storage.clear();
    }

    private static final Function<EventStoreRecord, EventRecord> TO_EVENT_RECORD = new Function<EventStoreRecord, EventRecord>() {
        @SuppressWarnings("NullableProblems") // record cannot be null because it is checked when saving to storage
        @Override
        public EventRecord apply(EventStoreRecord record) {

            EventRecord.Builder builder = EventRecord.newBuilder()
                    .setEvent(record.getEvent())
                    .setContext(record.getContext());

            return builder.build();
        }
    };
}
