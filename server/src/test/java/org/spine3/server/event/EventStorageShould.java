/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.base.FieldFilter;
import org.spine3.base.stringifiers.Identifiers;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Durations2;
import org.spine3.protobuf.TypeName;
import org.spine3.server.storage.AbstractStorageShould;
import org.spine3.test.storage.ProjectId;
import org.spine3.test.storage.event.ProjectCreated;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.Timestamps.add;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.spine3.base.Events.generateId;
import static org.spine3.protobuf.Durations2.nanos;
import static org.spine3.protobuf.Durations2.seconds;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.server.event.Given.AnEventRecord.projectCreated;
import static org.spine3.server.storage.Given.EventMessage.projectCreated;

public abstract class EventStorageShould
        extends AbstractStorageShould<EventId, Event, EventStorage> {

    /** Small positive delta in seconds or nanoseconds. */
    private static final int POSITIVE_DELTA = 10;

    /** Small negative delta in seconds or nanoseconds. */
    private static final int NEGATIVE_DELTA = -POSITIVE_DELTA;

    private static final int ZERO = 0;

    /** The point in time when the first event happened. */
    private Timestamp time1;
    private Event event1;

    /** The point in time when the second event happened. */
    @SuppressWarnings("FieldCanBeLocal") // to be consistent
    private Timestamp time2;
    private Event event2;

    /** The point in time when the third event happened. */
    private Timestamp time3;
    private Event event3;

    private EventStorage storage;

    @Before
    public void setUpEventStorageTest() {
        storage = getStorage();
    }

    @After
    public void tearDownEventStorageTest() {
        close(storage);
    }

    @Override
    protected Event newStorageRecord() {
        return Given.AnEvent.projectCreated();
    }

    @Override
    protected EventId newId() {
        return generateId();
    }

    @Test
    public void have_index_of_identifiers() {
        assertNotNull(storage.index());
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_events_from_empty_storage() {
        final Iterator<Event> iterator = findAll();

        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_and_read_several_events() {
        givenSequentialRecords();
        final List<Event> expected = ImmutableList.of(event1, event2, event3);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void return_iterator_pointed_to_first_element_if_read_all_events_several_times() {
        givenSequentialRecords();
        final List<Event> expected = ImmutableList.of(event1, event2, event3);

        assertStorageContainsOnly(expected);
        assertStorageContainsOnly(expected);
        assertStorageContainsOnly(expected);
    }

    @Test
    public void filter_events_by_type() {
        givenSequentialRecords();
        final String typeName = TypeName.ofEvent(event1);
        final EventFilter filter = EventFilter.newBuilder()
                                              .setEventType(typeName)
                                              .build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(filter)
                                                       .build();
        final List<Event> expected = ImmutableList.of(event1);

        final Iterator<Event> iterator = storage.iterator(query);
        final List<Event> actual = newArrayList(iterator);

        assertEquals(expected, actual);
    }

    @Test
    public void filter_events_by_aggregate_id() {
        givenSequentialRecords();
        final Any id = event1.getContext().getProducerId();
        final EventFilter filter = EventFilter.newBuilder()
                                              .addAggregateId(id)
                                              .build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(filter)
                                                       .build();
        final List<Event> expected = ImmutableList.of(event1);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    @Test
    public void filter_events_by_event_message_fields() {
        givenSequentialRecords();
        final ProjectId uid = ProjectId.newBuilder()
                                       .setId(Identifiers.newUuid())
                                       .build();
        final ProjectCreated projectCreated = projectCreated(uid);
        final Any projectCreatedAny = AnyPacker.pack(projectCreated);
        final Any eventProducerId = AnyPacker.pack(uid);
        final EventContext context = EventContext.newBuilder()
                                                 .setProducerId(eventProducerId)
                                                 .setTimestamp(getCurrentTime())
                                                 .build();

        final Event event = Events.createEvent(projectCreatedAny, context);

        storage.write(event.getContext().getEventId(),
                      event);

        final Any projectIdPacked = AnyPacker.pack(uid);
        final FieldFilter fieldFilter =
                FieldFilter.newBuilder()
                           .setFieldPath(projectCreated.getClass()
                                                       .getCanonicalName() + ".projectId")
                           .addValue(projectIdPacked)
                           .build();
        final EventFilter eventFilter
                = EventFilter.newBuilder()
                             .addEventFieldFilter(fieldFilter)
                             .build();
        final EventStreamQuery streamQuery = EventStreamQuery.newBuilder()
                                                             .addFilter(eventFilter)
                                                             .build();
        final Iterator<Event> read = storage.iterator(streamQuery);

        assertNotNull(read);
        assertTrue(read.hasNext());
        final Event first = read.next();
        assertFalse(read.hasNext());

        final ProjectCreated singleMessage = Events.getMessage(first);
        assertEquals(uid, singleMessage.getProjectId());
        assertEquals(projectCreated, singleMessage);
    }

    @Test
    public void filter_events_by_context_fields() {
        givenSequentialRecords();
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(Identifiers.newUuid())
                                             .build();
        final ProjectCreated projectCreated = ProjectCreated.newBuilder()
                                                            .setProjectId(projectId)
                                                            .build();
        final Any eventAny = AnyPacker.pack(projectCreated);
        final Message producerId = ProjectId.newBuilder()
                                            .setId(Identifiers.newUuid())
                                            .build();
        final Any eventProducerId = AnyPacker.pack(producerId);
        final EventContext context = EventContext.newBuilder()
                                                 .setProducerId(eventProducerId)
                                                 .setTimestamp(getCurrentTime())
                                                 .build();

        final Event event = Events.createEvent(eventAny, context);

        storage.write(event.getContext()
                           .getEventId(), event);

        final FieldFilter contextFieldFilter =
                FieldFilter.newBuilder()
                           .setFieldPath(
                                   EventContext.class.getCanonicalName() + ".producerId")
                           .addValue(eventProducerId)
                           .build();
        final EventFilter eventFilter = EventFilter.newBuilder()
                                                   .addContextFieldFilter(contextFieldFilter)
                                                   .build();
        final EventStreamQuery streamQuery = EventStreamQuery.newBuilder()
                                                             .addFilter(eventFilter)
                                                             .build();
        final Iterator<Event> read = storage.iterator(streamQuery);

        assertTrue(read.hasNext());
        final Event singleEvent = read.next();
        assertFalse(read.hasNext());

        final Message eventMessage = Events.getMessage(singleEvent);
        assertEquals(projectCreated, eventMessage);
        final EventContext eventContext = singleEvent.getContext();
        assertEquals(context, eventContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_filter_events_by_empty_field_name() {
        givenSequentialRecords();
        final FieldFilter contextFieldFilter =
                FieldFilter.newBuilder()
                           .setFieldPath(
                                   EventContext.class.getCanonicalName() + '.') // empty field name
                           .addValue(Any.getDefaultInstance())
                           .build();
        final EventFilter eventFilter = EventFilter.newBuilder()
                                                   .addContextFieldFilter(contextFieldFilter)
                                                   .build();
        final EventStreamQuery streamQuery = EventStreamQuery.newBuilder()
                                                             .addFilter(eventFilter)
                                                             .build();
        final Iterator<Event> read = storage.iterator(streamQuery);
        if (read.hasNext()) {
            read.next(); // Invoke all lazy operations
        } else {
            fail("Cannot ensure the element presence in the Event iterator.");
        }
    }

    /*
     * Find events which happened AFTER a point in time tests.
     *************************************************************/

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
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setAfter(time1)
                                                       .build();
        final List<Event> expected = ImmutableList.of(event2, event3);

        final Iterator<Event> iterator = storage.iterator(query);
        final List<Event> actual = newArrayList(iterator);

        assertEquals(expected, actual);
    }

    private void assertNoEventsAfterTime() {
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setAfter(time1)
                                                       .build();
        final Iterator<Event> iterator = storage.iterator(query);
        assertFalse(iterator.hasNext());
    }

    /*
     * Find events which happened BEFORE a point in time tests.
     ***********************************************************/

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
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setBefore(time1)
                                                       .build();
        final List<Event> expected = ImmutableList.of(event3, event2);

        final Iterator<Event> iterator = storage.iterator(query);
        final List<Event> actual = newArrayList(iterator);

        assertEquals(expected, actual);
    }

    private void assertNoEventsBeforeTime() {
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setBefore(time1)
                                                       .build();
        final Iterator<Event> iterator = storage.iterator(query);
        assertFalse(iterator.hasNext());
    }

    /*
     * Find events which happened between two points in time, etc.
     *************************************************************/

    @Test
    public void find_events_which_happened_between_two_points_in_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .setAfter(time1)
                                                       .setBefore(time3)
                                                       .build();
        final List<Event> expected = ImmutableList.of(event2);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    @Test
    public void find_event_by_type_and_aggregate_id_and_time() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(newEventFilterFor(event2))
                                                       .setAfter(time1)
                                                       .setBefore(time3)
                                                       .build();
        final List<Event> expected = ImmutableList.of(event2);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    @Test
    public void find_several_events_by_types_and_aggregate_ids() {
        givenSequentialRecords();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(newEventFilterFor(event1))
                                                       .addFilter(newEventFilterFor(event2))
                                                       .addFilter(newEventFilterFor(event3))
                                                       .build();
        final List<Event> expected = ImmutableList.of(event1, event2, event3);

        final Iterator<Event> actual = storage.iterator(query);

        assertEquals(expected, newArrayList(actual));
    }

    private static EventFilter newEventFilterFor(Event event) {
        final Any id = event.getContext()
                            .getProducerId();
        return EventFilter.newBuilder()
                          .addAggregateId(id)
                          .build();
    }

    private void givenSequentialRecords() {
        givenSequentialRecords(POSITIVE_DELTA, POSITIVE_DELTA);
    }

    private static Duration createDelta(long deltaSeconds, int deltaNanos) {
        if (deltaNanos == 0) {
            return seconds(deltaSeconds);
        }

        if (deltaSeconds == 0) {
            return nanos(deltaNanos);
        }

        // If deltaNanos is negative, subtract its value from seconds.
        if (deltaSeconds > 0 && deltaNanos < 0) {
            return com.google.protobuf.util.Durations.subtract(seconds(deltaSeconds),
                                                               nanos(deltaNanos));
        }

        // If deltaSeconds is negative and nanos are positive, add nanos.
        if (deltaSeconds < 0 && deltaNanos > 0) {
            return Durations2.add(seconds(deltaSeconds), nanos(deltaNanos));
        }

        // The params have the same sign, just create a Duration instance using them.
        return Duration.newBuilder()
                       .setSeconds(deltaSeconds)
                       .setNanos(deltaNanos)
                       .build();
    }

    private void givenSequentialRecords(long deltaSeconds, int deltaNanos) {
        final Duration delta = createDelta(deltaSeconds, deltaNanos);
        time1 = getCurrentTime().toBuilder()
                                .setNanos(POSITIVE_DELTA * 10)
                                .build(); // to be sure that nanos are bigger than delta
        event1 = projectCreated(time1);
        time2 = add(time1, delta);
        event2 = Given.AnEventRecord.taskAdded(time2);
        time3 = add(time2, delta);
        event3 = Given.AnEventRecord.projectStarted(time3);

        writeAll(event1, event2, event3);
    }

    protected void writeAll(Event... events) {
        for (Event e : events) {
            storage.write(e.getContext().getEventId(), e);
        }
    }

    protected void assertStorageContainsOnly(List<Event> expectedRecords) {
        final Iterator<Event> iterator = findAll();
        final List<Event> actual = newArrayList(iterator);
        assertEquals(expectedRecords, actual);
    }

    protected Iterator<Event> findAll() {
        final Iterator<Event> result = storage.iterator(EventStreamQuery.getDefaultInstance());
        return result;
    }
}
