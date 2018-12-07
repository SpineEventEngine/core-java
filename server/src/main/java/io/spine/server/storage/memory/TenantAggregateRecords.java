/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.protobuf.util.Timestamps;
import io.spine.core.Event;
import io.spine.server.aggregate.AggregateEventRecord;
import io.spine.server.aggregate.AggregateReadRequest;
import io.spine.server.entity.LifecycleFlags;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static io.spine.util.Exceptions.unsupported;
import static io.spine.validate.Validate.isDefault;

/**
 * The events for for a tenant.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 * @author Alexander Yevsyukov
 */
class TenantAggregateRecords<I> implements TenantStorage<I, AggregateEventRecord> {

    private final Multimap<I, AggregateEventRecord> records = TreeMultimap.create(
            new AggregateStorageKeyComparator<>(), // key comparator
            new AggregateStorageRecordReverseComparator() // value comparator
    );

    private final Map<I, LifecycleFlags> statuses = newHashMap();
    private final Map<I, Integer> eventCounts = newHashMap();

    @Override
    public Iterator<I> index() {
        Iterator<I> result = records.keySet()
                                    .iterator();
        return result;
    }

    /**
     * Always throws {@code UnsupportedOperationException}.
     *
     * <p>Unlike other storages, an aggregate data is a collection
     * of events, not a single record. That's why this method does not
     * conform in full to {@link TenantStorage} interface, and always throws.
     */
    @Override
    public @Nullable Optional<AggregateEventRecord> get(I id) {
        throw unsupported("Returning single record by aggregate ID is not supported");
    }

    /**
     * Obtains aggregate events in the reverse historical order.
     *
     * @return immutable list
     */
    List<AggregateEventRecord> getHistoryBackward(AggregateReadRequest<I> request) {
        I id = request.getRecordId();
        return ImmutableList.copyOf(records.get(id));
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
        Integer count = eventCounts.get(id);
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
    Optional<LifecycleFlags> getStatus(I id) {
        LifecycleFlags entityStatus = statuses.get(id);
        return Optional.ofNullable(entityStatus);
    }

    @Override
    public void put(I id, AggregateEventRecord record) {
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

    void putStatus(I id, LifecycleFlags status) {
        statuses.put(id, status);
    }

    @Override
    public boolean isEmpty() {
        return records.isEmpty();
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
            int firstHashCode = first.hashCode();
            int secondHashCode = second.hashCode();

            result = Integer.compare(firstHashCode, secondHashCode);
            return result;
        }
    }

    /** Used for sorting by version descending (from newer to older). */
    private static class AggregateStorageRecordReverseComparator
            implements Comparator<AggregateEventRecord>, Serializable {
        private static final long serialVersionUID = 0L;

        @Override
        public int compare(AggregateEventRecord first, AggregateEventRecord second) {
            int result = compareVersions(first, second);

            if (result == 0) {
                result = compareTimestamps(first, second);

                // In case the wall-clock isn't accurate enough, the timestamps may be the same.
                // In this case, compare the record type in a similar fashion.
                if(result == 0) {
                    result = compareSimilarRecords(first, second);
                }
            }
            return result;
        }

        /**
         * Compares the {@linkplain AggregateEventRecord records} with the same version number
         * and timestamp.
         *
         * <p>If the timestamp and versions are the same, we should check if one of the records
         * is a snapshot. From a snapshot and an event with the same version and timestamp,
         * a snapshot is considered "newer".
         *
         * @param first  the first record
         * @param second the second record
         * @return {@code -1}, {@code 1} or {@code 0} according to
         * {@linkplain Comparator#compare(Object, Object) compare(..) specification}
         */
        private static int compareSimilarRecords(AggregateEventRecord first,
                                                 AggregateEventRecord second) {
            boolean firstIsSnapshot = isSnapshot(first);
            boolean secondIsSnapshot = isSnapshot(second);
            if (firstIsSnapshot && !secondIsSnapshot) {
                return -1;
            } else if (secondIsSnapshot && !firstIsSnapshot) {
                return 1;
            } else {
                // Both are of the same kind and have the same versions and timestamps.
                return 0;
            }
        }

        private static boolean isSnapshot(AggregateEventRecord record) {
            return !isDefault(record.getSnapshot());
        }

        private static int compareVersions(AggregateEventRecord first,
                                           AggregateEventRecord second) {
            int result;
            int secondEventVersion = versionNumberOf(second);
            int firstEventVersion = versionNumberOf(first);
            result = Integer.compare(secondEventVersion, firstEventVersion);
            return result;
        }

        private static int compareTimestamps(AggregateEventRecord first,
                                             AggregateEventRecord second) {
            return Timestamps.compare(second.getTimestamp(), first.getTimestamp());
        }

        private static int versionNumberOf(AggregateEventRecord record) {
            int versionNumber;

            Event event = record.getEvent();
            if (isDefault(event)) {
                versionNumber = record.getSnapshot()
                                      .getVersion()
                                      .getNumber();
            } else {
                versionNumber = event.getContext()
                                     .getVersion()
                                     .getNumber();
            }
            return versionNumber;
        }
    }
}
