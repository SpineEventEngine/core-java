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

package org.spine3.server.aggregate.given;

import com.google.protobuf.StringValue;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.server.aggregate.AggregatePartRepository;
import org.spine3.server.aggregate.AggregateRoot;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.test.aggregate.Task;
import org.spine3.test.aggregate.TaskValidatingBuilder;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.test.aggregate.user.User;
import org.spine3.test.aggregate.user.UserValidatingBuilder;
import org.spine3.validate.StringValueValidatingBuilder;

public class AggregatePartTestEnv {

    private static final String TASK_DESCRIPTION = "Description";

    private AggregatePartTestEnv() {}

    public static class AnAggregateRoot extends AggregateRoot<String> {
        public AnAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    public static class WrongAggregatePart extends AggregatePart<String,
            StringValue,
            StringValueValidatingBuilder,
            AnAggregateRoot> {
        @SuppressWarnings("ConstantConditions")
        // Supply a "wrong" parameters on purpose to cause the validation failure
        protected WrongAggregatePart() {
            super(null);
        }
    }

    public static class AnAggregatePart extends AggregatePart<String,
            User,
            UserValidatingBuilder,
            AnAggregateRoot> {

        public AnAggregatePart(AnAggregateRoot root) {
            super(root);
        }
    }

    public static class TaskPart
            extends AggregatePart<String, Task, TaskValidatingBuilder, AnAggregateRoot> {

        public TaskPart(AnAggregateRoot root) {
            super(root);
        }

        @Assign
        TaskAdded handle(AddTask msg) {
            final TaskAdded result = TaskAdded.newBuilder()
                                              .build();
            //This command can be empty since we use apply method to setup aggregate part.
            return result;
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setDescription(TASK_DESCRIPTION);
        }
    }

    public static class TaskDescriptionPart extends AggregatePart<String,
            StringValue,
            StringValueValidatingBuilder,
            AnAggregateRoot> {

        public TaskDescriptionPart(AnAggregateRoot root) {
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
        private void apply(TaskAdded event) {
            getBuilder().setValue("Description value");
        }
    }

    public static class TaskRepository
            extends AggregatePartRepository<String, TaskPart, AnAggregateRoot> {

        public TaskRepository() {
            super();
        }
    }

    public static class TaskDescriptionRepository
            extends AggregatePartRepository<String, TaskDescriptionPart, AnAggregateRoot> {

        public TaskDescriptionRepository() {
            super();
        }
    }
}
