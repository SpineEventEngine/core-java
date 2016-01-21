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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.AggregateStorageRecord;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.util.Identifiers.idToString;

/**
 * In-memory storage for aggregate root events and snapshots.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("ComparatorNotSerializable")
class InMemoryAggregateStorage<I> extends AggregateStorage<I> {

    private final Multimap<String, AggregateStorageRecord> storage = TreeMultimap.create(
            new StringComparator(),
            new AggregateStorageRecordReverseComparator()
    );

    /**
     * Creates new storage instance.
     */
    protected static <I> InMemoryAggregateStorage<I> newInstance() {
        return new InMemoryAggregateStorage<>();
    }

    @Override
    protected void writeInternal(AggregateStorageRecord record) {
        checkNotNull(record);
        checkNotNull(record.getAggregateId());
        storage.put(record.getAggregateId(), record);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {

        final String idString = idToString(id);
        final Collection<AggregateStorageRecord> records = storage.get(idString);
        return records.iterator();
    }

    /**
     * Clears all data in the storage.
     */
    protected void clear() {
        storage.clear();
    }


    /*
     * Used for sorting by timestamp descending (from newer to older)
     */
    private static class AggregateStorageRecordReverseComparator implements Comparator<AggregateStorageRecord> {
        @Override
        public int compare(AggregateStorageRecord first, AggregateStorageRecord second) {
            return Timestamps.compare(second.getTimestamp(), first.getTimestamp());
        }
    }

    private static class StringComparator implements Comparator<String> {
        @Override
        public int compare(String first, String second) {
            return first.compareTo(second);
        }
    }
}
