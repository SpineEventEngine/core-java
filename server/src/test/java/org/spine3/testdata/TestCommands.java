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

package org.spine3.testdata;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.UserId;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandScheduler;
import org.spine3.server.command.CommandStore;
import org.spine3.server.storage.StorageFactory;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.testdata.TestAggregateIdFactory.newProjectId;

/**
 * The utility class for creating the test data related to commands (command messages, Commands etc.).
 *
 * @author Mikhail Mikhaylov
 */
public class TestCommands {

    private static final UserId STUB_USER_ID = newUserId(newUuid());
    private static final ProjectId STUB_PROJECT_ID = newProjectId();

    private TestCommands() {
    }

    /**
     * Creates a new {@link Command} with default properties (current time etc).
     */
    public static Command createProject() {
        return createProject(TimeUtil.getCurrentTime());
    }

    /**
     * Creates a new {@link Command} with the given timestamp.
     */
    public static Command createProject(Timestamp when) {
        return createProject(STUB_USER_ID, STUB_PROJECT_ID, when);
    }

    /**
     * Creates a new {@link Command} with the given userId, projectId and timestamp.
     */
    public static Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
        final CreateProject command = createProject(projectId);
        return createCommand(command, userId, when);
    }

    /**
     * Creates a new {@link Command} with the given userId, projectId and timestamp.
     */
    public static Command addTask(UserId userId, ProjectId projectId, Timestamp when) {
        final AddTask command = addTask(projectId);
        return createCommand(command, userId, when);
    }

    /**
     * Creates a new {@link Command} with the given userId, projectId and timestamp.
     */
    public static Command startProject(UserId userId, ProjectId projectId, Timestamp when) {
        final StartProject command = startProject(projectId);
        return createCommand(command, userId, when);
    }

    /**
     * Creates a new {@link Command} with the given command, userId and timestamp using default
     * {@link CommandId} instance.
     */
    public static Command createCommand(Message command, UserId userId, Timestamp when) {
        final CommandContext context = TestContextFactory.createCommandContext(userId, Commands.generateId(), when);
        final Command result = Commands.create(command, context);
        return result;
    }

    /**
     * Creates a new {@link CreateProject} command with the given project ID.
     */
    public static CreateProject createProject(ProjectId id) {
        return CreateProject.newBuilder().setProjectId(id).build();
    }

    /**
     * Creates {@link CreateProject} command for the passed project ID.
     */
    public static CreateProject createProject(String projectId) {
        return CreateProject.newBuilder()
                .setProjectId(
                        ProjectId.newBuilder()
                                 .setId(projectId)
                                 .build())
                .build();
    }

    /**
     * Creates a new {@link AddTask} command with the given project ID.
     */
    public static AddTask addTask(ProjectId id) {
        return AddTask.newBuilder().setProjectId(id).build();
    }

    /**
     * Creates a new {@link AddTask} command with the given project ID.
     */
    public static AddTask addTask(String projectId) {
        return AddTask.newBuilder().setProjectId(
                ProjectId.newBuilder()
                        .setId(projectId)
                        .build())
                .build();
    }

    /**
     * Creates a new {@link StartProject} command with the given project ID.
     */
    public static StartProject startProject(ProjectId id) {
        return StartProject.newBuilder().setProjectId(id).build();
    }

    /**
     * Creates {@link StartProject} command for the passed project ID.
     */
    public static StartProject startProject(String projectId) {
        return StartProject.newBuilder()
                .setProjectId(ProjectId.newBuilder()
                                       .setId(projectId)
                                       .build())
                .build();
    }

    /**
     * Creates a new command bus with the given storage factory.
     */
    public static CommandBus newCommandBus(StorageFactory storageFactory) {
        final CommandStore store = new CommandStore(storageFactory.createCommandStorage());
        final CommandBus commandBus = CommandBus.newBuilder()
                .setCommandStore(store)
                .build();
        return commandBus;
    }

    /**
     * Creates a new command bus with the given command store and scheduler.
     */
    public static CommandBus newCommandBus(CommandStore store, CommandScheduler scheduler) {
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(store)
                                                .setScheduler(scheduler)
                                                .build();
        return commandBus;
    }
}
