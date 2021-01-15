/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.aggregate;

import com.google.protobuf.Any;
import io.spine.query.RecordQuery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static io.spine.server.aggregate.HistoryBackwardOperation.inChronologicalOrder;

/**
 * Performs the truncation of the aggregate history.
 */
final class TruncateOperation {

    private final AggregateEventStorage eventStorage;

    /**
     * Creates an operation for the given storage of the historical event records for the aggregate
     * of a particular type.
     *
     * <p>Invoking the constructor does not start the truncation. Please use
     * {@link #performWith(int, Predicate) performWith(shapshotIndex, predicate)} to run
     * the operation.
     */
    TruncateOperation(AggregateEventStorage storage) {
        eventStorage = storage;
    }

    /**
     * Runs the history truncation.
     *
     * <p>The history records are truncated starting from the most recent one and deep into the
     * history. The operation is performed until all the following conditions are true:
     *
     * <ul>
     *     <li>the number of aggregate snapshots among the deleted history records is less than
     *     a given number passed as {@code snapshotIndex};
     *
     *     <li>the passed predicate is {@code true};
     *
     *     <li>the bottom of the aggregate history is not reached
     * </ul>
     *
     * @param snapshotIndex
     *         a zero-based snapshot index, until which the history should be truncated, exclusive
     * @param predicate
     *         a condition telling whether the truncation should be stopped, judging on
     *         the currently examined history record
     */
    void performWith(int snapshotIndex, Predicate<AggregateEventRecord> predicate) {
        Iterator<AggregateEventRecord> eventRecords = eventStorage.readAll(chronologically());
        Map<Any, Integer> snapshotHitsByAggregateId = new HashMap<>();
        Set<AggregateEventRecordId> toDelete = new HashSet<>();
        while (eventRecords.hasNext()) {
            AggregateEventRecord eventRecord = eventRecords.next();
            Any packedId = eventRecord.getAggregateId();
            int snapshotsHit = snapshotHitsByAggregateId.get(packedId) != null
                               ? snapshotHitsByAggregateId.get(packedId)
                               : 0;
            if (snapshotsHit > snapshotIndex && predicate.test(eventRecord)) {
                toDelete.add(eventRecord.getId());
            }
            if (eventRecord.hasSnapshot()) {
                snapshotHitsByAggregateId.put(packedId, snapshotsHit + 1);
            }
        }
        eventStorage.deleteAll(toDelete);
    }

    private RecordQuery<AggregateEventRecordId, AggregateEventRecord> chronologically() {
        RecordQuery<AggregateEventRecordId, AggregateEventRecord> orderChronologically =
                inChronologicalOrder(eventStorage.queryBuilder(), null).build();
        return orderChronologically;
    }
}
