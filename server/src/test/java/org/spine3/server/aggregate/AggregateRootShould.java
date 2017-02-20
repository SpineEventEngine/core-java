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

import com.google.protobuf.Message;
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
import org.spine3.test.aggregate.ProjectDefinition;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.ProjectLifeCycle;
import org.spine3.test.aggregate.Status;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

public class AggregateRootShould {

    private static final StreamObserver<Response> RESPONSE_OBSERVER = new MockStreamObserver();
    private ProjectRoot aggregateRoot;
    private CommandBus commandBus;
    private ProjectId projectId;
    private CommandContext commandContext;

    @Before
    public void setUp() {
        commandContext = createCommandContext();
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        projectId = ProjectId.newBuilder()
                             .setId(newUuid())
                             .build();
        aggregateRoot = new ProjectRoot(boundedContext, projectId);
        boundedContext.register(new ProjectDefinitionRepository(boundedContext));
        boundedContext.register(new ProjectLifeCycleRepository(boundedContext));

        commandBus = boundedContext.getCommandBus();
    }

    @Test
    public void return_part_state_by_class() {
        final Message definitionPart = aggregateRoot.getPartState(ProjectDefinition.class);
        assertNotNull(definitionPart);

        final Message lifeCyclePart = aggregateRoot.getPartState(ProjectLifeCycle.class);
        assertNotNull(lifeCyclePart);
    }

    @Test
    public void cache_repositories() {
        final AggregateRoot rootSpy = spy(aggregateRoot);
        final Class<ProjectDefinition> partClass = ProjectDefinition.class;

        rootSpy.getPartState(partClass);
        rootSpy.getPartState(partClass);

        // It may be called once in another test. So here we check for atMost().
        verify(rootSpy, atMost(1)).lookup(partClass);
    }

    @Test
    public void start_the_project() {
        final Command createProjectSmd = createProjectCmdInstance();
        commandBus.post(createProjectSmd, RESPONSE_OBSERVER);

        final Command startProjectCmd = createStartProjectCmd();
        commandBus.post(startProjectCmd, RESPONSE_OBSERVER);

        assertFalse(ProjectLifeCyclePart.exceptionOccurred);
    }

    @Test
    public void set_variable_to_true_when_it_is_trying_to_start_uncreated_project() {
        final Command startProjectCmd = createStartProjectCmd();
        commandBus.post(startProjectCmd, RESPONSE_OBSERVER);
        assertTrue(ProjectLifeCyclePart.exceptionOccurred);
    }

    private Command createProjectCmdInstance() {
        final CreateProject createProject = CreateProject.newBuilder()
                                                         .setProjectId(projectId)
                                                         .build();
        return Commands.createCommand(createProject, commandContext);
    }

    private Command createStartProjectCmd() {
        final StartProject startProject = StartProject.newBuilder()
                                                      .setProjectId(projectId)
                                                      .build();
        return Commands.createCommand(startProject, commandContext);
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
    private static class ProjectDefinitionPart
            extends AggregatePart<ProjectId, ProjectDefinition, ProjectDefinition.Builder> {

        private ProjectDefinitionPart(ProjectId id, ProjectRoot root) {
            super(id, root);
        }

        @Assign
        ProjectCreated handle(CreateProject msg) {
            final ProjectCreated result = ProjectCreated.newBuilder()
                                                        .setProjectId(msg.getProjectId())
                                                        .setName(msg.getName())
                                                        .build();
            return result;
        }

        @Apply
        private void apply(ProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod") // It is needed for testing.
    private static class ProjectLifeCyclePart
            extends AggregatePart<ProjectId, ProjectLifeCycle, ProjectLifeCycle.Builder> {

        private static boolean exceptionOccurred = false;

        /**
         * {@inheritDoc}
         *
         * @param id
         * @param root
         */
        protected ProjectLifeCyclePart(ProjectId id, ProjectRoot root) {
            super(id, root);
        }

        @Assign
        ProjectStarted handle(StartProject msg) {
            final ProjectDefinition projectDefinition = getPartState(ProjectDefinition.class);
            final boolean defaultId = projectDefinition.getId()
                                                       .equals(ProjectId.getDefaultInstance());
            if (defaultId) {
                exceptionOccurred = true;
            }
            final ProjectStarted result = ProjectStarted.newBuilder()
                                                        .setProjectId(msg.getProjectId())
                                                        .build();
            return result;
        }

        @Apply
        private void apply(ProjectStarted event) {
            getBuilder().setStatus(Status.STARTED);
        }
    }

    private static class ProjectDefinitionRepository
            extends AggregatePartRepository<ProjectId, ProjectDefinitionPart> {

        private ProjectDefinitionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectLifeCycleRepository
            extends AggregatePartRepository<ProjectId, ProjectLifeCyclePart> {

        private ProjectLifeCycleRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class MockStreamObserver implements StreamObserver<Response> {
        @Override
        public void onNext(Response value) {

        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onCompleted() {

        }
    }
}
