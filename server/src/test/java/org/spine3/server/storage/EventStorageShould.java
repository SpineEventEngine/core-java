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
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.test.project.ProjectId;
import org.spine3.testdata.TestEventFactory;
import org.spine3.type.TypeName;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.Identifiers.newUuid;
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
        final Iterator<Event> iterator = findAll();

        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_read_with_null_id() {
        //noinspection ConstantConditions
        storage.read(null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null() {
        storage.write(EventId.getDefaultInstance(), null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_by_null_id() {
        storage.write(null, Event.getDefaultInstance());
    }

    @Test
    public void store_and_read_one_event() {
        final Event expected = TestEventFactory.projectCreated();

        storage.write(Events.generateId(), expected);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void write_and_read_one_event() {
        final EventStorageRecord recordToStore = projectCreated();
        final Event expected = EventStorage.toEvent(recordToStore);

        storage.writeInternal(recordToStore);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void write_and_read_several_events() {
        final List<EventStorageRecord> recordsToStore = createEventStorageRecords();
        final List<Event> expectedEvents
                = EventStorage.toEventList(recordsToStore);

        writeAll(recordsToStore);

        assertStorageContainsOnly(expectedEvents);
    }

    @Test
    public void write_and_filter_events_by_type() {
        final EventStorageRecord expectedRecord = EventStorageRecord.newBuilder()
                .setMessage(toAny(newRandomStringValue()))
                .setEventId(Events.generateId().getUuid())
                .build();
        writeAll(expectedRecord, projectStarted(), taskAdded());

        final String typeName = TypeName.of(StringValue.class).value();
        final EventFilter filter = EventFilter.newBuilder()
                .setEventType(typeName).build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .addFilter(filter).build();
        final List<Event> expectedRecords = EventStorage.toEventList(expectedRecord);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expectedRecords, newArrayList(actual));
    }

    @Test
    public void write_and_filter_events_by_aggregate_id() {
        final ProjectId id = createProjectId("project-created-" + newUuid());
        final Event expectedRecord = TestEventFactory.projectCreated(id);
        writeAll(EventStorage.toEventStorageRecord(expectedRecord), projectStarted(), taskAdded());

        final EventFilter filter = EventFilter.newBuilder()
                .addAggregateId(toAny(id)).build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .addFilter(filter).build();

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(newArrayList(expectedRecord), newArrayList(actual));
    }

    @Test
    public void write_and_find_events_which_happened_after_a_point_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .setAfter(time1).build();
        final List<Event> expectedRecords = EventStorage.toEventList(record2, record3);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expectedRecords, newArrayList(actual));
    }

    @Test
    public void write_and_find_events_which_happened_before_a_point_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .setBefore(time3).build();
        final List<Event> expectedRecords = EventStorage.toEventList(record1, record2);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expectedRecords, newArrayList(actual));
    }

    @Test
    public void write_and_find_events_which_happened_between_two_points_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .setAfter(time1)
                .setBefore(time3)
                .build();
        final List<Event> expectedRecords = EventStorage.toEventList(record2);

        final Iterator<Event> actual = storage.iterator(query);

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
        final List<Event> expectedRecords = EventStorage.toEventList(recordsToStore);

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

    private void assertStorageContainsOnly(Event expected) {
        final Iterator<Event> iterator = findAll();

        assertTrue(iterator.hasNext());

        final Event actual = iterator.next();

        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    private void assertStorageContainsOnly(List<Event> expectedRecords) {
        final Iterator<Event> iterator = findAll();
        final List<Event> actualRecords = newArrayList(iterator);
        assertEquals(expectedRecords, actualRecords);
    }

    private static List<EventStorageRecord> createEventStorageRecords() {
        return newArrayList(projectCreated(), projectStarted(), taskAdded());
    }

    protected Iterator<Event> findAll() {
        final Iterator<Event> result = storage.iterator(EventStreamQuery.getDefaultInstance());
        return result;
    }

    private static StringValue newRandomStringValue() {
        return StringValue.newBuilder().setValue(newUuid()).build();
    }
}
