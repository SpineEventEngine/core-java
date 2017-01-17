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
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AggregateRootShould {

    private AggregateRoot aggregateRoot;

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setStorageFactory(InMemoryStorageFactory.getInstance())
                                                            .build();
        boundedContext.register(new ProjectHeaderRepository(boundedContext));

        final ProjectId id = Given.newProjectId();
        aggregateRoot = new ProjectRoot(boundedContext, id);
    }

    @Test
    public void return_part_state_by_class() {
        final Message part = aggregateRoot.getPart(Project.class);

        assertNotNull(part);
    }

    @Test
    public void cache_repositories() {
        final AggregateRoot rootSpy = spy(aggregateRoot);
        final Class<Project> partClass = Project.class;

        rootSpy.getPart(partClass);
        rootSpy.getPart(partClass);

        // It may be called once in another test. So here we check for atMost().
        verify(rootSpy, atMost(1)).lookup(partClass);
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

        private ProjectHeader(ProjectId id) {
            super(id);
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

        private ProjectHeaderRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
