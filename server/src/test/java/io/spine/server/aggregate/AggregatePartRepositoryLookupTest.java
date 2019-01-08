/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.AggregatePartRepositoryLookup.createLookup;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AggregatePartRepositoryLookup should")
class AggregatePartRepositoryLookupTest {

    private BoundedContext boundedContext;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        boundedContext.register(new ProjectPartRepository());
        boundedContext.register(new TaskAggregateRepository());
    }

    @Test
    @DisplayName("find repository")
    void findRepository() {
        AggregatePartRepositoryLookup lookup =
                createLookup(boundedContext, ProjectId.class, Project.class);

        AggregatePartRepository repository = lookup.find();
        assertNotNull(repository);
        assertTrue(repository instanceof ProjectPartRepository);
    }

    @Test
    @DisplayName("throw IllegalStateException if repository isn't found")
    void throwOnRepositoryNotFound() {
        AggregatePartRepositoryLookup lookup =
                createLookup(boundedContext, Timestamp.class, StringValue.class);
        assertThrows(IllegalStateException.class, lookup::find);
    }

    @Test
    @DisplayName("throw IllegalStateException if the repo found is not AggregatePartRepository")
    void throwOnWrongRepositoryTypeFound() {
        AggregatePartRepositoryLookup lookup =
                createLookup(boundedContext, TaskId.class, Task.class);
        assertThrows(IllegalStateException.class, lookup::find);
    }

    @Test
    @DisplayName("throw IllegalStateException if the ID class of the repo found does not match the ID class of its root")
    void throwOnIdClassNotMatching() {
        AggregatePartRepositoryLookup lookup =
                createLookup(boundedContext, TaskId.class, Project.class);
        assertThrows(IllegalStateException.class, lookup::find);
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
        AggProjectCreated handle(AggCreateProject msg, CommandContext context) {
            return AggProjectCreated.newBuilder()
                                    .setProjectId(msg.getProjectId())
                                    .setName(msg.getName())
                                    .build();
        }

        @Apply
        void apply(AggProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }
    }

    private static class ProjectPartRepository
            extends AggregatePartRepository<ProjectId, ProjectPart, ProjectRoot> {
        private ProjectPartRepository() {
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

    private static class TaskAggregatePart
            extends AggregatePart<TaskId, Task, TaskVBuilder, TaskRoot> {

        private TaskAggregatePart(TaskRoot root) {
            super(root);
        }

        @Assign
        AggTaskAdded handle(AggAddTask cmd) {
            return AggTaskAdded.newBuilder()
                               .setProjectId(cmd.getProjectId())
                               .build();
        }

        @Apply
        void apply(AggTaskAdded event) {
            Task task = event.getTask();
            getBuilder().setTitle(task.getTitle())
                        .setDescription(task.getDescription());
        }
    }

    private static class TaskAggregateRepository
            extends AggregateRepository<TaskId, TaskAggregatePart> {

        private TaskAggregateRepository() {
            super();
        }
    }
}
