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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.test.aggregate.ProjectDefinition;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.ProjectLifecycle;
import org.spine3.test.aggregate.Status;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Identifiers.newUuid;

public class AggregateRootShould {

    private ProjectRoot aggregateRoot;
    private BoundedContext boundedContext;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        ProjectId projectId = ProjectId.newBuilder()
                                       .setId(newUuid())
                                       .build();
        aggregateRoot = new ProjectRoot(boundedContext, projectId);
        boundedContext.register(new ProjectDefinitionRepository(boundedContext));
        boundedContext.register(new ProjectLifeCycleRepository(boundedContext));
    }

    @Test
    public void pass_null_tolerance_test() throws NoSuchMethodException {
        final Constructor<AnAggregateRoot> ctor =
                AnAggregateRoot.class.getDeclaredConstructor(BoundedContext.class, String.class);
        new NullPointerTester()
                .setDefault(Constructor.class, ctor)
                .setDefault(BoundedContext.class, boundedContext)
                .testStaticMethods(AggregateRoot.class, NullPointerTester.Visibility.PACKAGE);
    }

    @SuppressWarnings("unchecked")
    // Supply a "wrong" value on purpose to cause the validation failure.
    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_aggregate_root_does_not_have_appropriate_constructor() {
        AggregateRoot.create(newUuid(), boundedContext, AggregateRoot.class);
    }

    @Test
    public void create_aggregate_root_entity() {
        final AnAggregateRoot aggregateRoot =
                AggregateRoot.create(newUuid(), boundedContext, AnAggregateRoot.class);
        assertNotNull(aggregateRoot);
    }

    @Test
    public void return_part_state_by_class() {
        final Message definitionPart = aggregateRoot.getPartState(ProjectDefinition.class);
        assertNotNull(definitionPart);

        final Message lifeCyclePart = aggregateRoot.getPartState(ProjectLifecycle.class);
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

    /*
       Test environment classes
     ***************************/

    private static class AnAggregateRoot extends AggregateRoot<String> {
        protected AnAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    private static class ProjectRoot extends AggregateRoot<ProjectId> {

        private ProjectRoot(BoundedContext boundedContext, ProjectId id) {
            super(boundedContext, id);
        }
    }

    @SuppressWarnings("TypeMayBeWeakened") // Exact message classes without OrBuilder are needed.
    private static class ProjectDefinitionPart extends AggregatePart<ProjectId,
            ProjectDefinition,
            ProjectDefinition.Builder,
            ProjectRoot> {

        private ProjectDefinitionPart(ProjectRoot root) {
            super(root);
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

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    // Static field in the instance method is used for the test simplification.
    private static class ProjectLifeCyclePart extends AggregatePart<ProjectId,
            ProjectLifecycle,
            ProjectLifecycle.Builder,
            ProjectRoot> {

        protected ProjectLifeCyclePart(ProjectRoot root) {
            super(root);
        }

        @Assign
        ProjectStarted handle(StartProject msg) {
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
            extends AggregatePartRepository<ProjectId, ProjectDefinitionPart, ProjectRoot> {

        private ProjectDefinitionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectLifeCycleRepository
            extends AggregatePartRepository<ProjectId, ProjectLifeCyclePart, ProjectRoot> {

        private ProjectLifeCycleRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
