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

package org.spine3.server.storage;

import com.google.common.base.Function;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.server.aggregate.Snapshot;
import org.spine3.test.project.ProjectId;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static java.util.Collections.reverse;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.server.Identifiers.newUuid;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;
import static org.spine3.testdata.TestAggregateStorageRecordFactory.createSequentialRecords;
import static org.spine3.testdata.TestAggregateStorageRecordFactory.newAggregateStorageRecord;
import static org.spine3.testdata.TestEventFactory.projectCreated;

@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class AggregateStorageShould {

    private final ProjectId aggregateId = createProjectId(newUuid());

    private AggregateStorage<ProjectId> storage;

    @Before
    public void setUpTest() {
        storage = getStorage();
    }

    protected abstract AggregateStorage<ProjectId> getStorage();

    @Test
    public void return_iterator_over_empty_collection_if_read_history_from_empty_storage() {
        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(aggregateId);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_by_null_id() {
        // noinspection ConstantConditions
        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(null);

        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_event() {
        // noinspection ConstantConditions
        storage.write(null);
    }

    @Test
    public void store_and_read_one_record() {
        final Event expected = projectCreated(aggregateId);

        storage.write(expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(aggregateId);
        assertTrue(iterator.hasNext());
        final AggregateStorageRecord actual = iterator.next();
        assertEquals(expected, actual.getEvent());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_and_read_one_record() {
        final AggregateStorageRecord expected = newAggregateStorageRecord(getCurrentTime(), aggregateId);

        storage.writeInternal(expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(aggregateId);
        assertTrue(iterator.hasNext());
        final AggregateStorageRecord actual = iterator.next();
        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_records_and_return_sorted_by_timestamp_descending() {
        final List<AggregateStorageRecord> records = createSequentialRecords(aggregateId);

        writeAll(records);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(aggregateId);
        final List<AggregateStorageRecord> actual = newArrayList(iterator);
        reverse(records); // expected records should be in a reverse order
        assertEquals(records, actual);
    }

    @Test
    public void store_and_read_snapshot() {
        final Snapshot expected = newSnapshot(getCurrentTime());

        storage.write(aggregateId, expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(aggregateId);
        assertTrue(iterator.hasNext());
        final AggregateStorageRecord actual = iterator.next();
        assertEquals(expected, actual.getSnapshot());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_records_and_load_history_if_no_snapshots() {
        testWriteRecordsAndLoadHistory(getCurrentTime());
    }

    @Test
    public void write_records_and_load_history_till_last_snapshot() {
        final Duration delta = seconds(10);
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, delta);
        final Timestamp time3 = add(time2, delta);

        storage.writeInternal(newAggregateStorageRecord(time1, aggregateId));
        storage.write(aggregateId, newSnapshot(time2));

        testWriteRecordsAndLoadHistory(time3);
    }

    private void testWriteRecordsAndLoadHistory(Timestamp firstRecordTime) {
        final List<AggregateStorageRecord> records = createSequentialRecords(aggregateId, firstRecordTime);

        writeAll(records);

        final AggregateEvents events = storage.read(aggregateId);
        final List<Event> expectedEvents = transform(records, TO_EVENT);
        assertEquals(expectedEvents, events.getEventList());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_write_record_without_event_record_or_snapshot_and_load_it() {
        final AggregateStorageRecord record = AggregateStorageRecord.newBuilder().setAggregateId(aggregateId.getId()).build();

        storage.writeInternal(record);
        storage.read(aggregateId);
    }

    private void writeAll(Iterable<AggregateStorageRecord> records) {
        for (AggregateStorageRecord record : records) {
            storage.writeInternal(record);
        }
    }

    private static final Function<AggregateStorageRecord, Event> TO_EVENT = new Function<AggregateStorageRecord, Event>() {
        @Nullable // return null because an exception won't be propagated in this case
        @Override
        public Event apply(@Nullable AggregateStorageRecord input) {
            return (input == null) ? null : input.getEvent();
        }
    };

    private static Snapshot newSnapshot(Timestamp time) {
        return Snapshot.newBuilder().setTimestamp(time).build();
    }
}
