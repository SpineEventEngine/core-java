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
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeName;
import org.spine3.test.storage.ProjectId;
import org.spine3.test.storage.command.AddTask;
import org.spine3.test.storage.command.CreateProject;
import org.spine3.test.storage.command.StartProject;
import org.spine3.test.storage.event.ProjectCreated;
import org.spine3.test.storage.event.ProjectStarted;
import org.spine3.test.storage.event.TaskAdded;
import org.spine3.users.UserId;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.test.Tests.newUserId;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

public class Given {

    public static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }

        public static ProjectId newProjectId(String uuid) {
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    public static class EventMessage {

        private static final ProjectId DUMMY_PROJECT_ID = AggregateId.newProjectId();
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

        public static Any projectCreatedAny() {
            return PROJECT_CREATED_ANY;
        }

        public static Any taskAddedAny() {
            return TASK_ADDED_ANY;
        }

        public static Any projectStartedAny() {
            return PROJECT_STARTED_ANY;
        }
    }

    public static class CommandMessage {

        private CommandMessage() {
        }

        public static CreateProject createProject() {
            return CreateProject.newBuilder()
                                .setProjectId(AggregateId.newProjectId())
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

    public static class Command {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Command() {
        }

        /**
         * Creates a new {@link Command} with the given command message, userId and timestamp using
         * default {@link org.spine3.base.CommandId CommandId} instance.
         */
        public static org.spine3.base.Command create(Message command, UserId userId, Timestamp when) {
            final CommandContext context = createCommandContext(userId, Commands.generateId(), when);
            final org.spine3.base.Command result = Commands.createCommand(command, context);
            return result;
        }

        /** Creates a new {@link Command} with default properties (current time etc). */
        public static org.spine3.base.Command createProject() {
            return createProject(getCurrentTime());
        }

        public static org.spine3.base.Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        public static org.spine3.base.Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
            final CreateProject command = CommandMessage.createProject(projectId);
            return create(command, userId, when);
        }

        public static org.spine3.base.Command addTask() {
            return addTask(USER_ID, PROJECT_ID, getCurrentTime());
        }

        public static org.spine3.base.Command addTask(UserId userId, ProjectId projectId, Timestamp when) {
            final AddTask command = CommandMessage.addTask(projectId);
            return create(command, userId, when);
        }

        public static org.spine3.base.Command startProject() {
            return startProject(USER_ID, PROJECT_ID, getCurrentTime());
        }

        public static org.spine3.base.Command startProject(UserId userId, ProjectId projectId, Timestamp when) {
            final StartProject command = CommandMessage.startProject(projectId);
            return create(command, userId, when);
        }
    }

    public static class Event {

        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Event() {
        }

        /** Creates a new {@link Event} with default properties. */
        public static org.spine3.base.Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        public static org.spine3.base.Event projectCreated(ProjectId projectId) {
            return projectCreated(projectId, createEventContext(projectId));
        }

        public static org.spine3.base.Event projectCreated(ProjectId projectId, EventContext context) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId);
            final org.spine3.base.Event event = Events.createEvent(msg, context);
            return event;
        }

        public static org.spine3.base.Event taskAdded(ProjectId projectId, EventContext context) {
            final TaskAdded msg = EventMessage.taskAdded(projectId);
            final org.spine3.base.Event event = Events.createEvent(msg, context);
            return event;
        }

        public static org.spine3.base.Event projectStarted(ProjectId projectId, EventContext context) {
            final ProjectStarted msg = EventMessage.projectStarted(projectId);
            final org.spine3.base.Event event = Events.createEvent(msg, context);
            return event;
        }
    }

    public static class EventStorageRecord {

        private EventStorageRecord() {
        }

        public static org.spine3.server.event.storage.EventStorageRecord projectCreated() {
            final Timestamp time = getCurrentTime();
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.event.storage.EventStorageRecord.Builder builder =
                    org.spine3.server.event.storage.EventStorageRecord
                            .newBuilder()
                            .setMessage(EventMessage.projectCreatedAny())
                            .setTimestamp(time)
                            .setEventId("project_created")
                            .setEventType(TypeName.from(ProjectCreated.getDescriptor()))
                            .setProducerId(projectId.getId())
                            .setContext(createEventContext(projectId, time));
            return builder.build();
        }

        public static org.spine3.server.event.storage.EventStorageRecord projectCreated(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.event.storage.EventStorageRecord.Builder result =
                    org.spine3.server.event.storage.EventStorageRecord
                            .newBuilder()
                            .setMessage(EventMessage.projectCreatedAny())
                            .setTimestamp(when)
                            .setEventId("project_created_" + when.getSeconds())
                            .setEventType(TypeName.from(ProjectCreated.getDescriptor()))
                            .setProducerId(projectId.getId())
                            .setContext(createEventContext(projectId, when));
            return result.build();
        }

        public static org.spine3.server.event.storage.EventStorageRecord taskAdded(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.event.storage.EventStorageRecord.Builder result =
                    org.spine3.server.event.storage.EventStorageRecord
                            .newBuilder()
                            .setMessage(EventMessage.taskAddedAny())
                            .setTimestamp(when)
                            .setEventId("task_added_" + when.getSeconds())
                            .setEventType(TypeName.from(TaskAdded.getDescriptor()))
                            .setProducerId(projectId.getId())
                            .setContext(createEventContext(projectId, when));
            return result.build();
        }

        public static org.spine3.server.event.storage.EventStorageRecord projectStarted(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.event.storage.EventStorageRecord.Builder result =
                    org.spine3.server.event.storage.EventStorageRecord
                            .newBuilder()
                            .setMessage(EventMessage.projectStartedAny())
                            .setTimestamp(when)
                            .setEventId("project_started_" + when.getSeconds())
                            .setEventType(TypeName.from(ProjectStarted.getDescriptor()))
                            .setProducerId(projectId.getId())
                            .setContext(createEventContext(projectId, when));
            return result.build();
        }
    }
}
