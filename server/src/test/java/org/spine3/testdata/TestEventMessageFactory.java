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

package org.spine3.testdata;

import com.google.protobuf.Any;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;

/**
 * Contains events for tests.
 *
 * @author Alexander Litus
 */
public class TestEventMessageFactory {

    private static final ProjectId DUMMY_PROJECT_ID = createProjectId("testProjectID");

    private static final ProjectCreated PROJECT_CREATED = projectCreatedEvent(DUMMY_PROJECT_ID);
    private static final TaskAdded TASK_ADDED = taskAddedEvent(DUMMY_PROJECT_ID);
    private static final ProjectStarted PROJECT_STARTED = projectStartedEvent(DUMMY_PROJECT_ID);

    private static final Any PROJECT_CREATED_ANY = toAny(PROJECT_CREATED);
    private static final Any TASK_ADDED_ANY = toAny(TASK_ADDED);
    private static final Any PROJECT_STARTED_ANY = toAny(PROJECT_STARTED);

    private TestEventMessageFactory() {}


    public static ProjectCreated projectCreatedEvent() {
        return PROJECT_CREATED;
    }

    public static TaskAdded taskAddedEvent() {
        return TASK_ADDED;
    }

    public static ProjectStarted projectStartedEvent() {
        return PROJECT_STARTED;
    }

    public static Any projectCreatedEventAny() {
        return PROJECT_CREATED_ANY;
    }

    public static Any taskAddedEventAny() {
        return TASK_ADDED_ANY;
    }

    public static Any projectStartedEventAny() {
        return PROJECT_STARTED_ANY;
    }

    public static ProjectCreated projectCreatedEvent(ProjectId id) {
        return ProjectCreated.newBuilder().setProjectId(id).build();
    }

    public static ProjectCreated projectCreatedEvent(String projectId) {
        return ProjectCreated.newBuilder().setProjectId(
                ProjectId.newBuilder().setId(projectId).build()
        ).build();
    }

    public static TaskAdded taskAddedEvent(ProjectId id) {
        return TaskAdded.newBuilder().setProjectId(id).build();
    }

    public static ProjectStarted projectStartedEvent(ProjectId id) {
        return ProjectStarted.newBuilder().setProjectId(id).build();
    }
}
