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

import org.junit.Test;
import org.spine3.base.EventRecord;
import org.spine3.server.stream.EventStreamQuery;
import org.spine3.testdata.TestEventRecordFactory;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.spine3.server.storage.StorageUtil.toEventRecord;
import static org.spine3.server.storage.StorageUtil.toEventRecordsList;
import static org.spine3.testdata.TestEventStorageRecordFactory.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "AbstractClassWithoutAbstractMethods"})
public abstract class EventStorageShould {

    private final EventStorage storage;

    protected EventStorageShould(EventStorage storage) {
        this.storage = storage;
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_records_from_empty_storage() {
        final Iterator<EventRecord> iterator = findAll();

        assertFalse(iterator.hasNext());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null() {
        storage.write(null);
    }

    @Test
    public void store_and_read_one_event() {
        final EventRecord expected = TestEventRecordFactory.projectCreated();

        storage.store(expected);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void write_and_read_one_event() {
        final EventStorageRecord recordToStore = projectCreated();
        final EventRecord expected = toEventRecord(recordToStore);

        storage.write(recordToStore);

        assertStorageContainsOnly(expected);
    }

    @Test
    public void write_and_read_several_events() {
        final List<EventStorageRecord> recordsToStore = createEventStorageRecords();
        final List<EventRecord> expectedRecords = toEventRecordsList(recordsToStore);

        writeAll(recordsToStore);

        assertStorageContainsOnly(expectedRecords);
    }

    @Test
    public void return_iterator_pointed_to_first_element_if_read_all_events_several_times() {
        final List<EventStorageRecord> recordsToStore = createEventStorageRecords();
        final List<EventRecord> expectedRecords = toEventRecordsList(recordsToStore);

        writeAll(recordsToStore);

        assertStorageContainsOnly(expectedRecords);
        assertStorageContainsOnly(expectedRecords);
        assertStorageContainsOnly(expectedRecords);
    }

    private void writeAll(Iterable<EventStorageRecord> records) {
        for (EventStorageRecord r : records) {
            storage.write(r);
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
}
