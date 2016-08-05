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

package org.spine3.server.command;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.ProjectId;
import org.spine3.test.command.StartProject;
import org.spine3.users.UserId;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;


@SuppressWarnings("EmptyClass")
/*package */ class Given {

    /* package */ static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    /* package */ static class Command {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Command() {
        }

        /**
         * Creates a new {@link Command} with the given command message, userId and timestamp using default
         * {@link Command} instance.
         */
        public static org.spine3.base.Command create(Message command, UserId userId, Timestamp when) {
            final CommandContext context = createCommandContext(userId, Commands.generateId(), when);
            final org.spine3.base.Command result = Commands.create(command, context);
            return result;
        }

        public static org.spine3.base.Command addTask() {
            return addTask(USER_ID, PROJECT_ID, getCurrentTime());
        }

        public static org.spine3.base.Command addTask(UserId userId, ProjectId projectId, Timestamp when) {
            final AddTask command = CommandMessage.addTask(projectId);
            return create(command, userId, when);
        }

        /** Creates a new {@link Command} with default properties (current time etc). */
        public static org.spine3.base.Command createProject() {
            return createProject(getCurrentTime());
        }

        public static org.spine3.base.Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        public static org.spine3.base.Command createProject(Duration delay) {
            final org.spine3.base.Command cmd = Commands.create(CommandMessage.createProject(), createCommandContext(delay));
            return cmd;
        }

        public static org.spine3.base.Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
            final CreateProject command = CommandMessage.createProject(projectId);
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

    /* package */ static class CommandMessage {

        private CommandMessage() {
        }

        public static AddTask addTask(String projectId) {
            return AddTask.newBuilder()
                          .setProjectId(
                                  ProjectId.newBuilder()
                                           .setId(projectId)
                                           .build())
                          .build();
        }

        public static AddTask addTask(ProjectId id) {
            return AddTask.newBuilder()
                          .setProjectId(id)
                          .build();
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

        public static CreateProject createProject(String projectId) {
            return CreateProject.newBuilder()
                                .setProjectId(
                                        ProjectId.newBuilder()
                                                 .setId(projectId)
                                                 .build())
                                .build();
        }

        public static StartProject startProject(ProjectId id) {
            return StartProject.newBuilder()
                               .setProjectId(id)
                               .build();
        }
    }
}
