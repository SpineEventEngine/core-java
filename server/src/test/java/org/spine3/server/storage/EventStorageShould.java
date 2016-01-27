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

import com.google.protobuf.Duration;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventRecord;
import org.spine3.server.stream.EventRecordFilter;
import org.spine3.server.stream.EventStreamQuery;
import org.spine3.server.util.Events;
import org.spine3.test.project.ProjectId;
import org.spine3.testdata.TestEventRecordFactory;
import org.spine3.type.TypeName;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.storage.StorageUtil.*;
import static org.spine3.server.util.Identifiers.newUuid;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;
import static org.spine3.testdata.TestEventStorageRecordFactory.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public abstract class EventStorageShould {

    /**
     * The point in time when the first event happened.
     */
    private Timestamp time1;
    private EventStorageRecord record1;

    /**
     * The point in time when the second event happened.
     */
    @SuppressWarnings("FieldCanBeLocal") // to be consistent
    private Timestamp time2;
    private EventStorageRecord record2;

    /**
     * The point in time when the third event happened.
     */
    private Timestamp time3;
    private EventStorageRecord record3;

    private EventStorage storage;

    @Before
    public void setUpTest() {
        storage = getStorage();
    }

    protected abstract EventStorage getStorage();

    @Test
    public void return_iterator_over_empty_collection_if_read_records_from_empty_storage() {
        final Iterator<EventRecord> iterator = findAll();

        assertFalse(iterator.hasNext());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null() {
        storage.writeInternal(null);
    }

    @Test
    public void store_and_read_one_event() {
        final EventRecord expected = TestEventRecordFactory.projectCreated();

        storage.write(Events.generateId(), expected);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void write_and_read_one_event() {
        final EventStorageRecord recordToStore = projectCreated();
        final EventRecord expected = toEventRecord(recordToStore);

        storage.writeInternal(recordToStore);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void write_and_read_several_events() {
        final List<EventStorageRecord> recordsToStore = createEventStorageRecords();
        final List<EventRecord> expectedRecords = toEventRecordList(recordsToStore);

        writeAll(recordsToStore);

        assertStorageContainsOnly(expectedRecords);
    }

    @Test
    public void write_and_filter_events_by_type() {
        final EventStorageRecord expectedRecord = EventStorageRecord.newBuilder()
                .setEvent(toAny(newRandomStringValue()))
                .setEventId(Events.generateId().getUuid())
                .build();
        writeAll(expectedRecord, projectStarted(), taskAdded());

        final String typeName = TypeName.of(StringValue.class).value();
        final EventRecordFilter filter = EventRecordFilter.newBuilder()
                .setEventType(typeName).build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .addFilter(filter).build();
        final List<EventRecord> expectedRecords = toEventRecordList(expectedRecord);

        final Iterator<EventRecord> actual = storage.iterator(query);

        assertEquals(expectedRecords, newArrayList(actual));
    }

    @Test
    public void write_and_filter_events_by_aggregate_id() {
        final ProjectId id = createProjectId("project-created-" + newUuid());
        final EventRecord expectedRecord = TestEventRecordFactory.projectCreated(id);
        writeAll(toEventStorageRecord(expectedRecord), projectStarted(), taskAdded());

        final EventRecordFilter filter = EventRecordFilter.newBuilder()
                .addAggregateId(toAny(id)).build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .addFilter(filter).build();

        final Iterator<EventRecord> actual = storage.iterator(query);

        assertEquals(newArrayList(expectedRecord), newArrayList(actual));
    }

    @Test
    public void write_and_find_events_which_happened_after_a_point_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .setAfter(time1).build();
        final List<EventRecord> expectedRecords = toEventRecordList(record2, record3);

        final Iterator<EventRecord> actual = storage.iterator(query);

        assertEquals(expectedRecords, newArrayList(actual));
    }

    @Test
    public void write_and_find_events_which_happened_before_a_point_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .setBefore(time3).build();
        final List<EventRecord> expectedRecords = toEventRecordList(record1, record2);

        final Iterator<EventRecord> actual = storage.iterator(query);

        assertEquals(expectedRecords, newArrayList(actual));
    }

    @Test
    public void write_and_find_events_which_happened_between_two_points_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .setAfter(time1)
                .setBefore(time3)
                .build();
        final List<EventRecord> expectedRecords = toEventRecordList(record2);

        final Iterator<EventRecord> actual = storage.iterator(query);

        assertEquals(expectedRecords, newArrayList(actual));
    }

    private void givenSequentialRecords() {
        final Duration delta = seconds(10);
        time1 = getCurrentTime();
        record1 = projectCreated(time1);
        time2 = add(time1, delta);
        record2 = taskAdded(time2);
        time3 = add(time2, delta);
        record3 = projectStarted(time3);

        writeAll(record1, record2, record3);
    }

    @Test
    public void return_iterator_pointed_to_first_element_if_read_all_events_several_times() {
        final List<EventStorageRecord> recordsToStore = createEventStorageRecords();
        final List<EventRecord> expectedRecords = toEventRecordList(recordsToStore);

        writeAll(recordsToStore);

        assertStorageContainsOnly(expectedRecords);
        assertStorageContainsOnly(expectedRecords);
        assertStorageContainsOnly(expectedRecords);
    }

    private void writeAll(Iterable<EventStorageRecord> records) {
        for (EventStorageRecord r : records) {
            storage.writeInternal(r);
        }
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    private void writeAll(EventStorageRecord... records) {
        for (EventStorageRecord r : records) {
            storage.writeInternal(r);
        }
    }

    private void assertStorageContainsOnly(EventRecord expected) {
        final Iterator<EventRecord> iterator = findAll();

        assertTrue(iterator.hasNext());

        final EventRecord actual = iterator.next();

        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    private void assertStorageContainsOnly(List<EventRecord> expectedRecords) {
        final Iterator<EventRecord> iterator = findAll();
        final List<EventRecord> actualRecords = newArrayList(iterator);
        assertEquals(expectedRecords, actualRecords);
    }

    private static List<EventStorageRecord> createEventStorageRecords() {
        return newArrayList(projectCreated(), projectStarted(), taskAdded());
    }

    protected Iterator<EventRecord> findAll() {
        final Iterator<EventRecord> result = storage.iterator(EventStreamQuery.getDefaultInstance());
        return result;
    }

    private static StringValue newRandomStringValue() {
        return StringValue.newBuilder().setValue(newUuid()).build();
    }
}
