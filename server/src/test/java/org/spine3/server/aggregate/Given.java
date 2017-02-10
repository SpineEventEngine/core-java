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

package org.spine3.server.aggregate;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.EventContext;
import org.spine3.base.Identifiers;
import org.spine3.server.aggregate.storage.AggregateStorageRecord;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.users.UserId;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.Timestamps.add;
import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.seconds;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.test.Tests.newUserId;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

class Given {

    private Given() {
    }

    static ProjectId newProjectId() {
        final String uuid = newUuid();
        return ProjectId.newBuilder()
                        .setId(uuid)
                        .build();
    }

    static class EventMessage {

        private EventMessage() {
        }

        static ProjectCreated projectCreated() {
            final ProjectId id = newProjectId();
            return projectCreated(id, newUuid());
        }

        static ProjectCreated projectCreated(ProjectId id, String projectName) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .setName(projectName)
                                 .build();
        }

        static TaskAdded taskAdded(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }

        static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    static class Event {

        private Event() {
        }

        static org.spine3.base.Event projectCreated() {
            return projectCreated(newProjectId());
        }

        static org.spine3.base.Event projectCreated(ProjectId projectId) {
            return projectCreated(projectId, createEventContext(projectId));
        }

        static org.spine3.base.Event projectCreated(ProjectId projectId, EventContext context) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId, newUuid());
            final org.spine3.base.Event event = createEvent(msg, context);
            return event;
        }

        static org.spine3.base.Event taskAdded(ProjectId projectId) {
            return taskAdded(projectId, createEventContext(projectId));
        }

        static org.spine3.base.Event taskAdded(ProjectId projectId, EventContext context) {
            final TaskAdded msg = EventMessage.taskAdded(projectId);
            final org.spine3.base.Event event = createEvent(msg, context);
            return event;
        }

        static org.spine3.base.Event projectStarted(ProjectId projectId, EventContext context) {
            final ProjectStarted msg = EventMessage.projectStarted(projectId);
            final org.spine3.base.Event event = createEvent(msg, context);
            return event;
        }
    }

    static class Command {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = newProjectId();

        private Command() {
        }

        static org.spine3.base.Command createProject() {
            return createProject(getCurrentTime());
        }

        static org.spine3.base.Command createProject(ProjectId id) {
            return createProject(USER_ID, id, getCurrentTime());
        }

        static org.spine3.base.Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        static org.spine3.base.Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
            final CreateProject command = CommandMessage.createProject(projectId);
            return create(command, userId, when);
        }

        static org.spine3.base.Command addTask(ProjectId id) {
            final AddTask command = CommandMessage.addTask(id);
            return create(command, USER_ID, getCurrentTime());
        }

        static org.spine3.base.Command startProject(ProjectId id) {
            final StartProject command = CommandMessage.startProject(id);
            return create(command, USER_ID, getCurrentTime());
        }

        /**
         * Creates a new {@link Command} with the given command message, userId and timestamp using default
         * {@link org.spine3.base.CommandId} instance.
         */
        static org.spine3.base.Command create(Message command, UserId userId, Timestamp when) {
            final CommandContext context = createCommandContext(userId, Commands.generateId(), when);
            final org.spine3.base.Command result = Commands.createCommand(command, context);
            return result;
        }
    }

    static class CommandMessage {

        private CommandMessage() {
        }

        static CreateProject createProject(ProjectId id) {
            final CreateProject.Builder builder = CreateProject.newBuilder()
                                                               .setProjectId(id)
                                                               .setName("Project_" + id.getId());
            return builder.build();
        }

        static AddTask addTask(ProjectId id) {
            final AddTask.Builder builder = AddTask.newBuilder()
                                                   .setProjectId(id);
            return builder.build();
        }

        static StartProject startProject(ProjectId id) {
            final StartProject.Builder builder = StartProject.newBuilder()
                                                             .setProjectId(id);
            return builder.build();
        }
    }

    static class StorageRecord {

        private StorageRecord() {
        }

        static AggregateStorageRecord create(Timestamp timestamp) {
            final AggregateStorageRecord.Builder builder
                    = AggregateStorageRecord.newBuilder()
                                            .setEventId(Identifiers.newUuid())
                                            .setTimestamp(timestamp);
            return builder.build();
        }

        static AggregateStorageRecord create(Timestamp timestamp, org.spine3.base.Event event) {
            final AggregateStorageRecord.Builder builder = create(timestamp)
                    .toBuilder()
                    .setEvent(event);
            return builder.build();
        }
    }

    static class StorageRecords {

        private StorageRecords() {
        }

        /**
         * Returns several records sorted by timestamp ascending.
         * First record's timestamp is the current time.
         */
        static List<AggregateStorageRecord> sequenceFor(ProjectId id) {
            return sequenceFor(id, getCurrentTime());
        }

        /**
         * Returns several records sorted by timestamp ascending.
         *
         * @param timestamp1 the timestamp of first record.
         */
        static List<AggregateStorageRecord> sequenceFor(ProjectId id, Timestamp timestamp1) {
            final Duration delta = seconds(10);
            final Timestamp timestamp2 = add(timestamp1, delta);
            final Timestamp timestamp3 = add(timestamp2, delta);

            final AggregateStorageRecord record1 = StorageRecord.create(timestamp1,
                                                                        Event.projectCreated(id, createEventContext(timestamp1)));
            final AggregateStorageRecord record2 = StorageRecord.create(timestamp2,
                                                                        Event.taskAdded(id, createEventContext(timestamp2)));
            final AggregateStorageRecord record3 = StorageRecord.create(timestamp3,
                                                                        Event.projectStarted(id, createEventContext(timestamp3)));

            return newArrayList(record1, record2, record3);
        }
    }
}
