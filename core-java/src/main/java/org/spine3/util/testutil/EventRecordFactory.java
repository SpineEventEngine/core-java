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

package org.spine3.util.testutil;

import com.google.protobuf.Any;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import static org.spine3.protobuf.Messages.toAny;

/**
 * The utility class which is used for creating EventRecords for tests.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class EventRecordFactory {

    private EventRecordFactory() {}

    public static EventRecord projectCreated(ProjectId projectId) {
        return projectCreated(projectId, EventContext.getDefaultInstance());
    }

    public static EventRecord taskAdded(ProjectId projectId) {
        return taskAdded(projectId, EventContext.getDefaultInstance());
    }

    public static EventRecord projectStarted(ProjectId projectId) {
        return projectStarted(projectId, EventContext.getDefaultInstance());
    }

    public static EventRecord projectCreated(ProjectId projectId, EventContext eventContext) {

        final ProjectCreated event = ProjectCreated.newBuilder().setProjectId(projectId).build();
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(eventContext).setEvent(toAny(event));
        return builder.build();
    }

    public static EventRecord taskAdded(ProjectId projectId, EventContext eventContext) {
        final TaskAdded taskAdded = TaskAdded.newBuilder().setProjectId(projectId).build();
        final Any event = toAny(taskAdded);
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(eventContext).setEvent(event);
        return builder.build();
    }

    public static EventRecord projectStarted(ProjectId projectId, EventContext eventContext) {

        final ProjectStarted event = ProjectStarted.newBuilder().setProjectId(projectId).build();
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(eventContext).setEvent(toAny(event));
        return builder.build();
    }
}
