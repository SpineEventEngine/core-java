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

package org.spine3.testdata;

import org.spine3.server.storage.EventStoreRecord;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;

/**
 * Contains EventStoreRecords for tests.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class TestEventStoreRecordFactory {

    private static final ProjectId DUMMY_PROJECT_ID = createProjectId("stub_project_id_event_store");

    private static final ProjectCreated PROJECT_CREATED = ProjectCreated.newBuilder().setProjectId(DUMMY_PROJECT_ID).build();
    private static final EventStoreRecord PROJECT_CREATED_RECORD = EventStoreRecord.newBuilder()
            .setEvent(toAny(PROJECT_CREATED))
            .setEventId("project_created").build();

    private static final TaskAdded TASK_ADDED = TaskAdded.newBuilder().setProjectId(DUMMY_PROJECT_ID).build();
    private static final EventStoreRecord TASK_ADDED_RECORD = EventStoreRecord.newBuilder()
            .setEvent(toAny(TASK_ADDED))
            .setEventId("task_added").build();

    private static final ProjectStarted PROJECT_STARTED = ProjectStarted.newBuilder().setProjectId(DUMMY_PROJECT_ID).build();
    private static final EventStoreRecord PROJECT_STARTED_RECORD = EventStoreRecord.newBuilder()
            .setEvent(toAny(PROJECT_STARTED))
            .setEventId("project_started").build();

    private TestEventStoreRecordFactory() {}

    public static EventStoreRecord projectCreated() {
        return PROJECT_CREATED_RECORD;
    }

    public static EventStoreRecord taskAdded() {
        return TASK_ADDED_RECORD;
    }

    public static EventStoreRecord projectStarted() {
        return PROJECT_STARTED_RECORD;
    }
}
