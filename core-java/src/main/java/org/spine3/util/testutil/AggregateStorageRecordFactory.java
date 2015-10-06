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

package org.spine3.util.testutil;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.server.storage.AggregateStorageRecord;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;

@SuppressWarnings("UtilityClass")
public class AggregateStorageRecordFactory {

    private AggregateStorageRecordFactory() {}

    public static AggregateStorageRecord newAggregateStorageRecord(Timestamp timestamp, String aggregateId) {
        final AggregateStorageRecord.Builder builder = AggregateStorageRecord.newBuilder()
                .setAggregateId(aggregateId)
                .setTimestamp(timestamp);
        return builder.build();
    }

    /*
     * Returns several records sorted by timestamp ascending
     */
    public static List<AggregateStorageRecord> getSequentialRecords(String aggregateId) {

        final int secondsDelta = 10;
        final Duration delta = Duration.newBuilder().setSeconds(secondsDelta).build();

        final Timestamp timestampFirst = getCurrentTime();
        final Timestamp timestampSecond = TimeUtil.add(timestampFirst, delta);
        final Timestamp timestampLast = TimeUtil.add(timestampSecond, delta);

        final AggregateStorageRecord recordFirst = newAggregateStorageRecord(timestampFirst, aggregateId);
        final AggregateStorageRecord recordSecond = newAggregateStorageRecord(timestampSecond, aggregateId);
        final AggregateStorageRecord recordLast = newAggregateStorageRecord(timestampLast, aggregateId);

        return newArrayList(recordFirst, recordSecond, recordLast);
    }
}
