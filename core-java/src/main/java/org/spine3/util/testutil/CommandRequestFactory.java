/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.util.testutil;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandRequest;
import org.spine3.base.UserId;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;

import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.util.Users.newUserId;
import static org.spine3.util.testutil.AggregateIdFactory.newProjectId;
import static org.spine3.util.testutil.ContextFactory.getCommandContext;

/**
 * The utility class which is used for creating CommandRequests for tests.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class CommandRequestFactory {

    private CommandRequestFactory() {
    }

    public static CommandRequest createProject() {
        return createProject(TimeUtil.getCurrentTime());
    }

    public static CommandRequest createProject(Timestamp when) {

        return createProject(newUserId("projectCreated"), newProjectId(), when);
    }

    public static CommandRequest createProject(UserId userId, ProjectId projectId, Timestamp when) {

        final CreateProject command = CreateProject.newBuilder().setProjectId(projectId).build();
        return createCommandRequest(command, userId, when);
    }

    public static CommandRequest addTask(UserId userId, ProjectId projectId, Timestamp when) {

        final AddTask command = AddTask.newBuilder().setProjectId(projectId).build();
        return createCommandRequest(command, userId, when);
    }

    public static CommandRequest startProject(UserId userId, ProjectId projectId, Timestamp when) {

        final StartProject command = StartProject.newBuilder().setProjectId(projectId).build();
        return createCommandRequest(command, userId, when);
    }

    public static CommandRequest createCommandRequest(Message command, UserId userId, Timestamp when) {

        final CommandContext context = getCommandContext(userId, when);
        final CommandRequest.Builder result = CommandRequest.newBuilder()
                .setContext(context)
                .setCommand(toAny(command));

        return result.build();
    }
}
