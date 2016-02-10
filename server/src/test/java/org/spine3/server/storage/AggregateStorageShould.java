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
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.After;
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
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.testutil.TestAggregateIdFactory.createProjectId;
import static org.spine3.testutil.TestAggregateStorageRecordFactory.createSequentialRecords;
import static org.spine3.testutil.TestAggregateStorageRecordFactory.newAggregateStorageRecord;
import static org.spine3.testutil.TestEventFactory.projectCreated;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public abstract class AggregateStorageShould {

    private final ProjectId id = createProjectId(newUuid());

    private AggregateStorage<ProjectId> storage;

    @Before
    public void setUpTest() {
        storage = getStorage();
    }

    @After
    public void tearDownTest() throws Exception {
        storage.close();
    }

    /**
     * Used to initialize the storage before each test.
     *
     * @return an empty storage instance
     */
    protected abstract AggregateStorage<ProjectId> getStorage();

    @Test
    public void return_iterator_over_empty_collection_if_read_history_from_empty_storage() {
        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);

        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_read_events_by_null_id() {
        // noinspection ConstantConditions
        storage.read(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_read_history_by_null_id() {
        // noinspection ConstantConditions
        storage.historyBackward(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_events() {
        // noinspection ConstantConditions
        storage.write(id, (AggregateEvents) null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_events_by_null_id() {
        // noinspection ConstantConditions
        storage.write(null, AggregateEvents.getDefaultInstance());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_event() {
        // noinspection ConstantConditions
        storage.writeEvent(id, null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_event_by_null_id() {
        // noinspection ConstantConditions
        storage.writeEvent(null, Event.getDefaultInstance());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_snapshot() {
        // noinspection ConstantConditions
        storage.write(id, (Snapshot) null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_snapshot_by_null_id() {
        // noinspection ConstantConditions
        storage.write(null, Snapshot.getDefaultInstance());
    }

    @Test
    public void write_and_read_one_event() {
        final Event expected = projectCreated(id);

        storage.writeEvent(id, expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);
        assertTrue(iterator.hasNext());
        final AggregateStorageRecord actual = iterator.next();
        assertEquals(expected, actual.getEvent());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_and_read_one_record() {
        final AggregateStorageRecord expected = newAggregateStorageRecord(getCurrentTime());

        storage.writeInternal(id, expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);
        assertTrue(iterator.hasNext());
        final AggregateStorageRecord actual = iterator.next();
        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_records_and_return_sorted_by_timestamp_descending() {
        final List<AggregateStorageRecord> records = createSequentialRecords(id);

        writeAll(id, records);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);
        final List<AggregateStorageRecord> actual = newArrayList(iterator);
        reverse(records); // expected records should be in a reverse order
        assertEquals(records, actual);
    }

    @Test
    public void write_and_read_aggregate_events() {
        final List<AggregateStorageRecord> records = createSequentialRecords(id);
        final List<Event> expectedEvents = transform(records, TO_EVENT);
        final AggregateEvents aggregateEvents = AggregateEvents.newBuilder().addAllEvent(expectedEvents).build();

        storage.write(id, aggregateEvents);

        final AggregateEvents events = storage.read(id);

        final List<Event> actualEvents = events.getEventList();
        assertEquals(expectedEvents, actualEvents);
    }

    @Test
    public void store_and_read_snapshot() {
        final Snapshot expected = newSnapshot(getCurrentTime());

        storage.write(id, expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);
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

        storage.writeInternal(id, newAggregateStorageRecord(time1));
        storage.write(id, newSnapshot(time2));

        testWriteRecordsAndLoadHistory(time3);
    }

    private void testWriteRecordsAndLoadHistory(Timestamp firstRecordTime) {
        final List<AggregateStorageRecord> records = createSequentialRecords(id, firstRecordTime);

        writeAll(id, records);

        final AggregateEvents events = storage.read(id);
        final List<Event> expectedEvents = transform(records, TO_EVENT);
        final List<Event> actualEvents = events.getEventList();
        assertEquals(expectedEvents, actualEvents);
    }

    private void writeAll(ProjectId id, Iterable<AggregateStorageRecord> records) {
        for (AggregateStorageRecord record : records) {
            storage.writeInternal(id, record);
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
        return Snapshot.newBuilder()
                .setState(Any.getDefaultInstance())
                .setTimestamp(time)
                .build();
    }
}
