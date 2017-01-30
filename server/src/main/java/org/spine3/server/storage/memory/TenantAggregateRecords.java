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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.aggregate.storage.AggregateStatus;
import org.spine3.server.aggregate.storage.AggregateStorageRecord;
import org.spine3.server.aggregate.storage.Predicates;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.util.Exceptions.unsupported;

/**
 * The events for for a tenant.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 * @author Alexander Yevsyukov
 */
class TenantAggregateRecords<I> implements TenantStorage<I, AggregateStorageRecord> {

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

    /**
     * Always throws {@code UnsupportedOperationException}.
     *
     * <p>Unlike other storages, an aggregate data is a collection
     * of events, not a single record. That's why this method does not
     * conform in full to {@link TenantStorage} interface, and always throws.
     */
    @Nullable
    @Override
    public Optional<AggregateStorageRecord> get(I id) {
        throw unsupported("Returning single record by aggregate ID is not supported");
    }

    /**
     * Obtains aggregate events in the reverse historical order.
     *
     * @return immutable list
     */
    List<AggregateStorageRecord> getHistoryBackward(I id) {
        return ImmutableList.copyOf(filtered.get(id));
    }

    /**
     * Obtains a count of events stored for the aggregate with the passed ID.
     *
     * <p>If no events were stored, the method returns zero.
     *
     * @param id the ID of the aggregate
     * @return the number of events stored for the aggregate or zero
     */
    int getEventCount(I id) {
        final Integer count = eventCounts.get(id);
        if (count == null) {
            return 0;
        }
        return count;
    }

    /**
     * Obtains {@code AggregateStatus} for the passed ID.
     *
     * <p>If no status stored, the default instance is returned.
     */
    AggregateStatus getStatus(I id) {
        final AggregateStatus aggregateStatus = statuses.get(id);
        return aggregateStatus == null
               ? AggregateStatus.getDefaultInstance()
               : aggregateStatus;
    }

    @Override
    public void put(I id, AggregateStorageRecord record) {
        records.put(id, record);
    }

    /**
     * Stores the number of the aggregate events occurred since the last snapshot
     * of the aggregate with the passed ID.
     *
     * @param id the aggregate ID
     * @param eventCount the number of events
     */
    void putEventCount(I id, int eventCount) {
        eventCounts.put(id, eventCount);
    }

    void putStatus(I id, AggregateStatus status) {
        statuses.put(id, status);
    }

    @Override
    public boolean isEmpty() {
        return filtered.isEmpty();
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
