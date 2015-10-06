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

import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.base.EventRecordOrBuilder;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.util.testutil.ContextFactory;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.util.Events.toEventRecord;
import static org.spine3.util.Events.toEventRecordsList;
import static org.spine3.util.testutil.AggregateIdFactory.createCommon;
import static org.spine3.util.testutil.EventStoreRecordFactory.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "ConstantConditions",
        "AbstractClassWithoutAbstractMethods", "ConstructorNotProtectedInAbstractClass"})
public abstract class EventStorageShould {

    private final EventStorage storage;

    public EventStorageShould(EventStorage storage) {
        this.storage = storage;
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_records_from_empty_storage() {

        final Iterator<EventRecord> iterator = storage.allEvents();
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null() {
        storage.write(null);
    }

    @Test
    public void store_and_read_one_event() {

        final EventRecord expected = getProjectCreatedRecord();
        storage.store(expected);

        assertStorageContains(expected);
    }

    @Test
    public void write_and_read_one_event() {

        final EventStoreRecord recordToStore = projectCreated();
        final EventRecord expected = toEventRecord(recordToStore);

        storage.write(recordToStore);

        assertStorageContains(expected);
    }

    private void assertStorageContains(EventRecordOrBuilder expected) {

        final Iterator<EventRecord> iterator = storage.allEvents();

        assertTrue(iterator.hasNext());

        final EventRecord actual = iterator.next();

        assertEventRecordsAreEqual(expected, actual);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_and_read_several_events() {

        final List<EventStoreRecord> recordsToStore = createEventStoreRecords();
        final List<EventRecord> expectedRecords = toEventRecordsList(recordsToStore);

        for (EventStoreRecord r : recordsToStore) {
            storage.write(r);
        }

        final Iterator<EventRecord> iterator = storage.allEvents();
        final List<EventRecord> actualRecords = newArrayList(iterator);

        assertEventRecordListsAreEqual(expectedRecords, actualRecords);
    }

    @Test
    public void return_iterator_pointed_to_first_element_if_read_all_events_several_times() {

        final List<EventStoreRecord> recordsToStore = createEventStoreRecords();
        final List<EventRecord> expectedRecords = toEventRecordsList(recordsToStore);

        for (EventStoreRecord r : recordsToStore) {
            storage.write(r);
        }

        final Iterator<EventRecord> iteratorFirst = storage.allEvents();
        final List<EventRecord> actualRecordsFirst = newArrayList(iteratorFirst);
        assertEventRecordListsAreEqual(expectedRecords, actualRecordsFirst);

        final Iterator<EventRecord> iteratorSecond = storage.allEvents();
        final List<EventRecord> actualRecordsSecond = newArrayList(iteratorSecond);
        assertEventRecordListsAreEqual(expectedRecords, actualRecordsSecond);
    }

    private static void assertEventRecordListsAreEqual(List<EventRecord> expected, List<EventRecord> actual) {
        if (expected.size() != actual.size()) {
            fail("Expected records count: " + expected.size() + " is not equal to actual records count: " + actual.size());
        }
        for (int i = 0; i < expected.size(); i++) {
            assertEventRecordsAreEqual(expected.get(i), actual.get(i));
        }
    }

    protected static void assertEventRecordsAreEqual(EventRecordOrBuilder expected, EventRecordOrBuilder actual) {
        assertEquals(expected.getEvent(), actual.getEvent());
        assertEquals(expected.getContext(), actual.getContext());
    }

    private static List<EventStoreRecord> createEventStoreRecords() {
        return newArrayList(projectCreated(), projectStarted(), taskAdded());
    }

    private static EventRecord getProjectCreatedRecord() {
        final ProjectCreated event = ProjectCreated.newBuilder().setProjectId(createCommon()).build();
        final EventContext context = ContextFactory.getEventContext();
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(context).setEvent(toAny(event));
        return builder.build();
    }
}
