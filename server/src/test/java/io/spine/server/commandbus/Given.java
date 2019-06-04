/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.protobuf.Timestamp;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.test.commandbus.ProjectId;
import io.spine.test.commandbus.Task;
import io.spine.test.commandbus.TaskId;
import io.spine.test.commandbus.command.CmdBusAddTask;
import io.spine.test.commandbus.command.CmdBusCreateProject;
import io.spine.test.commandbus.command.CmdBusCreateTask;
import io.spine.test.commandbus.command.CmdBusRemoveTask;
import io.spine.test.commandbus.command.CmdBusStartProject;
import io.spine.test.commandbus.command.FirstCmdBusCreateProject;
import io.spine.test.commandbus.command.SecondCmdBusStartProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenCommandContext;
import io.spine.testing.core.given.GivenUserId;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.testing.TestValues.random;

public class Given {

    private Given() {
        // Prevent construction from outside.
    }

    private static ProjectId newProjectId() {
        String uuid = newUuid();
        return ProjectId
                .newBuilder()
                .setId(uuid)
                .build();
    }

    public static class ACommand {

        private static final UserId USER_ID = GivenUserId.newUuid();
        private static final ProjectId PROJECT_ID = newProjectId();
        private static final TaskId TASK_ID = newTaskId();

        private ACommand() {
            // Prevent construction from outside.
        }

        /**
         * Creates a new {@code ACommand} with the given command message,
         * serId and timestamp using default {@code ACommand} instance.
         */
        private static Command create(io.spine.base.CommandMessage command, UserId userId,
                                      Timestamp when) {
            TenantId generatedTenantId = TenantId
                    .newBuilder()
                    .setValue(newUuid())
                    .build();
            TestActorRequestFactory factory =
                    new TestActorRequestFactory(userId, generatedTenantId);
            Command result = factory.createCommand(command, when);
            return result;
        }

        public static Command withMessage(io.spine.base.CommandMessage message) {
            return create(message, USER_ID, currentTime());
        }

        public static Command createTask(boolean startTask) {
            return createTask(TASK_ID, USER_ID, startTask);
        }

        public static Command addTask() {
            return addTask(USER_ID, PROJECT_ID, currentTime());
        }

        static Command secondStartProject() {
            SecondCmdBusStartProject command = CommandMessage.secondStartProject(newProjectId());
            return create(command, USER_ID, currentTime());
        }

        static Command firstCreateProject() {
            FirstCmdBusCreateProject command = CommandMessage.firstCreateProject(newProjectId());
            return create(command, USER_ID, currentTime());
        }

        static Command createTask(TaskId taskId, UserId userId, boolean startTask) {
            CmdBusCreateTask command = CommandMessage.createTask(taskId, userId, startTask);
            return create(command, userId, currentTime());
        }

        static Command addTask(UserId userId, ProjectId projectId, Timestamp when) {
            CmdBusAddTask command = CommandMessage.addTask(projectId);
            return create(command, userId, when);
        }

        static Command removeTask() {
            CmdBusRemoveTask command = CommandMessage.removeTask(PROJECT_ID);
            return create(command, USER_ID, currentTime());
        }

        /** Creates a new {@code ACommand} with default properties (current time etc). */
        public static Command createProject() {
            return createProject(currentTime());
        }

        static Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        static Command createProject(Duration delay) {

            CmdBusCreateProject projectMessage = CommandMessage.createProjectMessage();
            CommandContext commandContext = GivenCommandContext.withScheduledDelayOf(delay);
            CommandFactory commandFactory = new TestActorRequestFactory(ACommand.class).command();
            Command cmd = commandFactory.createBasedOnContext(projectMessage, commandContext);
            return cmd;
        }

        static Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
            CmdBusCreateProject command = CommandMessage.createProjectMessage(projectId);
            return create(command, userId, when);
        }

        public static Command startProject() {
            return startProject(USER_ID, PROJECT_ID, currentTime());
        }

        static Command startProject(UserId userId, ProjectId projectId, Timestamp when) {
            CmdBusStartProject command = CommandMessage.startProject(projectId);
            return create(command, userId, when);
        }

        private static TaskId newTaskId() {
            int id = random(1, 100);
            return TaskId
                    .newBuilder()
                    .setId(id)
                    .build();
        }
    }

    public static class CommandMessage {

        private CommandMessage() {
            // Prevent construction from outside.
        }

        static CmdBusCreateTask createTask(TaskId taskId, UserId userId, boolean startTask) {
            Task task = Task
                    .newBuilder()
                    .setTaskId(taskId)
                    .setAssignee(userId)
                    .build();
            return CmdBusCreateTask
                    .newBuilder()
                    .setTaskId(taskId)
                    .setTask(task)
                    .setStart(startTask)
                    .build();
        }

        static CmdBusAddTask addTask(String projectId) {
            return addTask(ProjectId.newBuilder()
                                    .setId(projectId)
                                    .build());
        }

        static CmdBusAddTask addTask(ProjectId id) {
            return CmdBusAddTask.newBuilder()
                                .setProjectId(id)
                                .build();
        }

        static CmdBusRemoveTask removeTask(ProjectId projectId) {
            return CmdBusRemoveTask.newBuilder()
                                   .setProjectId(projectId)
                                   .build();
        }

        public static CmdBusCreateProject createProjectMessage() {
            return createProjectMessage(newProjectId());
        }

        static CmdBusCreateProject createProjectMessage(ProjectId id) {
            return CmdBusCreateProject
                    .newBuilder()
                    .setProjectId(id)
                    .build();
        }

        static CmdBusCreateProject createProjectMessage(String projectId) {
            return createProjectMessage(ProjectId.newBuilder()
                                                 .setId(projectId)
                                                 .build());
        }

        static CmdBusStartProject startProject(ProjectId id) {
            return CmdBusStartProject
                    .newBuilder()
                    .setProjectId(id)
                    .build();
        }

        static FirstCmdBusCreateProject firstCreateProject(ProjectId projectId) {
            return FirstCmdBusCreateProject
                    .newBuilder()
                    .setId(projectId)
                    .build();
        }

        static SecondCmdBusStartProject secondStartProject(ProjectId projectId) {
            return SecondCmdBusStartProject
                    .newBuilder()
                    .setId(projectId)
                    .build();
        }
    }
}
