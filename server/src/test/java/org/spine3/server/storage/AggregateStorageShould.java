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
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.Snapshot;
import org.spine3.test.Tests;
import org.spine3.test.storage.Project;
import org.spine3.test.storage.ProjectId;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.protobuf.util.Timestamps.add;
import static java.util.Collections.reverse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Timestamps.getCurrentTime;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public abstract class AggregateStorageShould extends AbstractStorageShould<ProjectId, AggregateEvents> {

    private final ProjectId id = Given.AggregateId.newProjectId();

    private AggregateStorage<ProjectId> storage;

    @Before
    public void setUpAggregateStorageTest() {
        storage = getStorage();
    }

    @After
    public void tearDownAggregateStorageTest() {
        close(storage);
    }

    @Override
    protected abstract AggregateStorage<ProjectId> getStorage();

    /**
     * Used to get a storage in tests with different ID types.
     *
     * <p>NOTE: the storage is closed after each test.
     *
     * @param <Id> the type of aggregate IDs
     * @return an empty storage instance
     */
    protected abstract <Id> AggregateStorage<Id> getStorage(
            Class<? extends Aggregate<Id, ? extends Message, ? extends Message.Builder>> aggregateClass);

    @Override
    protected AggregateEvents newStorageRecord() {
        final List<AggregateStorageRecord> records = Given.AggregateStorageRecord.createSequentialRecords(id);
        final List<Event> expectedEvents = transform(records, TO_EVENT);
        final AggregateEvents aggregateEvents = AggregateEvents.newBuilder().addAllEvent(expectedEvents).build();
        return aggregateEvents;
    }

    @Override
    protected ProjectId newId() {
        return Given.AggregateId.newProjectId();
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_history_from_empty_storage() {
        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);

        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_read_history_by_null_id() {
        storage.historyBackward(Tests.<ProjectId>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_event() {
        storage.writeEvent(id, Tests.<Event>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_event_by_null_id() {
        storage.writeEvent(Tests.<ProjectId>nullRef(), Event.getDefaultInstance());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_snapshot() {
        storage.write(id, Tests.<AggregateEvents>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_snapshot_by_null_id() {
        storage.write(Tests.<ProjectId>nullRef(), Snapshot.getDefaultInstance());
    }

    @Test
    public void write_read_and_one_event_by_Message_id() {
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_event_by_String_id() {
        final AggregateStorage<String> storage = getStorage(TestAggregateWithIdString.class);
        final String id = newUuid();
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_event_by_Long_id() {
        final AggregateStorage<Long> storage = getStorage(TestAggregateWithIdLong.class);
        final long id = 10L;
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_event_by_Integer_id() {
        final AggregateStorage<Integer> storage = getStorage(TestAggregateWithIdInteger.class);
        final int id = 10;
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_one_record() {
        final AggregateStorageRecord expected = Given.AggregateStorageRecord.create(getCurrentTime());

        storage.writeRecord(id, expected);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);
        assertTrue(iterator.hasNext());
        final AggregateStorageRecord actual = iterator.next();
        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_records_and_return_sorted_by_timestamp_descending() {
        final List<AggregateStorageRecord> records = Given.AggregateStorageRecord.createSequentialRecords(id);

        writeAll(id, records);

        final Iterator<AggregateStorageRecord> iterator = storage.historyBackward(id);
        final List<AggregateStorageRecord> actual = newArrayList(iterator);
        reverse(records); // expected records should be in a reverse order
        assertEquals(records, actual);
    }

    @Test
    public void write_and_read_snapshot() {
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

        storage.writeRecord(id, Given.AggregateStorageRecord.create(time1));
        storage.write(id, newSnapshot(time2));

        testWriteRecordsAndLoadHistory(time3);
    }

    @Test
    public void return_zero_event_count_after_last_snapshot_by_default() {
        assertEquals(0, storage.readEventCountAfterLastSnapshot(id));
    }

    @Test
    public void write_and_read_event_count_after_last_snapshot() {
        final int expectedCount = 32;
        storage.writeEventCountAfterLastSnapshot(id, expectedCount);

        final int actualCount = storage.readEventCountAfterLastSnapshot(id);

        assertEquals(expectedCount, actualCount);
    }

    @Test
    public void rewrite_event_count_after_last_snapshot() {
        final int primaryValue = 16;
        storage.writeEventCountAfterLastSnapshot(id, primaryValue);
        final int expectedValue = 32;
        storage.writeEventCountAfterLastSnapshot(id, expectedValue);

        final int actualCount = storage.readEventCountAfterLastSnapshot(id);

        assertEquals(expectedValue, actualCount);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_write_event_count_to_closed_storage() {
        close(storage);

        storage.writeEventCountAfterLastSnapshot(id, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_read_event_count_from_closed_storage() {
        close(storage);

        storage.readEventCountAfterLastSnapshot(id);
    }

    protected <Id> void writeAndReadEventTest(Id id, AggregateStorage<Id> storage) {
        final Event expectedEvent = Given.Event.projectCreated();

        storage.writeEvent(id, expectedEvent);

        final AggregateEvents events = storage.read(id);
        assertEquals(1, events.getEventCount());
        final Event actualEvent = events.getEvent(0);
        assertEquals(expectedEvent, actualEvent);

        close(storage);
    }

    // Ignore this test because several records can be stored by an aggregate ID.
    @Override
    @SuppressWarnings({"NoopMethodInAbstractClass", "RefusedBequest"})
    public void rewrite_record_if_write_by_the_same_id() {
    }

    protected void testWriteRecordsAndLoadHistory(Timestamp firstRecordTime) {
        final List<AggregateStorageRecord> records = Given.AggregateStorageRecord.createSequentialRecords(id, firstRecordTime);

        writeAll(id, records);

        final AggregateEvents events = storage.read(id);
        final List<Event> expectedEvents = transform(records, TO_EVENT);
        final List<Event> actualEvents = events.getEventList();
        assertEquals(expectedEvents, actualEvents);
    }

    protected void writeAll(ProjectId id, Iterable<AggregateStorageRecord> records) {
        for (AggregateStorageRecord record : records) {
            storage.writeRecord(id, record);
        }
    }

    protected static final Function<AggregateStorageRecord, Event> TO_EVENT = new Function<AggregateStorageRecord, Event>() {
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

    private static class TestAggregateWithIdString extends Aggregate<String, Project, Project.Builder> {
        private TestAggregateWithIdString(String id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdInteger extends Aggregate<Integer, Project, Project.Builder> {
        private TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdLong extends Aggregate<Long, Project, Project.Builder> {
        private TestAggregateWithIdLong(Long id) {
            super(id);
        }
    }
}
