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

package org.spine3.server.storage.memory;

import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.base.UserId;
import org.spine3.server.storage.EventStoreRecord;
import org.spine3.test.project.ProjectId;
import org.spine3.util.Users;

import java.util.Iterator;

import static org.junit.Assert.*;
import static org.spine3.testutil.ContextFactory.getEventContext;
import static org.spine3.testutil.EventRecordFactory.projectCreated;

/**
 * In-memory implementation of {@link org.spine3.server.storage.EventStorage} tests.
 *
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "ClassWithTooManyMethods",
        "DuplicateStringLiteralInspection", "ConstantConditions"})
public class InMemoryEventStorageShould {

    private InMemoryEventStorage storage;
    private ProjectId projectId;
    private EventContext eventContext;

    @Before
    public void setUp() {

        storage = (InMemoryEventStorage) InMemoryStorageFactory.getInstance().createEventStorage();
        UserId userId = Users.createId("user@testing-in-memory-storage.org");
        projectId = ProjectId.newBuilder().setId("project_id").build();
        eventContext = getEventContext(userId, projectId);
    }

    @Test
    public void return_null_if_read_one_record_from_empty_storage() {

        final EventStoreRecord record = storage.read(eventContext.getEventId());
        assertNull(record);
    }

    @Test
    public void return_null_if_read_one_record_by_null_id() {

        final EventStoreRecord record = storage.read(null);
        assertNull(record);
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_all_records_from_empty_storage() {

        final Iterator<EventRecord> iterator = storage.allEvents();
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null() {
        storage.write(null);
    }

    @Test
    public void save_and_read_event() {

        final EventRecord expected = projectCreated(projectId, eventContext);
        storage.store(expected);

        final EventStoreRecord actual = storage.read(eventContext.getEventId());

        assertEquals(expected.getEvent(), actual.getEvent());
        assertEquals(expected.getContext(), actual.getContext());
    }

    @Test
    public void save_and_read_all_events() {

        final EventRecord expected = projectCreated(projectId, eventContext);
        storage.store(expected);

        final Iterator<EventRecord> iterator = storage.allEvents();

        assertTrue(iterator.hasNext());

        final EventRecord actual = iterator.next();

        assertEquals(expected.getEvent(), actual.getEvent());
        assertEquals(expected.getContext(), actual.getContext());

        assertFalse(iterator.hasNext());
    }
}
