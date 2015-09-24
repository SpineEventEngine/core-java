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

package org.spine3.server.storage.filesystem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventRecord;
import org.spine3.test.project.ProjectId;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.spine3.server.storage.filesystem.Helper.cleanTestData;
import static org.spine3.server.storage.filesystem.Helper.configure;
import static org.spine3.util.testutil.EventRecordFactory.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection", "ConstantConditions"})
public class FileSystemEventStorageShould {

    private static final FileSystemEventStorage STORAGE = (FileSystemEventStorage) FileSystemEventStorage.newInstance();

    private ProjectId id;

    @Before
    public void setUpTest() {
        STORAGE.releaseResources();
        configure(FileSystemEventStorageShould.class);
        cleanTestData();

        id = ProjectId.newBuilder().setId("project_id").build();
    }

    @After
    public void tearDownTest() {
        STORAGE.releaseResources();
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_all_records_from_empty_storage() {

        final Iterator<EventRecord> iterator = STORAGE.allEvents();
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null() {
        STORAGE.write(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throw_exception_if_try_to_remove_element_via_iterator() {

        final Iterator<EventRecord> iterator = STORAGE.allEvents();
        iterator.remove();
    }

    @Test
    public void save_and_read_events() {

        final EventRecord expected = projectCreated(id);
        STORAGE.store(expected);

        final Iterator<EventRecord> iterator = STORAGE.allEvents();

        assertTrue(iterator.hasNext());

        final EventRecord actual = iterator.next();

        assertEventRecordsAreEqual(expected, actual);
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void throw_exception_if_iteration_has_no_more_elements() {

        final EventRecord expected = projectCreated(id);
        STORAGE.store(expected);

        final Iterator<EventRecord> iterator = STORAGE.allEvents();

        assertTrue(iterator.hasNext());

        final EventRecord actual = iterator.next();

        assertEventRecordsAreEqual(expected, actual);
        assertFalse(iterator.hasNext());

        iterator.next();
    }

    @Test
    public void return_iterator_pointed_to_first_element_if_read_all_events_several_times() {

        final List<EventRecord> expectedRecords = newArrayList(projectCreated(id), projectStarted(id), taskAdded(id));

        for (EventRecord r : expectedRecords) {
            STORAGE.store(r);
        }

        final Iterator<EventRecord> iteratorFirst = STORAGE.allEvents();
        final List<EventRecord> actualRecordsFirst = newArrayList(iteratorFirst);
        assertEventRecordListsAreEqual(expectedRecords, actualRecordsFirst);

        final Iterator<EventRecord> iteratorSecond = STORAGE.allEvents();
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

    @SuppressWarnings("TypeMayBeWeakened")
    private static void assertEventRecordsAreEqual(EventRecord expected, EventRecord actual) {
        assertEquals(expected.getEvent(), actual.getEvent());
        assertEquals(expected.getContext(), actual.getContext());
    }
}
