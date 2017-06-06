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

import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHistory;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectValidatingBuilder;
import io.spine.test.aggregate.command.AddTask;
import io.spine.test.aggregate.command.CreateProject;
import io.spine.test.aggregate.command.StartProject;
import io.spine.test.aggregate.event.ProjectCreated;
import io.spine.test.aggregate.event.ProjectStarted;
import io.spine.test.aggregate.event.TaskAdded;

/**
 * @author Alexander Yevsyukov
 */
public class AggregateCommandEndpointTestEnv {

    private AggregateCommandEndpointTestEnv() {
        // Prevent instantiation of this utility class.
    }

    public static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {

        // Needs to be `static` to share the state updates in scope of the test.
        private static final CommandHistory commandsHandled = new CommandHistory();

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        public static void assertHandled(Command expected) {
            commandsHandled.assertHandled(expected);
        }

        public static void clearCommandsHandled() {
            commandsHandled.clear();
        }

        @Assign
        ProjectCreated handle(CreateProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
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
            commandsHandled.add(msg, context);
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
            commandsHandled.add(msg, context);
            return ProjectStarted.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .build();
        }

        @Apply
        private void apply(ProjectStarted event) {
        }
    }

    public static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {
    }
}
