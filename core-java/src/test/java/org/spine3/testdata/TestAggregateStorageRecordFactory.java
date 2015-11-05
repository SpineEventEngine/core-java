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

package org.spine3.testdata;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;
import org.spine3.server.storage.AggregateStorageRecord;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.ProjectIdOrBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.protobuf.Durations.seconds;


@SuppressWarnings("UtilityClass")
public class TestAggregateStorageRecordFactory {

    private TestAggregateStorageRecordFactory() {}

    public static AggregateStorageRecord newAggregateStorageRecord(Timestamp timestamp, ProjectIdOrBuilder aggregateId) {
        final AggregateStorageRecord.Builder builder = AggregateStorageRecord.newBuilder()
                .setAggregateId(aggregateId.getId())
                .setTimestamp(timestamp);
        return builder.build();
    }

    public static AggregateStorageRecord newAggregateStorageRecord(Timestamp timestamp, ProjectIdOrBuilder aggregateId, EventRecord event) {
        final AggregateStorageRecord.Builder builder = newAggregateStorageRecord(timestamp, aggregateId)
                .toBuilder()
                .setEventRecord(event);
        return builder.build();
    }

    /*
     * Returns several records sorted by timestamp ascending.
     * First record's timestamp is current time.
     */
    public static List<AggregateStorageRecord> createSequentialRecords(ProjectId id) {
        return createSequentialRecords(id, getCurrentTime());
    }

    /**
     * Returns several records sorted by timestamp ascending.
     * @param timestamp1 the timestamp of first record.
     */
    public static List<AggregateStorageRecord> createSequentialRecords(ProjectId id, Timestamp timestamp1) {

        final Duration delta = seconds(10);

        final Timestamp timestamp2 = add(timestamp1, delta);
        final Timestamp timestamp3 = add(timestamp2, delta);

        final AggregateStorageRecord record1 = newAggregateStorageRecord(timestamp1, id, TestEventRecordFactory.projectCreated(id));
        final AggregateStorageRecord record2 = newAggregateStorageRecord(timestamp2, id, TestEventRecordFactory.taskAdded(id));
        final AggregateStorageRecord record3 = newAggregateStorageRecord(timestamp3, id, TestEventRecordFactory.projectStarted(id));

        return newArrayList(record1, record2, record3);
    }
}
