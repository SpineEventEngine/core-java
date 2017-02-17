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

import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Response;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandBus;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.testdata.Sample;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

/**
 * @author Illia Shepilov
 */
public class AggregatePartRepositoryShould {

    public static final StreamObserver<Response> RESPONSE_OBSERVER = new StreamObserver<Response>() {
        @Override
        public void onNext(Response value) {

        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onCompleted() {

        }
    };
    private BoundedContext boundedContext;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        boundedContext.register(
                new ProjectHeaderRepository(boundedContext, new ProjectRoot(boundedContext, id)));

    }

    @Test
    public void test() {
        final CommandBus commandBus = boundedContext.getCommandBus();

        final ProjectId.Builder projectId = ProjectId.newBuilder()
                                                     .setId(newUuid());
        final CreateProject createProject = CreateProject.newBuilder()
                                                         .setProjectId(projectId)
                                                         .build();
        final CommandContext commandContext = createCommandContext();
        final Command command = Commands.createCommand(createProject, commandContext);
        commandBus.post(command, RESPONSE_OBSERVER);
    }

     /*
       Test environment classes
     ***************************/

    private static class ProjectRoot extends AggregateRoot<ProjectId> {

        private ProjectRoot(BoundedContext boundedContext, ProjectId id) {
            super(boundedContext, id);
        }
    }

    @SuppressWarnings("TypeMayBeWeakened") // We need exact message classes, without OrBuilder.
    private static class ProjectHeader extends AggregatePart<ProjectId, Project, Project.Builder> {

        private ProjectHeader(ProjectId id, ProjectRoot root) {
            super(id, root);
        }

        @Assign
        public ProjectCreated handle(CreateProject msg, CommandContext context) {
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
        public TaskAdded handle(AddTask msg, CommandContext context) {
            return TaskAdded.newBuilder()
                            .setProjectId(msg.getProjectId())
                            .build();
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setId(event.getProjectId());
        }

        @Assign
        public ProjectStarted handle(StartProject msg, CommandContext context) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .build();
        }

        @Apply
        private void apply(ProjectStarted event) {
        }
    }

    private static class ProjectHeaderRepository extends AggregatePartRepository<ProjectId, ProjectHeader> {

        private ProjectHeaderRepository(BoundedContext boundedContext, ProjectRoot root) {
            super(boundedContext);
        }
    }
}
