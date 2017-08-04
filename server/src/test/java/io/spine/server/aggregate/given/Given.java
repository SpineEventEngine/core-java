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

package io.spine.server.aggregate.given;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.UserId;
import io.spine.core.given.GivenUserId;
import io.spine.server.aggregate.AggregateEventRecord;
import io.spine.server.command.TestEventFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.testdata.Sample;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.taskAdded;
import static io.spine.server.command.TestEventFactory.newInstance;
import static io.spine.time.Durations2.seconds;
import static io.spine.time.Time.getCurrentTime;

public class Given {

    private Given() {
    }

    public static class EventMessage {

        private EventMessage() {
        }

        public static AggProjectCreated projectCreated(ProjectId id, String projectName) {
            return AggProjectCreated.newBuilder()
                                    .setProjectId(id)
                                    .setName(projectName)
                                    .build();
        }

        public static AggTaskAdded taskAdded(ProjectId id) {
            return AggTaskAdded.newBuilder()
                               .setProjectId(id)
                               .build();
        }

        public static AggProjectStarted projectStarted(ProjectId id) {
            return AggProjectStarted.newBuilder()
                                    .setProjectId(id)
                                    .build();
        }
    }

    public static class ACommand {

        private static final UserId USER_ID = GivenUserId.newUuid();
        private static final ProjectId PROJECT_ID = Sample.messageOfType(ProjectId.class);

        private ACommand() {
        }

        public static Command createProject() {
            return createProject(getCurrentTime());
        }

        public static Command createProject(ProjectId id) {
            return createProject(USER_ID, id, getCurrentTime());
        }

        public static Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        public static Command createProject(UserId userId,
                                            ProjectId projectId,
                                            Timestamp when) {
            final AggCreateProject command = CommandMessage.createProject(projectId);
            return create(command, userId, when);
        }

        public static Command addTask(ProjectId id) {
            final AggAddTask command = CommandMessage.addTask(id);
            return create(command, USER_ID, getCurrentTime());
        }

        public static Command startProject(ProjectId id) {
            final AggStartProject command = CommandMessage.startProject(id);
            return create(command, USER_ID, getCurrentTime());
        }

        /**
         * Creates a new {@link ACommand} with the given command message,
         * userId and timestamp using default
         * {@link io.spine.core.CommandId CommandId} instance.
         */
        public static Command create(Message command, UserId userId, Timestamp when) {
            final Command result = TestActorRequestFactory.newInstance(userId)
                                                          .createCommand(command, when);
            return result;
        }
    }

    public static class CommandMessage {

        private CommandMessage() {
        }

        public static AggCreateProject createProject(ProjectId id) {
            final AggCreateProject.Builder builder = AggCreateProject.newBuilder()
                                                               .setProjectId(id)
                                                               .setName(projectName(id));
            return builder.build();
        }

        public static AggAddTask addTask(ProjectId id) {
            final AggAddTask.Builder builder = AggAddTask.newBuilder()
                                                   .setProjectId(id);
            return builder.build();
        }

        public static AggStartProject startProject(ProjectId id) {
            final AggStartProject.Builder builder = AggStartProject.newBuilder()
                                                             .setProjectId(id);
            return builder.build();
        }
    }

    private static String projectName(ProjectId id) {
        return "Project_" + id.getId();
    }

    public static class StorageRecord {

        private static final TestEventFactory eventFactory = newInstance(Given.class);

        private StorageRecord() {
        }

        public static AggregateEventRecord create(Timestamp timestamp) {
            final Message eventMessage = Sample.messageOfType(AggProjectCreated.class);
            final Event event = eventFactory.createEvent(eventMessage);
            final AggregateEventRecord.Builder builder
                    = AggregateEventRecord.newBuilder()
                                          .setTimestamp(timestamp)
                                          .setEvent(event);
            return builder.build();
        }

        public static AggregateEventRecord create(Timestamp timestamp, Event event) {
            final AggregateEventRecord.Builder builder = create(timestamp)
                    .toBuilder()
                    .setEvent(event);
            return builder.build();
        }
    }

    public static class StorageRecords {

        private StorageRecords() {
        }

        /**
         * Returns several records sorted by timestamp ascending.
         * First record's timestamp is the current time.
         */
        public static List<AggregateEventRecord> sequenceFor(ProjectId id) {
            return sequenceFor(id, getCurrentTime());
        }

        /**
         * Returns several records sorted by timestamp ascending.
         *
         * @param timestamp1 the timestamp of first record.
         */
        public static List<AggregateEventRecord> sequenceFor(ProjectId id, Timestamp timestamp1) {
            final Duration delta = seconds(10);
            final Timestamp timestamp2 = add(timestamp1, delta);
            final Timestamp timestamp3 = add(timestamp2, delta);

            final TestEventFactory eventFactory = newInstance(Given.class);

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
