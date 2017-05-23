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
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.test.TestEventFactory;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.testdata.Sample;
import org.spine3.users.UserId;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.Timestamps.add;
import static org.spine3.base.Identifier.newUuid;
import static org.spine3.server.aggregate.Given.EventMessage.projectCreated;
import static org.spine3.server.aggregate.Given.EventMessage.taskAdded;
import static org.spine3.test.Tests.newUserId;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.time.Durations2.seconds;
import static org.spine3.time.Time.getCurrentTime;

class Given {

    private Given() {
    }

    static class EventMessage {

        private EventMessage() {
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

    static class ACommand {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = Sample.messageOfType(ProjectId.class);

        private ACommand() {
        }

        static Command createProject() {
            return createProject(getCurrentTime());
        }

        static Command createProject(ProjectId id) {
            return createProject(USER_ID, id, getCurrentTime());
        }

        static Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        static Command createProject(UserId userId,
                ProjectId projectId,
                Timestamp when) {
            final CreateProject command = CommandMessage.createProject(projectId);
            return create(command, userId, when);
        }

        static Command addTask(ProjectId id) {
            final AddTask command = CommandMessage.addTask(id);
            return create(command, USER_ID, getCurrentTime());
        }

        static Command startProject(ProjectId id) {
            final StartProject command = CommandMessage.startProject(id);
            return create(command, USER_ID, getCurrentTime());
        }

        /**
         * Creates a new {@link ACommand} with the given command message,
         * userId and timestamp using default
         * {@link org.spine3.base.CommandId CommandId} instance.
         */
        static Command create(Message command, UserId userId, Timestamp when) {
            final CommandContext context =
                    createCommandContext(userId, when);
            final Command result = Commands.createCommand(command, context);
            return result;
        }
    }

    static class CommandMessage {

        private CommandMessage() {
        }

        static CreateProject createProject(ProjectId id) {
            final CreateProject.Builder builder = CreateProject.newBuilder()
                                                               .setProjectId(id)
                                                               .setName(projectName(id));
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

    private static String projectName(ProjectId id) {
        return "Project_" + id.getId();
    }

    static class StorageRecord {

        private StorageRecord() {
        }

        static AggregateEventRecord create(Timestamp timestamp) {
            final AggregateEventRecord.Builder builder
                    = AggregateEventRecord.newBuilder()
                                          .setTimestamp(timestamp);
            return builder.build();
        }

        static AggregateEventRecord create(Timestamp timestamp, Event event) {
            final AggregateEventRecord.Builder builder = create(timestamp)
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
        static List<AggregateEventRecord> sequenceFor(ProjectId id) {
            return sequenceFor(id, getCurrentTime());
        }

        /**
         * Returns several records sorted by timestamp ascending.
         *
         * @param timestamp1 the timestamp of first record.
         */
        static List<AggregateEventRecord> sequenceFor(ProjectId id, Timestamp timestamp1) {
            final Duration delta = seconds(10);
            final Timestamp timestamp2 = add(timestamp1, delta);
            final Timestamp timestamp3 = add(timestamp2, delta);

            final TestEventFactory eventFactory = TestEventFactory.newInstance(Given.class);

            final Event e1 = eventFactory.createEvent(projectCreated(id, projectName(id)),
                                                      null,
                                                      timestamp1);
            final AggregateEventRecord record1 = StorageRecord.create(timestamp1, e1);

            final Event e2 = eventFactory.createEvent(taskAdded(id),
                                                      null,
                                                      timestamp2);
            final AggregateEventRecord record2 = StorageRecord.create(timestamp2, e2);

            final Event e3 = eventFactory.createEvent(EventMessage.projectStarted(id),
                                                      null,
                                                      timestamp3);
            final AggregateEventRecord record3 = StorageRecord.create(timestamp3, e3);

            return newArrayList(record1, record2, record3);
        }
    }
}
