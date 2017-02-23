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

package org.spine3.server.storage;

import com.google.protobuf.Any;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.protobuf.AnyPacker;
import org.spine3.test.storage.ProjectId;
import org.spine3.test.storage.command.AddTask;
import org.spine3.test.storage.command.CreateProject;
import org.spine3.test.storage.command.StartProject;
import org.spine3.test.storage.event.ProjectCreated;
import org.spine3.test.storage.event.ProjectStarted;
import org.spine3.test.storage.event.TaskAdded;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

public class Given {

    private Given() {
        // Prevent instantiation of this utility class from outside.
    }

    static ProjectId newProjectId() {
        final String uuid = newUuid();
        return ProjectId.newBuilder()
                        .setId(uuid)
                        .build();
    }

    public static class EventMessage {

        private static final ProjectId DUMMY_PROJECT_ID = newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreated(DUMMY_PROJECT_ID);
        private static final TaskAdded TASK_ADDED = taskAdded(DUMMY_PROJECT_ID);
        private static final ProjectStarted PROJECT_STARTED = projectStarted(DUMMY_PROJECT_ID);
        private static final Any PROJECT_CREATED_ANY = AnyPacker.pack(PROJECT_CREATED);
        private static final Any TASK_ADDED_ANY = AnyPacker.pack(TASK_ADDED);
        private static final Any PROJECT_STARTED_ANY = AnyPacker.pack(PROJECT_STARTED);

        private EventMessage() {
        }

        public static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static TaskAdded taskAdded(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }

        public static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    public static class CommandMessage {

        private CommandMessage() {
        }

        public static CreateProject createProject() {
            return CreateProject.newBuilder()
                                .setProjectId(newProjectId())
                                .build();
        }

        public static CreateProject createProject(ProjectId id) {
            return CreateProject.newBuilder()
                                .setProjectId(id)
                                .build();
        }

        public static AddTask addTask(ProjectId id) {
            return AddTask.newBuilder()
                          .setProjectId(id)
                          .build();
        }

        public static StartProject startProject(ProjectId id) {
            return StartProject.newBuilder()
                               .setProjectId(id)
                               .build();
        }
    }

    public static class AnEvent {

        private static final ProjectId PROJECT_ID = newProjectId();

        private AnEvent() {
        }

        /** Creates a new {@code Event} with default properties. */
        public static Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        public static Event projectCreated(ProjectId projectId) {
            return projectCreated(projectId, createEventContext(projectId));
        }

        public static Event projectCreated(ProjectId projectId, EventContext context) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId);
            final Event event = Events.createEvent(msg, context);
            return event;
        }

        public static Event taskAdded(ProjectId projectId, EventContext context) {
            final TaskAdded msg = EventMessage.taskAdded(projectId);
            final Event event = Events.createEvent(msg, context);
            return event;
        }

        public static Event projectStarted(ProjectId projectId, EventContext context) {
            final ProjectStarted msg = EventMessage.projectStarted(projectId);
            final Event event = Events.createEvent(msg, context);
            return event;
        }
    }


}
