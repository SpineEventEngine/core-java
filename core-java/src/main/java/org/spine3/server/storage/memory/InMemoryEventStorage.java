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

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;
import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStoreRecord;
import org.spine3.util.Events;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory implementation of {@link EventStorage}.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
class InMemoryEventStorage extends EventStorage {

    @SuppressWarnings("CollectionDeclaredAsConcreteClass") // to stress that the queue is sorted.
    private final PriorityQueue<EventRecord> storage = new PriorityQueue<>(100, new EventRecordComparator());

    /**
     * Compares event records by timestamp of events.
     */
    private static class EventRecordComparator implements Comparator<EventRecord>, Serializable {
        @Override
        public int compare(EventRecord o1, EventRecord o2) {
            final Timestamp timestamp = Events.getTimestamp(o1);
            final Timestamp anotherTimestamp = Events.getTimestamp(o2);
            final int result = Timestamps.compare(timestamp, anotherTimestamp);
            return result;
        }

        private static final long serialVersionUID = 0L;
    }

    @Override
    public Iterator<EventRecord> allEvents() {
        final Iterator<EventRecord> result = Iterators.unmodifiableIterator(storage.iterator());
        return result;
    }

    @Override
    public Iterator<EventRecord> since(Timestamp timestamp) {
        final Predicate<EventRecord> isAfter = new Events.IsAfter(timestamp);

        final Iterator<EventRecord> result = FluentIterable.from(storage)
                .filter(isAfter)
                .iterator();

        return result;
    }

    @Override
    protected void write(EventStoreRecord record) {
        checkNotNull(record);
        checkNotNull(record.getEventId());

        final EventRecord eventRec = Events.toEventRecord(record);
        storage.add(eventRec);
    }

    @Override
    public void close() {
        clear();
    }

    /**
     * Clears all data in the storage.
     */
    protected void clear() {
        storage.clear();
    }

}
