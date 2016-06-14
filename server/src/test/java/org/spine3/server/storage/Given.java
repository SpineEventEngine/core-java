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

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.EventContext;
import org.spine3.test.storage.ProjectId;
import org.spine3.test.storage.command.AddTask;
import org.spine3.test.storage.command.CreateProject;
import org.spine3.test.storage.command.StartProject;
import org.spine3.test.storage.event.ProjectCreated;
import org.spine3.test.storage.event.ProjectStarted;
import org.spine3.test.storage.event.TaskAdded;
import org.spine3.type.TypeName;
import org.spine3.users.UserId;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;


@SuppressWarnings("EmptyClass")
/* package */ class Given {

    /* package */ static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }

        /**
         * Creates a new ProjectId with the given UUID value.
         *
         * @param uuid the project id
         * @return ProjectId instance
         */
        public static ProjectId newProjectId(String uuid) {
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    /* package */ static class EventMessage {

        private static final ProjectId DUMMY_PROJECT_ID = AggregateId.newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreated(DUMMY_PROJECT_ID);
        private static final TaskAdded TASK_ADDED = taskAdded(DUMMY_PROJECT_ID);
        private static final ProjectStarted PROJECT_STARTED = projectStarted(DUMMY_PROJECT_ID);
        private static final Any PROJECT_CREATED_ANY = toAny(PROJECT_CREATED);
        private static final Any TASK_ADDED_ANY = toAny(TASK_ADDED);
        private static final Any PROJECT_STARTED_ANY = toAny(PROJECT_STARTED);

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

    /* package */ static class Command {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Command() {
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given command, userId and timestamp using default
         * {@link CommandId} instance.
         */
        public static org.spine3.base.Command create(Message command, UserId userId, Timestamp when) {
            final CommandContext context = createCommandContext(userId, Commands.generateId(), when);
            final org.spine3.base.Command result = Commands.create(command, context);
            return result;
        }

        /**
         * Creates a new {@link org.spine3.test.reflect.command.CreateProject} command with the generated project ID.
         */
        public static CreateProject createProject() {
            return CreateProject.newBuilder()
                                .setProjectId(AggregateId.newProjectId())
                                .build();
        }

        /**
         * Creates a new {@link CreateProject} command with the given project ID.
         */
        public static CreateProject createProject(ProjectId id) {
            return CreateProject.newBuilder()
                                .setProjectId(id)
                                .build();
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with default properties (current time etc).
         */
        public static org.spine3.base.Command createProjectCmd() {
            return createProjectCmd(getCurrentTime());
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given timestamp.
         */
        public static org.spine3.base.Command createProjectCmd(Timestamp when) {
            return createProjectCmd(USER_ID, PROJECT_ID, when);
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given userId, projectId and timestamp.
         */
        public static org.spine3.base.Command createProjectCmd(UserId userId, ProjectId projectId, Timestamp when) {
            final CreateProject command = createProject(projectId);
            return create(command, userId, when);
        }

        /**
         * Creates a new {@link org.spine3.base.Command}.
         */
        public static org.spine3.base.Command addTaskCmd() {
            return addTaskCmd(USER_ID, PROJECT_ID, getCurrentTime());
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given userId, projectId and timestamp.
         */
        public static org.spine3.base.Command addTaskCmd(UserId userId, ProjectId projectId, Timestamp when) {
            final AddTask command = addTask(projectId);
            return create(command, userId, when);
        }

        /**
         * Creates a new {@link org.spine3.base.Command}.
         */
        public static org.spine3.base.Command startProjectCmd() {
            return startProjectCmd(USER_ID, PROJECT_ID, getCurrentTime());
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given userId, projectId and timestamp.
         */
        public static org.spine3.base.Command startProjectCmd(UserId userId, ProjectId projectId, Timestamp when) {
            final StartProject command = startProject(projectId);
            return create(command, userId, when);
        }

        /**
         * Creates a new {@link AddTask} command with the given project ID.
         */
        public static AddTask addTask(ProjectId id) {
            return AddTask.newBuilder()
                          .setProjectId(id)
                          .build();
        }

        /**
         * Creates a new {@link StartProject} command with the given project ID.
         */
        public static StartProject startProject(ProjectId id) {
            return StartProject.newBuilder()
                               .setProjectId(id)
                               .build();
        }
    }

    /* package */ static class Event {

        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Event() {
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with default properties.
         */
        public static org.spine3.base.Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event projectCreated(ProjectId projectId) {
            return projectCreated(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectCreated(ProjectId projectId, EventContext eventContext) {
            final ProjectCreated eventMessage = EventMessage.projectCreated(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(eventMessage));
            return builder.build();
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event taskAdded(ProjectId projectId, EventContext eventContext) {
            final TaskAdded eventMessage = EventMessage.taskAdded(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(eventMessage));
            return builder.build();
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectStarted(ProjectId projectId, EventContext eventContext) {
            final ProjectStarted eventMessage = EventMessage.projectStarted(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(eventMessage));
            return builder.build();
        }
    }

    /* package */ static class AggregateStorageRecord {

        private AggregateStorageRecord() {
        }

        /**
         * Creates a new {@link org.spine3.server.storage.AggregateStorageRecord} with the given timestamp.
         */
        public static org.spine3.server.storage.AggregateStorageRecord create(Timestamp timestamp) {
            final org.spine3.server.storage.AggregateStorageRecord.Builder builder = org.spine3.server.storage.AggregateStorageRecord.newBuilder()
                                                                                                                                     .setTimestamp(timestamp);
            return builder.build();
        }

        /**
         * Creates a new {@link org.spine3.server.storage.AggregateStorageRecord} with the given timestamp and event record.
         */
        public static org.spine3.server.storage.AggregateStorageRecord create(Timestamp timestamp, org.spine3.base.Event event) {
            final org.spine3.server.storage.AggregateStorageRecord.Builder builder = create(timestamp)
                    .toBuilder()
                    .setEvent(event);
            return builder.build();
        }

        /**
         * Returns several records sorted by timestamp ascending.
         * First record's timestamp is current time.
         */
        public static List<org.spine3.server.storage.AggregateStorageRecord> createSequentialRecords(ProjectId id) {
            return createSequentialRecords(id, getCurrentTime());
        }

        /**
         * Returns several records sorted by timestamp ascending.
         *
         * @param timestamp1 the timestamp of first record.
         */
        public static List<org.spine3.server.storage.AggregateStorageRecord> createSequentialRecords(ProjectId id, Timestamp timestamp1) {
            final Duration delta = seconds(10);
            final Timestamp timestamp2 = add(timestamp1, delta);
            final Timestamp timestamp3 = add(timestamp2, delta);

            final org.spine3.server.storage.AggregateStorageRecord record1 = create(timestamp1,
                                                                                    Event.projectCreated(id, createEventContext(timestamp1)));
            final org.spine3.server.storage.AggregateStorageRecord record2 = create(timestamp2,
                                                                                    Event.taskAdded(id, createEventContext(timestamp2)));
            final org.spine3.server.storage.AggregateStorageRecord record3 = create(timestamp3,
                                                                                    Event.projectStarted(id, createEventContext(timestamp3)));

            return newArrayList(record1, record2, record3);
        }
    }

    /* package */ static class EventStorageRecord {

        private EventStorageRecord() {
        }

        public static org.spine3.server.storage.EventStorageRecord projectCreated() {
            final Timestamp time = getCurrentTime();
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.storage.EventStorageRecord.Builder builder = org.spine3.server.storage.EventStorageRecord.newBuilder()
                                                                                                                             .setMessage(EventMessage.projectCreatedAny())
                                                                                                                             .setTimestamp(time)
                                                                                                                             .setEventId("project_created")
                                                                                                                             .setEventType(TypeName.of(ProjectCreated.getDescriptor())
                                                                                                                                                   .value())
                                                                                                                             .setProducerId(projectId.getId())
                                                                                                                             .setContext(createEventContext(projectId, time));
            return builder.build();
        }

        public static org.spine3.server.storage.EventStorageRecord projectCreated(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.storage.EventStorageRecord.Builder result = org.spine3.server.storage.EventStorageRecord.newBuilder()
                                                                                                                            .setMessage(EventMessage.projectCreatedAny())
                                                                                                                            .setTimestamp(when)
                                                                                                                            .setEventId("project_created_" + when.getSeconds())
                                                                                                                            .setEventType(TypeName.of(ProjectCreated.getDescriptor())
                                                                                                                                                  .value())
                                                                                                                            .setProducerId(projectId.getId())
                                                                                                                            .setContext(createEventContext(projectId, when));
            return result.build();
        }

        public static org.spine3.server.storage.EventStorageRecord taskAdded(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.storage.EventStorageRecord.Builder result = org.spine3.server.storage.EventStorageRecord.newBuilder()
                                                                                                                            .setMessage(EventMessage.taskAddedAny())
                                                                                                                            .setTimestamp(when)
                                                                                                                            .setEventId("task_added_" + when.getSeconds())
                                                                                                                            .setEventType(TypeName.of(TaskAdded.getDescriptor())
                                                                                                                                                  .value())
                                                                                                                            .setProducerId(projectId.getId())
                                                                                                                            .setContext(createEventContext(projectId, when));
            return result.build();
        }

        public static org.spine3.server.storage.EventStorageRecord projectStarted(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final org.spine3.server.storage.EventStorageRecord.Builder result = org.spine3.server.storage.EventStorageRecord.newBuilder()
                                                                                                                            .setMessage(EventMessage.projectStartedAny())
                                                                                                                            .setTimestamp(when)
                                                                                                                            .setEventId("project_started_" + when.getSeconds())
                                                                                                                            .setEventType(TypeName.of(ProjectStarted.getDescriptor())
                                                                                                                                                  .value())
                                                                                                                            .setProducerId(projectId.getId())
                                                                                                                            .setContext(createEventContext(projectId, when));
            return result.build();
        }
    }
}
