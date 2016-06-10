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
import org.spine3.base.Event;
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
import static org.spine3.testdata.TestEventMessageFactory.projectCreatedEventAny;
import static org.spine3.testdata.TestEventMessageFactory.projectStartedEventAny;
import static org.spine3.testdata.TestEventMessageFactory.taskAddedEventAny;


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

    public static class EventMessage {

        private static final ProjectId DUMMY_PROJECT_ID = Given.AggregateId.newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreatedMsg(DUMMY_PROJECT_ID);
        private static final Any PROJECT_CREATED_ANY = toAny(PROJECT_CREATED);

        private EventMessage() {
        }

        public static ProjectCreated projectCreatedMsg() {
            return PROJECT_CREATED;
        }

        public static ProjectCreated projectCreatedMsg(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static TaskAdded taskAddedMsg(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }

        public static ProjectStarted projectStartedMsg(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static Any projectCreatedEventAny() {
            return PROJECT_CREATED_ANY;
        }
    }

    public static class Command {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Command() {
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given command, userId and timestamp using default
         * {@link CommandId} instance.
         */
        public static org.spine3.base.Command createCommand(Message command, UserId userId, Timestamp when) {
            final CommandContext context = createCommandContext(userId, Commands.generateId(), when);
            final org.spine3.base.Command result = Commands.create(command, context);
            return result;
        }

        /**
         * Creates a new {@link org.spine3.test.project.command.CreateProject} command with the generated project ID.
         */
        public static CreateProject createProjectMsg() {
            return CreateProject.newBuilder()
                                .setProjectId(AggregateId.newProjectId())
                                .build();
        }

        /**
         * Creates a new {@link CreateProject} command with the given project ID.
         */
        public static CreateProject createProjectMsg(ProjectId id) {
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
            final CreateProject command = createProjectMsg(projectId);
            return createCommand(command, userId, when);
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
            final AddTask command = addTaskMsg(projectId);
            return createCommand(command, userId, when);
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
            final StartProject command = startProjectMsg(projectId);
            return createCommand(command, userId, when);
        }

        /**
         * Creates a new {@link AddTask} command with the given project ID.
         */
        public static AddTask addTaskMsg(ProjectId id) {
            return AddTask.newBuilder()
                          .setProjectId(id)
                          .build();
        }

        /**
         * Creates a new {@link StartProject} command with the given project ID.
         */
        public static StartProject startProjectMsg(ProjectId id) {
            return StartProject.newBuilder()
                               .setProjectId(id)
                               .build();
        }
    }

    public static class Event {
        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Event() {
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with default properties.
         */
        public static org.spine3.base.Event projectCreatedEvent() {
            return projectCreatedEvent(PROJECT_ID);
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event projectCreatedEvent(ProjectId projectId) {
            return projectCreatedEvent(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectCreatedEvent(ProjectId projectId, EventContext eventContext) {
            final ProjectCreated eventMessage = Given.EventMessage.projectCreatedMsg(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(eventMessage));
            return builder.build();
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event taskAddedEvent(ProjectId projectId, EventContext eventContext) {
            final TaskAdded eventMessage = Given.EventMessage.taskAddedMsg(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(eventMessage));
            return builder.build();
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectStartedEvent(ProjectId projectId, EventContext eventContext) {
            final ProjectStarted eventMessage = Given.EventMessage.projectStartedMsg(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(eventMessage));
            return builder.build();
        }
    }

    public static class RecordStorage {

        private RecordStorage() {
        }

        /**
         * Creates a new {@link AggregateStorageRecord} with the given timestamp.
         */
        public static AggregateStorageRecord newAggregateStorageRecord(Timestamp timestamp) {
            final AggregateStorageRecord.Builder builder = AggregateStorageRecord.newBuilder()
                                                                                 .setTimestamp(timestamp);
            return builder.build();
        }

        /**
         * Creates a new {@link AggregateStorageRecord} with the given timestamp and event record.
         */
        public static AggregateStorageRecord newAggregateStorageRecord(Timestamp timestamp, org.spine3.base.Event event) {
            final AggregateStorageRecord.Builder builder = newAggregateStorageRecord(timestamp)
                    .toBuilder()
                    .setEvent(event);
            return builder.build();
        }

        /*
              Returns several records sorted by timestamp ascending.
              First record's timestamp is current time.
        **/
        public static List<AggregateStorageRecord> createSequentialRecords(ProjectId id) {
            return createSequentialRecords(id, getCurrentTime());
        }

        /**
         * Returns several records sorted by timestamp ascending.
         *
         * @param timestamp1 the timestamp of first record.
         */
        public static List<AggregateStorageRecord> createSequentialRecords(ProjectId id, Timestamp timestamp1) {
            final Duration delta = seconds(10);
            final Timestamp timestamp2 = add(timestamp1, delta);
            final Timestamp timestamp3 = add(timestamp2, delta);

            final AggregateStorageRecord record1 = newAggregateStorageRecord(timestamp1,
                                                                             Event.projectCreatedEvent(id, createEventContext(timestamp1)));
            final AggregateStorageRecord record2 = newAggregateStorageRecord(timestamp2,
                                                                             Event.taskAddedEvent(id, createEventContext(timestamp2)));
            final AggregateStorageRecord record3 = newAggregateStorageRecord(timestamp3,
                                                                             Event.projectStartedEvent(id, createEventContext(timestamp3)));

            return newArrayList(record1, record2, record3);
        }
    }

    public static class EventStorage {

        private EventStorage() {
        }

        public static EventStorageRecord projectCreated() {
            final Timestamp time = getCurrentTime();
            final ProjectId projectId = AggregateId.newProjectId();
            final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                                                                         .setMessage(projectCreatedEventAny())
                                                                         .setTimestamp(time)
                                                                         .setEventId("project_created")
                                                                         .setEventType(TypeName.of(org.spine3.test.project.event.ProjectCreated.getDescriptor())
                                                                                               .value())
                                                                         .setProducerId(projectId.getId())
                                                                         .setContext(createEventContext(projectId, time));
            return builder.build();
        }

        public static EventStorageRecord projectCreated(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                                                                        .setMessage(projectCreatedEventAny())
                                                                        .setTimestamp(when)
                                                                        .setEventId("project_created_" + when.getSeconds())
                                                                        .setEventType(TypeName.of(org.spine3.test.project.event.ProjectCreated.getDescriptor())
                                                                                              .value())
                                                                        .setProducerId(projectId.getId())
                                                                        .setContext(createEventContext(projectId, when));
            return result.build();
        }

        public static EventStorageRecord taskAdded(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                                                                        .setMessage(taskAddedEventAny())
                                                                        .setTimestamp(when)
                                                                        .setEventId("task_added_" + when.getSeconds())
                                                                        .setEventType(TypeName.of(org.spine3.test.project.event.TaskAdded.getDescriptor())
                                                                                              .value())
                                                                        .setProducerId(projectId.getId())
                                                                        .setContext(createEventContext(projectId, when));
            return result.build();
        }

        public static EventStorageRecord projectStarted(Timestamp when) {
            final ProjectId projectId = AggregateId.newProjectId();
            final EventStorageRecord.Builder result = EventStorageRecord.newBuilder()
                                                                        .setMessage(projectStartedEventAny())
                                                                        .setTimestamp(when)
                                                                        .setEventId("project_started_" + when.getSeconds())
                                                                        .setEventType(TypeName.of(org.spine3.test.project.event.ProjectStarted.getDescriptor())
                                                                                              .value())
                                                                        .setProducerId(projectId.getId())
                                                                        .setContext(createEventContext(projectId, when));
            return result.build();
        }
    }
}
