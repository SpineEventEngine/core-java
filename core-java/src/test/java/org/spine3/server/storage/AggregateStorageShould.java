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

package org.spine3.server.storage;

import com.google.common.base.Function;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.EventRecord;
import org.spine3.server.aggregate.Snapshot;
import org.spine3.test.project.ProjectId;
import org.spine3.util.testutil.AggregateIdFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.server.storage.EventStorageShould.assertEventRecordListsAreEqual;
import static org.spine3.util.testutil.AggregateStorageRecordFactory.getSequentialRecords;
import static org.spine3.util.testutil.AggregateStorageRecordFactory.newAggregateStorageRecord;
import static org.spine3.util.testutil.EventRecordFactory.projectCreated;

@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection", "ConstantConditions",
"ConstructorNotProtectedInAbstractClass", "AbstractClassWithoutAbstractMethods", "MagicNumber", "NoopMethodInAbstractClass"})
public abstract class AggregateStorageShould {

    private static final ProjectId ID = AggregateIdFactory.newProjectId();

    private final AggregateStorage<ProjectId> storage;

    public AggregateStorageShould(AggregateStorage<ProjectId> storage) {
        this.storage = storage;
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_history_from_empty_storage() {

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(ID);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_by_null_id() {

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(null);
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_record() {
        storage.write(null);
    }

    @Test
    public void store_and_read_one_record() {

        final EventRecord expected = projectCreated();
        storage.store(expected);
        waitIfNeeded(5);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(ID);

        assertTrue(iterator.hasNext());

        final AggregateStorageRecord actual = iterator.next();

        assertEquals(expected, actual.getEventRecord());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_and_read_one_record() {

        final AggregateStorageRecord expected = newAggregateStorageRecord(getCurrentTime(), ID);
        storage.write(expected);
        waitIfNeeded(5);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(ID);

        assertTrue(iterator.hasNext());

        final AggregateStorageRecord actual = iterator.next();

        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_records_and_return_sorted_by_timestamp_descending() {

        final List<AggregateStorageRecord> records = getSequentialRecords(ID);

        for (AggregateStorageRecord record : records) {
            storage.write(record);
        }

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(ID);
        final List<AggregateStorageRecord> actual = newArrayList(iterator);

        Collections.reverse(records); // expected records should be in reverse order

        assertEquals(records, actual);
    }

    @Test
    public void store_and_read_snapshot() {

        final Snapshot expected = newSnapshot(getCurrentTime());
        storage.store(ID, expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(ID);

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

        storage.write(newAggregateStorageRecord(time1, ID));

        storage.store(ID, newSnapshot(time2));

        testWriteRecordsAndLoadHistory(time3);
    }

    private void testWriteRecordsAndLoadHistory(Timestamp firstRecordTime) {

        final List<AggregateStorageRecord> records = getSequentialRecords(ID, firstRecordTime);

        for (AggregateStorageRecord record : records) {
            storage.write(record);
        }

        final AggregateEvents events = storage.load(ID);

        final List<EventRecord> expectedEventRecords = transform(records, TO_EVENT_RECORD);

        assertEventRecordListsAreEqual(expectedEventRecords, events.getEventRecordList());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_write_record_without_event_record_or_snapshot_and_load_it() {

        final AggregateStorageRecord record = AggregateStorageRecord.newBuilder().setAggregateId(ID.getId()).build();

        storage.write(record);

        storage.load(ID);
    }


    private static final Function<AggregateStorageRecord, EventRecord> TO_EVENT_RECORD = new Function<AggregateStorageRecord, EventRecord>() {
        @Override
        public EventRecord apply(@Nullable AggregateStorageRecord input) {
            return input.getEventRecord();
        }
    };

    private static Snapshot newSnapshot(Timestamp time) {
        return Snapshot.newBuilder().setTimestamp(time).build();
    }

    protected void waitIfNeeded(long seconds) {
        // NOP
    }
}
