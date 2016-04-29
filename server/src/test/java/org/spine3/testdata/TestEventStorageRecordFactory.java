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

import com.google.protobuf.Timestamp;
import org.spine3.server.storage.EventStorageRecord;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.type.TypeName;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.testdata.TestAggregateIdFactory.newProjectId;
import static org.spine3.testdata.TestEventContextFactory.*;
import static org.spine3.testdata.TestEventMessageFactory.*;

/**
 * Contains EventStorageRecords for tests.
 *
 * @author Alexander Litus
 */
public class TestEventStorageRecordFactory {

    private TestEventStorageRecordFactory() {}

    public static EventStorageRecord projectCreated() {
        final Timestamp time = getCurrentTime();
        final ProjectId projectId = newProjectId();
        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                .setMessage(projectCreatedEventAny())
                .setTimestamp(time)
                .setEventId("project_created")
                .setEventType(TypeName.of(ProjectCreated.getDescriptor()).value())
                .setProducerId(projectId.getId())
                .setContext(createEventContext(projectId, time));
        return builder.build();
    }

    public static EventStorageRecord taskAdded() {
        final Timestamp time = getCurrentTime();
        final ProjectId projectId = newProjectId();
        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                .setMessage(taskAddedEventAny())
                .setTimestamp(time)
                .setEventId("task_added")
                .setEventType(TypeName.of(TaskAdded.getDescriptor()).value())
                .setProducerId(projectId.getId())
                .setContext(createEventContext(projectId, time));
        return builder.build();
    }

    public static EventStorageRecord projectStarted() {
        final Timestamp time = getCurrentTime();
        final ProjectId projectId = newProjectId();
        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                .setMessage(projectStartedEventAny())
                .setTimestamp(time)
                .setEventId("project_started")
                .setEventType(TypeName.of(ProjectStarted.getDescriptor()).value())
                .setProducerId(projectId.getId())
                .setContext(createEventContext(projectId, time));
        return builder.build();
    }

    public static EventStorageRecord projectCreated(Timestamp when) {
        final ProjectId projectId = newProjectId();
        final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                .setMessage(projectCreatedEventAny())
                .setTimestamp(when)
                .setEventId("project_created_" + when.getSeconds())
                .setEventType(TypeName.of(ProjectCreated.getDescriptor()).value())
                .setProducerId(projectId.getId())
                .setContext(createEventContext(projectId, when));
        return result.build();
    }

    public static EventStorageRecord taskAdded(Timestamp when) {
        final ProjectId projectId = newProjectId();
        final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                .setMessage(taskAddedEventAny())
                .setTimestamp(when)
                .setEventId("task_added_" + when.getSeconds())
                .setEventType(TypeName.of(TaskAdded.getDescriptor()).value())
                .setProducerId(projectId.getId())
                .setContext(createEventContext(projectId, when));
        return result.build();
    }

    public static EventStorageRecord projectStarted(Timestamp when) {
        final ProjectId projectId = newProjectId();
        final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                .setMessage(projectStartedEventAny())
                .setTimestamp(when)
                .setEventId("project_started_" + when.getSeconds())
                .setEventType(TypeName.of(ProjectStarted.getDescriptor()).value())
                .setProducerId(projectId.getId())
                .setContext(createEventContext(projectId, when));
        return result.build();
    }
}
