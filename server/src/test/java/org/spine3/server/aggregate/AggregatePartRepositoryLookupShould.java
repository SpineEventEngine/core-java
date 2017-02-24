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

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.Task;
import org.spine3.test.aggregate.TaskId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.TaskAdded;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.server.aggregate.AggregatePartRepositoryLookup.createLookup;

public class AggregatePartRepositoryLookupShould {

    private BoundedContext boundedContext;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        final ProjectId id = ProjectId.newBuilder()
                                      .setId(newUuid())
                                      .build();
        final ProjectRoot root = new ProjectRoot(boundedContext, id);
        boundedContext.register(new ProjectPartRepository(boundedContext));
        boundedContext.register(new TaskAggregateRepository(boundedContext));
    }

    @Test
    public void find_a_repository() {
        AggregatePartRepositoryLookup lookup = createLookup(boundedContext,
                                                            ProjectId.class,
                                                            Project.class);

        final AggregatePartRepository repository = lookup.find();
        assertNotNull(repository);
        assertTrue(repository instanceof ProjectPartRepository);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_if_repository_is_not_AggregatePartRepository() {
        final AggregatePartRepositoryLookup lookup = createLookup(boundedContext,
                                                                  TaskId.class,
                                                                  Task.class);
        lookup.find();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_if_repository_not_found() {
        AggregatePartRepositoryLookup lookup = createLookup(boundedContext,
                                                            Timestamp.class,
                                                            StringValue.class);
        lookup.find();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_if_id_class_does_not_match() {
        AggregatePartRepositoryLookup lookup = createLookup(boundedContext,
                                                            TaskId.class,
                                                            Project.class);
        lookup.find();
    }

    /*
     * Test environment classes
     *****************************/

    private static class ProjectRoot extends AggregateRoot<ProjectId> {

        /**
         * Creates an new instance.
         *
         * @param boundedContext the bounded context to which the aggregate belongs
         * @param id             the ID of the aggregate
         */
        protected ProjectRoot(BoundedContext boundedContext, ProjectId id) {
            super(boundedContext, id);
        }
    }

    private static class ProjectPart extends AggregatePart<ProjectId, Project, Project.Builder> {
        private ProjectPart(ProjectId id, ProjectRoot root) {
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
    }

    private static class ProjectPartRepository
            extends AggregatePartRepository<ProjectId, ProjectPart, ProjectRoot> {
        private ProjectPartRepository(BoundedContext boundedContext) {
            super(boundedContext, ProjectRoot.class);
        }
    }

    private static class TaskRoot extends AggregateRoot<TaskId> {

        /**
         * Creates an new instance.
         *
         * @param boundedContext the bounded context to which the aggregate belongs
         * @param id             the ID of the aggregate
         */
        protected TaskRoot(BoundedContext boundedContext, TaskId id) {
            super(boundedContext, id);
        }
    }

    private static class TaskAggregatePart extends AggregatePart<TaskId, Task, Task.Builder> {
        private TaskAggregatePart(TaskId id, TaskRoot root) {
            super(id, root);
        }

        @Assign
        public TaskAdded handle(AddTask cmd) {
            return TaskAdded.newBuilder()
                            .setProjectId(cmd.getProjectId())
                            .build();
        }

        @Apply
        public void apply(TaskAdded event) {
            final Task task = event.getTask();
            getBuilder().setTitle(task.getTitle())
                        .setDescription(task.getDescription())
                        .build();
        }
    }

    private static class TaskAggregateRepository extends AggregateRepository<TaskId,
                                                                             TaskAggregatePart> {
        private TaskAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
