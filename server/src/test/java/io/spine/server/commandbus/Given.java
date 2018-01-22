/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.commandbus;

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.client.CommandFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.core.given.GivenCommandContext;
import io.spine.core.given.GivenUserId;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.ProjectId;
import io.spine.test.command.CmdStartProject;

import static io.spine.Identifier.newUuid;
import static io.spine.time.Time.getCurrentTime;

public class Given {

    private Given() {
        // Prevent construction from outside.
    }

    static ProjectId newProjectId() {
        final String uuid = newUuid();
        return ProjectId.newBuilder()
                        .setId(uuid)
                        .build();
    }

    public static class ACommand {

        private static final UserId USER_ID = GivenUserId.newUuid();
        private static final ProjectId PROJECT_ID = newProjectId();

        private ACommand() {
            // Prevent construction from outside.
        }

        /**
         * Creates a new {@link ACommand} with the given command message,
         * serId and timestamp using default {@link ACommand} instance.
         */
        private static Command create(Message command, UserId userId,
                                      Timestamp when) {
            final TenantId generatedTenantId = TenantId.newBuilder()
                                                       .setValue(newUuid())
                                                       .build();
            final TestActorRequestFactory factory =
                    TestActorRequestFactory.newInstance(userId, generatedTenantId);
            final Command result = factory.createCommand(command, when);
            return result;
        }

        public static Command withMessage(Message message) {
            return create(message, USER_ID, getCurrentTime());
        }

        public static Command addTask() {
            return addTask(USER_ID, PROJECT_ID, getCurrentTime());
        }

        static Command addTask(UserId userId, ProjectId projectId, Timestamp when) {
            final CmdAddTask command = CommandMessage.addTask(projectId);
            return create(command, userId, when);
        }

        /** Creates a new {@link ACommand} with default properties (current time etc). */
        public static Command createProject() {
            return createProject(getCurrentTime());
        }

        static Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        static Command createProject(Duration delay) {

            final CmdCreateProject projectMessage = CommandMessage.createProjectMessage();
            final CommandContext commandContext = GivenCommandContext.withScheduledDelayOf(delay);
            final CommandFactory commandFactory =
                    TestActorRequestFactory.newInstance(ACommand.class)
                                           .command();
            final Command cmd = commandFactory.createBasedOnContext(projectMessage, commandContext);
            return cmd;
        }

        static Command createProject(UserId userId,
                                     ProjectId projectId,
                                     Timestamp when) {
            final CmdCreateProject command = CommandMessage.createProjectMessage(projectId);
            return create(command, userId, when);
        }

        public static Command startProject() {
            return startProject(USER_ID, PROJECT_ID, getCurrentTime());
        }

        static Command startProject(UserId userId,
                                    ProjectId projectId,
                                    Timestamp when) {
            final CmdStartProject command = CommandMessage.startProject(projectId);
            return create(command, userId, when);
        }
    }

    public static class CommandMessage {

        private CommandMessage() {
            // Prevent construction from outside.
        }

        static CmdAddTask addTask(String projectId) {
            return addTask(ProjectId.newBuilder()
                                    .setId(projectId)
                                    .build());
        }

        static CmdAddTask addTask(ProjectId id) {
            return CmdAddTask.newBuilder()
                             .setProjectId(id)
                             .build();
        }

        public static CmdCreateProject createProjectMessage() {
            return createProjectMessage(newProjectId());
        }

        static CmdCreateProject createProjectMessage(ProjectId id) {
            return CmdCreateProject.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }

        static CmdCreateProject createProjectMessage(String projectId) {
            return createProjectMessage(ProjectId.newBuilder()
                                                 .setId(projectId)
                                                 .build());
        }

        static CmdStartProject startProject(ProjectId id) {
            return CmdStartProject.newBuilder()
                                  .setProjectId(id)
                                  .build();
        }
    }
}
