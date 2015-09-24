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

import org.junit.*;
import org.spine3.base.EventRecord;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;

import java.util.Iterator;

import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.storage.filesystem.Helper.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection", "ConstantConditions"})
public class FileSystemEventStorageShould {

    private static final FileSystemEventStorage STORAGE = new FileSystemEventStorage();

    private ProjectId projectId;

    @Before
    public void setUpTest() {

        STORAGE.releaseResources();
        cleanTestData();
        configure(FileSystemEventStorageShould.class);

        projectId = ProjectId.newBuilder().setId("project_id").build();
    }

    @After
    public void tearDownTest() {
        STORAGE.releaseResources();
        cleanTestData();
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

    @Test
    public void save_and_read_event() {

        final EventRecord expected = projectCreated(projectId);
        STORAGE.store(expected);

        final Iterator<EventRecord> iterator = STORAGE.allEvents();

        assertTrue(iterator.hasNext());

        final EventRecord actual = iterator.next();

        assertEquals(expected.getEvent(), actual.getEvent());
        assertEquals(expected.getContext(), actual.getContext());

        assertFalse(iterator.hasNext());
    }

    public static EventRecord projectCreated(ProjectId projectId) {

        final ProjectCreated event = ProjectCreated.newBuilder().setProjectId(projectId).build();
        final EventRecord.Builder builder = EventRecord.newBuilder().setEvent(toAny(event));
        return builder.build();
    }
}
