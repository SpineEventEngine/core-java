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

package org.spine3.server.storage.memory;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.protobuf.Timestamp;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStorageRecord;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.filter;

/**
 * In-memory implementation of {@link EventStorage}.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
/*package*/ class InMemoryEventStorage extends EventStorage {

    private static final int INITIAL_CAPACITY = 100;

    @SuppressWarnings("CollectionDeclaredAsConcreteClass") // to stress that the queue is sorted.
    private final PriorityQueue<EventStorageRecord> storage = new PriorityQueue<>(
            INITIAL_CAPACITY,
            new EventStorageRecordComparator());
    private final Map<String, EventStorageRecord> index = Maps.newConcurrentMap();

    /**
     * Compares event records by timestamps of events.
     */
    private static class EventStorageRecordComparator implements Comparator<EventStorageRecord>, Serializable {
        @Override
        public int compare(EventStorageRecord record1, EventStorageRecord record2) {
            final Timestamp time1 = record1.getTimestamp();
            final Timestamp time2 = record2.getTimestamp();
            final int result = Timestamps.compare(time1, time2);
            return result;
        }
        private static final long serialVersionUID = 0L;
    }

    @Override
    public Iterator<Event> iterator(EventStreamQuery query) {
        final Predicate<Event> matchesQuery = new MatchesStreamQuery(query);
        final Iterator<Event> transformed = toEventIterator(storage.iterator());
        final Iterator<Event> result = filter(transformed, matchesQuery);
        return result;
    }

    @Override
    protected void writeInternal(EventStorageRecord record) {
        checkNotNull(record);
        final String eventId = record.getEventId();
        checkState(!eventId.isEmpty(), "eventId cannot be empty");
        storage.add(record);
        index.put(eventId, record);
    }

    @Nullable
    @Override
    protected EventStorageRecord readInternal(EventId eventId) {
        final EventStorageRecord result = index.get(eventId.getUuid());
        return result;
    }

    @Override
    public void close() throws Exception {
        super.close();
    }
}
