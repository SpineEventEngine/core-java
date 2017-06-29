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

package io.spine.server.aggregate;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.core.CommandContext;
import io.spine.server.BoundedContext;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.TaskId;
import io.spine.test.aggregate.TaskVBuilder;
import io.spine.test.aggregate.command.AddTask;
import io.spine.test.aggregate.command.CreateProject;
import io.spine.test.aggregate.event.ProjectCreated;
import io.spine.test.aggregate.event.TaskAdded;
import org.junit.Before;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static io.spine.server.aggregate.AggregatePartRepositoryLookup.createLookup;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    private static class ProjectPart extends AggregatePart<ProjectId,
                                                           Project,
            ProjectVBuilder,
                                                           ProjectRoot> {
        private ProjectPart(ProjectRoot root) {
            super(root);
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
            super();
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

    private static class TaskAggregatePart extends AggregatePart<TaskId,
                                                                 Task,
                                                                 TaskVBuilder,
                                                                 TaskRoot> {
        private TaskAggregatePart(TaskRoot root) {
            super(root);
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
            super();
        }
    }
}
