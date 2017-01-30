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

import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.server.command.Assign;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;

import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Alexander Yevsyukov
 */
class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

    // Needs to be `static` to share the state updates in scope of the test.
    private static final Map<CommandId, Command> commandsHandled = newConcurrentMap();

    @SuppressWarnings("PublicConstructorInNonPublicClass")      // Required to be `public`.
    public ProjectAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    ProjectCreated handle(CreateProject msg, CommandContext context) {
        final Command cmd = Commands.create(msg, context);
        commandsHandled.put(context.getCommandId(), cmd);
        return ProjectCreated.newBuilder()
                             .setProjectId(msg.getProjectId())
                             .setName(msg.getName())
                             .build();
    }

    @Apply
    private void apply(ProjectCreated event) {
        getBuilder().setId(event.getProjectId())
                    .setName(event.getName());
    }

    @Assign
    TaskAdded handle(AddTask msg, CommandContext context) {
        final Command cmd = Commands.create(msg, context);
        commandsHandled.put(context.getCommandId(), cmd);
        return TaskAdded.newBuilder()
                        .setProjectId(msg.getProjectId())
                        .build();
    }

    @Apply
    private void apply(TaskAdded event) {
        getBuilder().setId(event.getProjectId());
    }

    @Assign
    ProjectStarted handle(StartProject msg, CommandContext context) {
        final Command cmd = Commands.create(msg, context);
        commandsHandled.put(context.getCommandId(), cmd);
        return ProjectStarted.newBuilder()
                             .setProjectId(msg.getProjectId())
                             .build();
    }

    @Apply
    private void apply(ProjectStarted event) {
    }

    static void assertHandled(Command expected) {
        final CommandId id = Commands.getId(expected);
        final Command actual = commandsHandled.get(id);
        final String cmdName = Commands.getMessage(expected)
                                       .getClass()
                                       .getName();
        assertNotNull("No such command handled: " + cmdName, actual);
        assertEquals(expected, actual);
    }

    static void clearCommandsHandled() {
        commandsHandled.clear();
    }
}
