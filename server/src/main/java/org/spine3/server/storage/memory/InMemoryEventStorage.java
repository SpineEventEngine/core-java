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
import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStorageRecord;
import org.spine3.server.stream.EventStreamQuery;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.filter;
import static org.spine3.server.storage.StorageUtil.toEventRecordsIterator;

/**
 * In-memory implementation of {@link EventStorage}.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
class InMemoryEventStorage extends EventStorage {

    private static final int INITIAL_CAPACITY = 100;

    @SuppressWarnings("CollectionDeclaredAsConcreteClass") // to stress that the queue is sorted.
    private final PriorityQueue<EventStorageRecord> storage = new PriorityQueue<>(
            INITIAL_CAPACITY,
            new EventRecordComparator());

    /**
     * Compares event records by timestamp of events.
     */
    private static class EventRecordComparator implements Comparator<EventStorageRecord>, Serializable {
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
    public Iterator<EventRecord> iterator(EventStreamQuery query) {
        final Predicate<EventRecord> matchesQuery = new MatchesStreamQuery(query);
        final Iterator<EventRecord> transformed = toEventRecordsIterator(storage.iterator());
        final Iterator<EventRecord> result = filter(transformed, matchesQuery);
        return result;
    }

    @Override
    protected void write(EventStorageRecord record) {
        checkNotNull(record);
        checkNotNull(record.getEventId());
        storage.add(record);
    }

    @Override
    public void close() throws Exception {
        clear();
        super.close();
    }

    /**
     * Clears all data in the storage.
     */
    protected void clear() {
        storage.clear();
    }
}
