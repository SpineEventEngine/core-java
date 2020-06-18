/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import io.spine.annotation.SPI;
import io.spine.client.ResponseFormat;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Performs the truncation of the Aggregate history.
 *
 * @param <I>
 *         the type of the aggregate identifiers
 */
@SPI
public class Truncate<I> {

    private final AggregateEventStorage eventStorage;

    protected Truncate(AggregateEventStorage storage) {
        eventStorage = storage;
    }

    protected void performWith(int snapshotIndex, Predicate<AggregateEventRecord> predicate) {
        ResponseFormat orderChronologically = HistoryBackward.chronologicalResponseWith(null);
        Iterator<AggregateEventRecord> eventRecords = eventStorage.readAll(orderChronologically);
        Map<Any, Integer> snapshotHitsByAggregateId = newHashMap();
        while (eventRecords.hasNext()) {
            AggregateEventRecord eventRecord = eventRecords.next();
            Any packedId = eventRecord.getAggregateId();
            int snapshotsHit = snapshotHitsByAggregateId.get(packedId) != null
                               ? snapshotHitsByAggregateId.get(packedId)
                               : 0;
            if (snapshotsHit > snapshotIndex && predicate.test(eventRecord)) {
                eventStorage.delete(eventRecord.getId());
            }
            if (eventRecord.hasSnapshot()) {
                snapshotHitsByAggregateId.put(packedId, snapshotsHit + 1);
            }
        }
    }
}