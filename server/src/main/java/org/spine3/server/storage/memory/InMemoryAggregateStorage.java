/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.aggregate.AggregateStorage;
import org.spine3.server.aggregate.storage.AggregateStorageRecord;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

/**
 * In-memory storage for aggregate events and snapshots.
 *
 * @author Alexander Litus
 */
class InMemoryAggregateStorage<I> extends AggregateStorage<I> {

    private final Multimap<I, AggregateStorageRecord> recordMap = TreeMultimap.create(
            new AggregateStorageKeyComparator<I>(), // key comparator
            new AggregateStorageRecordReverseComparator() // value comparator
    );

    private final Map<I, Integer> eventCountMap = newHashMap();

    protected InMemoryAggregateStorage(boolean multitenant) {
        super(multitenant);
    }

    /** Creates a new single-tenant storage instance. */
    protected static <I> InMemoryAggregateStorage<I> newInstance() {
        return new InMemoryAggregateStorage<>(false);
    }

    @Override
    protected void writeRecord(I id, AggregateStorageRecord record) {
        recordMap.put(id, record);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {
        checkNotNull(id);
        final Collection<AggregateStorageRecord> records = recordMap.get(id);
        return records.iterator();
    }

    @Override
    protected int readEventCountAfterLastSnapshot(I id) {
        checkNotClosed();
        final Integer count = eventCountMap.get(id);
        if (count == null) {
            return 0;
        }
        return count;
    }

    @Override
    protected void writeEventCountAfterLastSnapshot(I id, int eventCount) {
        checkNotClosed();
        eventCountMap.put(id, eventCount);
    }

    /** Used for sorting by timestamp descending (from newer to older). */
    private static class AggregateStorageRecordReverseComparator implements Comparator<AggregateStorageRecord>,
                                                                            Serializable {
        private static final long serialVersionUID = 0L;

        @Override
        public int compare(AggregateStorageRecord first, AggregateStorageRecord second) {
            final int result = Timestamps.compare(second.getTimestamp(), first.getTimestamp());
            return result;
        }
    }

    /** Used for sorting keys by the key hash codes. */
    private static class AggregateStorageKeyComparator<K> implements Comparator<K>, Serializable {

        private static final long serialVersionUID = 0L;

        @Override
        public int compare(K first, K second) {
            int result = 0;
            if (first.equals(second)) {
                return result;
            }

            // To define an order:
            final int firstHashCode = first.hashCode();
            final int secondHashCode = second.hashCode();

            result = Integer.compare(firstHashCode, secondHashCode);
            return result;
        }
    }
}
