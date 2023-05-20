/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.core.Event;
import io.spine.server.aggregate.AggregateEventRecord;
import io.spine.server.aggregate.AggregateReadRequest;
import io.spine.server.entity.LifecycleFlags;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.collect.Multimaps.synchronizedSortedSetMultimap;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.util.Exceptions.unsupported;

/**
 * The events for for a tenant.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 */
final class TenantAggregateRecords<I> implements TenantStorage<I, AggregateEventRecord> {

    private final Multimap<I, AggregateEventRecord> records = synchronizedSortedSetMultimap(
            TreeMultimap.create(
                    new AggregateStorageKeyComparator<>(), // key comparator
                    new AggregateStorageRecordReverseComparator() // value comparator
            ));

    private final Map<I, LifecycleFlags> statuses = new HashMap<>();

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
    List<AggregateEventRecord> historyBackward(AggregateReadRequest<I> request) {
        I id = request.recordId();
        return ImmutableList.copyOf(records.get(id));
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
    public synchronized void put(I id, AggregateEventRecord record) {
        records.put(id, record);
    }

    synchronized void putStatus(I id, LifecycleFlags status) {
        statuses.put(id, status);
    }

    @Override
    public boolean isEmpty() {
        return records.isEmpty();
    }

    /**
     * Drops all records that are older than the Nth snapshot for each entity.
     *
     * @see io.spine.server.aggregate.AggregateStorage#truncateOlderThan(int)
     */
    void truncateOlderThan(int snapshotIndex) {
        truncate(snapshotIndex, record -> true);
    }

    /**
     * Drops all records older than {@code date} but not newer than the Nth snapshot for each
     * entity.
     *
     * @see io.spine.server.aggregate.AggregateStorage#truncateOlderThan(int, Timestamp)
     */
    void truncateOlderThan(int snapshotIndex, Timestamp date) {
        Predicate<AggregateEventRecord> isOlder =
                record -> Timestamps.compare(date, record.getTimestamp()) > 0;
        truncate(snapshotIndex, isOlder);
    }

    /**
     * Drops the records that are preceding the specified snapshot and match the specified
     * {@code Predicate}.
     */
    private synchronized void
    truncate(int snapshotIndex, Predicate<AggregateEventRecord> predicate) {
        ImmutableSet.copyOf(records.keySet())
                    .forEach(id -> truncate(id, snapshotIndex, predicate));
    }

    private void
    truncate(I id, int snapshotIndex, Predicate<AggregateEventRecord> predicate) {
        ImmutableList<AggregateEventRecord> recordsCopy = ImmutableList.copyOf(records.get(id));
        int snapshotsHit = 0;
        for (AggregateEventRecord record : recordsCopy) {
            if (snapshotsHit > snapshotIndex && predicate.test(record)) {
                this.records.remove(id, record);
            }
            if (record.hasSnapshot()) {
                snapshotsHit++;
            }
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
                if (result == 0) {
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
         * @param first
         *         the first record
         * @param second
         *         the second record
         * @return {@code -1}, {@code 1} or {@code 0} according to
         *         {@linkplain Comparator#compare(Object, Object) compare(..) specification}
         */
        private static int compareSimilarRecords(AggregateEventRecord first,
                                                 AggregateEventRecord second) {
            boolean firstIsSnapshot = first.hasSnapshot();
            boolean secondIsSnapshot = second.hasSnapshot();
            if (firstIsSnapshot && !secondIsSnapshot) {
                return -1;
            } else if (secondIsSnapshot && !firstIsSnapshot) {
                return 1;
            } else if (!first.equals(second)) {
                // Both are of the same kind and have the same versions and timestamps.
                // We cannot allow 2 nonidentical records to be equal in terms of `compare(..)`,
                // so compare by hash codes.
                return Integer.compare(first.hashCode(), second.hashCode());
            } else {
                // Two records are equal in terms of both `equals(..)` and `compare(..)`.
                return 0;
            }
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
            Event event = record.getEvent();
            if (isDefault(event)) {
                return record.getSnapshot()
                             .getVersion()
                             .getNumber();
            }
            return event.context()
                        .getVersion()
                        .getNumber();
        }
    }
}
