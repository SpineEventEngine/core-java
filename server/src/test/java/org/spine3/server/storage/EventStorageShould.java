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

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.testdata.TestEventFactory;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.base.Events.generateId;
import static org.spine3.server.Identifiers.idToAny;
import static org.spine3.server.storage.EventStorage.toEvent;
import static org.spine3.server.storage.EventStorage.toEventList;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;
import static org.spine3.testdata.TestEventStorageRecordFactory.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public abstract class EventStorageShould {

    /**
     * Small positive delta in seconds or nanoseconds.
     */
    private static final int POSITIVE_DELTA = 10;

    /**
     * Small negative delta in seconds or nanoseconds.
     */
    private static final int NEGATIVE_DELTA = -POSITIVE_DELTA;

    private static final int ZERO = 0;

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
    public void return_null_if_no_record_with_such_id() {
        final Event event = storage.read(EventId.getDefaultInstance());

        assertNull(event);
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_events_from_empty_storage() {
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
        storage.write(generateId(), null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_by_null_id() {
        storage.write(null, Event.getDefaultInstance());
    }

    @Test
    public void write_and_read_one_event() {
        final Event expected = TestEventFactory.projectCreated();
        final EventId id = generateId();

        storage.write(id, expected);

        final Event actual = storage.read(id);
        assertEquals(expected, actual);
    }

    @Test
    public void writeInternal_and_read_one_event() {
        final EventStorageRecord recordToStore = projectCreated();
        final EventId id = EventId.newBuilder().setUuid(recordToStore.getEventId()).build();
        final Event expected = toEvent(recordToStore);

        storage.writeInternal(recordToStore);
        final Event actual = storage.read(id);

        assertEquals(expected, actual);
    }

    @Test
    public void write_and_read_several_events() {
        givenSequentialRecords();
        final List<Event> expected = toEventList(record1, record2, record3);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void return_iterator_pointed_to_first_element_if_read_all_events_several_times() {
        givenSequentialRecords();
        final List<Event> expected = toEventList(record1, record2, record3);

        assertStorageContainsOnly(expected);
        assertStorageContainsOnly(expected);
        assertStorageContainsOnly(expected);
    }

    @Test
    public void filter_events_by_type() {
        givenSequentialRecords();
        final String typeName = record1.getEventType();
        final EventFilter filter = EventFilter.newBuilder().setEventType(typeName).build();
        final EventStreamQuery query = EventStreamQuery.newBuilder().addFilter(filter).build();
        final List<Event> expected = toEventList(record1);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    @Test
    public void filter_events_by_aggregate_id() {
        givenSequentialRecords();
        final Any id = idToAny(createProjectId(record1.getAggregateId()));
        final EventFilter filter = EventFilter.newBuilder().addAggregateId(id).build();
        final EventStreamQuery query = EventStreamQuery.newBuilder().addFilter(filter).build();
        final List<Event> expected = toEventList(record1);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    /**
     * Find events which happened AFTER a point in time tests.
     */

    @Test
    public void find_events_which_happened_after_a_point_in_time_CASE_secs_BIGGER_and_nanos_BIGGER() {
        givenSequentialRecords(POSITIVE_DELTA, POSITIVE_DELTA);
        assertThereAreEventsAfterTime();
    }

    @Test
    public void find_events_which_happened_after_a_point_in_time_CASE_secs_BIGGER_and_nanos_EQUAL() {
        givenSequentialRecords(POSITIVE_DELTA, ZERO);
        assertThereAreEventsAfterTime();
    }

    @Test
    public void find_events_which_happened_after_a_point_in_time_CASE_secs_BIGGER_and_nanos_LESS() {
        givenSequentialRecords(POSITIVE_DELTA, NEGATIVE_DELTA);
        assertThereAreEventsAfterTime();
    }

    @Test
    public void find_events_which_happened_after_a_point_in_time_CASE_secs_EQUAL_and_nanos_BIGGER() {
        givenSequentialRecords(0, POSITIVE_DELTA);
        assertThereAreEventsAfterTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_after_a_point_in_time_CASE_secs_LESS_and_nanos_LESS() {
        givenSequentialRecords(NEGATIVE_DELTA, NEGATIVE_DELTA);
        assertNoEventsAfterTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_after_a_point_in_time_CASE_secs_LESS_and_nanos_BIGGER() {
        givenSequentialRecords(NEGATIVE_DELTA, POSITIVE_DELTA);
        assertNoEventsAfterTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_after_a_point_in_time_CASE_secs_EQUAL_and_nanos_LESS() {
        givenSequentialRecords(ZERO, NEGATIVE_DELTA);
        assertNoEventsAfterTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_after_a_point_in_time_CASE_secs_EQUAL_and_nanos_EQUAL() {
        givenSequentialRecords(ZERO, ZERO);
        assertNoEventsAfterTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_after_a_point_in_time_CASE_secs_LESS_and_nanos_EQUAL() {
        givenSequentialRecords(NEGATIVE_DELTA, ZERO);
        assertNoEventsAfterTime();
    }

    private void assertThereAreEventsAfterTime() {
        final EventStreamQuery query = EventStreamQuery.newBuilder().setAfter(time1).build();
        final List<Event> expected = toEventList(record2, record3);

        final Iterator<Event> iterator = storage.iterator(query);
        final List<Event> actual = newArrayList(iterator);

        assertEquals(expected, actual);
    }

    private void assertNoEventsAfterTime() {
        final EventStreamQuery query = EventStreamQuery.newBuilder().setAfter(time1).build();
        final Iterator<Event> iterator = storage.iterator(query);
        assertFalse(iterator.hasNext());
    }

    /**
     * Find events which happened BEFORE a point in time tests.
     */

    @Test
    public void find_events_which_happened_before_a_point_in_time_CASE_secs_LESS_and_nanos_LESS() {
        givenSequentialRecords(NEGATIVE_DELTA, NEGATIVE_DELTA);
        assertThereAreEventsBeforeTime();
    }

    @Test
    public void find_events_which_happened_before_a_point_in_time_CASE_secs_LESS_and_nanos_EQUAL() {
        givenSequentialRecords(NEGATIVE_DELTA, ZERO);
        assertThereAreEventsBeforeTime();
    }

    @Test
    public void find_events_which_happened_before_a_point_in_time_CASE_secs_LESS_and_nanos_BIGGER() {
        givenSequentialRecords(NEGATIVE_DELTA, POSITIVE_DELTA);
        assertThereAreEventsBeforeTime();
    }

    @Test
    public void find_events_which_happened_before_a_point_in_time_CASE_secs_EQUAL_and_nanos_LESS() {
        givenSequentialRecords(ZERO, NEGATIVE_DELTA);
        assertThereAreEventsBeforeTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_before_a_point_in_time_CASE_secs_BIGGER_and_nanos_BIGGER() {
        givenSequentialRecords(POSITIVE_DELTA, POSITIVE_DELTA);
        assertNoEventsBeforeTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_before_a_point_in_time_CASE_secs_BIGGER_and_nanos_LESS() {
        givenSequentialRecords(POSITIVE_DELTA, NEGATIVE_DELTA);
        assertNoEventsBeforeTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_before_a_point_in_time_CASE_secs_EQUAL_and_nanos_BIGGER() {
        givenSequentialRecords(ZERO, POSITIVE_DELTA);
        assertNoEventsBeforeTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_before_a_point_in_time_CASE_secs_EQUAL_and_nanos_EQUAL() {
        givenSequentialRecords(ZERO, ZERO);
        assertNoEventsBeforeTime();
    }

    @Test
    public void NOT_find_any_events_which_happened_before_a_point_in_time_CASE_secs_BIGGER_and_nanos_EQUAL() {
        givenSequentialRecords(POSITIVE_DELTA, ZERO);
        assertNoEventsBeforeTime();
    }

    private void assertThereAreEventsBeforeTime() {
        final EventStreamQuery query = EventStreamQuery.newBuilder().setBefore(time1).build();
        final List<Event> expected = toEventList(record3, record2);

        final Iterator<Event> iterator = storage.iterator(query);
        final List<Event> actual = newArrayList(iterator);

        assertEquals(expected, actual);
    }

    private void assertNoEventsBeforeTime() {
        final EventStreamQuery query = EventStreamQuery.newBuilder().setBefore(time1).build();
        final Iterator<Event> iterator = storage.iterator(query);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void find_events_which_happened_between_two_points_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .setAfter(time1)
                .setBefore(time3)
                .build();
        final List<Event> expected = toEventList(record2);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    @Test
    public void filter_events_by_type_and_aggregate_id_and_time() {
        givenSequentialRecords();
        final String typeName = record2.getEventType();
        final Any id = idToAny(createProjectId(record2.getAggregateId()));
        final EventFilter filter = EventFilter.newBuilder().setEventType(typeName).addAggregateId(id).build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                .addFilter(filter)
                .setAfter(time1)
                .setBefore(time3)
                .build();
        final List<Event> expected = toEventList(record2);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    private void givenSequentialRecords() {
        givenSequentialRecords(POSITIVE_DELTA, POSITIVE_DELTA);
    }

    private void givenSequentialRecords(long deltaSeconds, int deltaNanos) {
        final Duration delta = Duration.newBuilder().setSeconds(deltaSeconds).setNanos(deltaNanos).build();
        time1 = getCurrentTime().toBuilder().setNanos(POSITIVE_DELTA * 10).build(); // to be sure that nanos are bigger than delta
        record1 = projectCreated(time1);
        time2 = add(time1, delta);
        record2 = taskAdded(time2);
        time3 = add(time2, delta);
        record3 = projectStarted(time3);

        writeAll(record1, record2, record3);
    }

    private void writeAll(EventStorageRecord... records) {
        for (EventStorageRecord r : records) {
            storage.writeInternal(r);
        }
    }

    private void assertStorageContainsOnly(List<Event> expectedRecords) {
        final Iterator<Event> iterator = findAll();
        final List<Event> actual = newArrayList(iterator);
        assertEquals(expectedRecords, actual);
    }

    protected Iterator<Event> findAll() {
        final Iterator<Event> result = storage.iterator(EventStreamQuery.getDefaultInstance());
        return result;
    }
}
