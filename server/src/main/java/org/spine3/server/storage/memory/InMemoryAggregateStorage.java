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

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.aggregate.AggregateStorage;
import org.spine3.server.aggregate.storage.AggregateStatus;
import org.spine3.server.aggregate.storage.AggregateStorageRecord;
import org.spine3.server.aggregate.storage.Predicates;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.util.Exceptions.unsupported;

/**
 * In-memory storage for aggregate events and snapshots.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 *
 * @author Alexander Litus
 */
class InMemoryAggregateStorage<I> extends AggregateStorage<I> {

    private final MultitenantStorage<I, TenantAggregateRecords<I>> multitenantStorage;

    protected InMemoryAggregateStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<I, TenantAggregateRecords<I>>(multitenant) {
            @Override
            TenantAggregateRecords<I> createSlice() {
                return new TenantAggregateRecords<>();
            }
        };
    }

    private TenantAggregateRecords<I> getStorage() {
        return multitenantStorage.getStorage();
    }

    /** Creates a new single-tenant storage instance. */
    protected static <I> InMemoryAggregateStorage<I> newInstance() {
        return new InMemoryAggregateStorage<>(false);
    }

    @Override
    protected void writeRecord(I id, AggregateStorageRecord record) {
        getStorage().put(id, record);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {
        checkNotNull(id);
        final Collection<AggregateStorageRecord> records = getStorage().filtered.get(id);
        return records.iterator();
    }

    @Override
    protected int readEventCountAfterLastSnapshot(I id) {
        checkNotClosed();
        final Integer count = getStorage().eventCounts.get(id);
        if (count == null) {
            return 0;
        }
        return count;
    }

    @Override
    protected void writeEventCountAfterLastSnapshot(I id, int eventCount) {
        checkNotClosed();
        getStorage().eventCounts.put(id, eventCount);
    }

    @Override
    protected boolean markArchived(I id) {
        final AggregateStatus currentStatus = getStorage().statuses.get(id);
        if (currentStatus != null) {
            if (currentStatus.getArchived()) {
                return false; // Already archived.
            }
            final AggregateStatus updatedStatus = currentStatus.toBuilder()
                                                       .setArchived(true)
                                                       .build();
            getStorage().statuses.put(id, updatedStatus);
            return true;
        }

        getStorage().statuses.put(id, AggregateStatus.newBuilder()
                                        .setArchived(true)
                                        .build());
        return true;
    }

    @Override
    protected boolean markDeleted(I id) {
        final AggregateStatus currentStatus = getStorage().statuses.get(id);
        if (currentStatus != null) {
            if (currentStatus.getDeleted()) {
                return false; // Already deleted.
            }
            final AggregateStatus updatedStatus = currentStatus.toBuilder()
                                                               .setDeleted(true)
                                                               .build();
            getStorage().statuses.put(id, updatedStatus);
            return true;
        }

        getStorage().statuses.put(id, AggregateStatus.newBuilder()
                                                     .setDeleted(true)
                                                     .build());
        return true;
    }

    /**
     * The data “slice” for a tenant.
     *
     * @param <I> the type of IDs of aggregates managed by this storage
     */
    private static class TenantAggregateRecords<I> implements TenantStorage<I, AggregateStorageRecord> {

        private final Multimap<I, AggregateStorageRecord> records = TreeMultimap.create(
                new AggregateStorageKeyComparator<I>(), // key comparator
                new AggregateStorageRecordReverseComparator() // value comparator
        );

        private final Map<I, AggregateStatus> statuses = newHashMap();

        private final Predicate<I> isVisible = new Predicate<I>() {
            @Override
            public boolean apply(@Nullable I input) {
                final AggregateStatus aggregateStatus = statuses.get(input);

                return aggregateStatus == null
                        || Predicates.isVisible()
                                     .apply(aggregateStatus);
            }
        };

        private final Multimap<I, AggregateStorageRecord> filtered = Multimaps.filterKeys(records, isVisible);

        private final Map<I, Integer> eventCounts = newHashMap();

        @Nullable
        @Override
        public AggregateStorageRecord get(I id) {
            throw unsupported("Returning single record by aggregate ID is not supported");
        }

        @Override
        public void put(I id, AggregateStorageRecord record) {
            records.put(id, record);
        }

        @Override
        public boolean isEmpty() {
            return filtered.isEmpty();
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
}
