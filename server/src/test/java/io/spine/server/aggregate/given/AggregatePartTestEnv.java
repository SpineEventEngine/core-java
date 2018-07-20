/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.StringValue;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregatePartRepository;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.TaskVBuilder;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.validate.StringValueVBuilder;

public class AggregatePartTestEnv {

    private static final String TASK_DESCRIPTION = "Description";

    private AggregatePartTestEnv() {
    }

    public static class AnAggregateRoot extends AggregateRoot<String> {
        public AnAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    public static class WrongAggregatePart extends AggregatePart<String,
            StringValue,
            StringValueVBuilder,
            AnAggregateRoot> {
        @SuppressWarnings("ConstantConditions")
        // Supply a "wrong" parameters on purpose to cause the validation failure
        protected WrongAggregatePart() {
            super(null);
        }
    }

    public static class AnAggregatePart extends AggregatePart<String,
            StringValue,
            StringValueVBuilder,
            AnAggregateRoot> {

        public AnAggregatePart(AnAggregateRoot root) {
            super(root);
        }
    }

    public static class TaskPart
            extends AggregatePart<String, Task, TaskVBuilder, AnAggregateRoot> {

        public TaskPart(AnAggregateRoot root) {
            super(root);
        }

        @Assign
        AggTaskAdded handle(AggAddTask msg) {
            AggTaskAdded result = AggTaskAdded.newBuilder()
                                              .build();
            //This command can be empty since we use apply method to setup aggregate part.
            return result;
        }

        @Apply
        void apply(AggTaskAdded event) {
            getBuilder().setDescription(TASK_DESCRIPTION);
        }
    }

    public static class TaskDescriptionPart extends AggregatePart<String,
            StringValue,
            StringValueVBuilder,
            AnAggregateRoot> {

        public TaskDescriptionPart(AnAggregateRoot root) {
            super(root);
        }

        @Assign
        AggProjectCreated handle(AggCreateProject msg) {
            AggProjectCreated result = AggProjectCreated.newBuilder()
                                                        .setProjectId(msg.getProjectId())
                                                        .setName(msg.getName())
                                                        .build();
            return result;
        }

        @Apply
        void apply(AggTaskAdded event) {
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
