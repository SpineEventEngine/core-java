/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
package io.spine.server.delivery.given;

import com.google.protobuf.StringValue;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateClass;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.delivery.CommandShardedStream;
import io.spine.server.delivery.DeliveryTag;
import io.spine.server.delivery.ShardIndex;
import io.spine.server.delivery.ShardedStream;
import io.spine.server.delivery.ShardingKey;
import io.spine.server.model.Model;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.TaskId;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.validate.StringValueVBuilder;

import static io.spine.server.BoundedContext.newName;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Alex Tymchenko
 */
public class ShardedStreamTestEnv {

    private static final BoundedContextName TASKS_CONTEXT = newName("Tasks context");
    private static final BoundedContextName PROJECTS_CONTEXT = newName("Projects context");

    private static final CommandShardedStream<ProjectId> streamZeroToProjects;
    private static final CommandShardedStream<ProjectId> anotherZeroToProjects;
    private static final CommandShardedStream<ProjectId> streamOneToProjects;
    private static final CommandShardedStream<ProjectId> anotherOneToProjects;
    private static final CommandShardedStream<TaskId> commandStreamToTasksZero;
    private static final CommandShardedStream<TaskId> commandStreamToTasksOne;

    static {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

        final Model model = Model.getInstance();
        final AggregateClass<?> projectAggregateClass =
                model.asAggregateClass(ProjectAggregate.class);

        final AggregateClass<?> taskAggregateClass =
                model.asAggregateClass(TaskAggregate.class);

        final ProjectAggregateRepository projectRepo = new ProjectAggregateRepository();
        final BoundedContextName projectsContextName = projectRepo.getBoundedContextName();
        final ShardingKey projectsShardZero = shardingKeyOf(projectAggregateClass, 0);
        final ShardingKey projectsShardOne = shardingKeyOf(projectAggregateClass, 1);
        final DeliveryTag<CommandEnvelope> commandsToProjects = DeliveryTag.forCommandsOf(
                projectRepo);

        final TaskAggregateRepository taskRepo = new TaskAggregateRepository();
        final BoundedContextName tasksContextName = taskRepo.getBoundedContextName();
        final ShardingKey tasksShardZero = shardingKeyOf(taskAggregateClass, 0);
        final ShardingKey tasksShardOne = shardingKeyOf(taskAggregateClass, 1);
        final DeliveryTag<CommandEnvelope> commandsToTask = DeliveryTag.forCommandsOf(taskRepo);

        streamZeroToProjects = streamToProject(transportFactory,
                                               projectsContextName,
                                               projectsShardZero,
                                               commandsToProjects);

        anotherZeroToProjects = streamToProject(transportFactory,
                                                projectsContextName,
                                                projectsShardZero,
                                                commandsToProjects);

        streamOneToProjects = streamToProject(transportFactory,
                                              projectsContextName,
                                              projectsShardOne,
                                              commandsToProjects);

        anotherOneToProjects = streamToProject(transportFactory,
                                               projectsContextName,
                                               projectsShardOne,
                                               commandsToProjects);

        commandStreamToTasksZero =
                CommandShardedStream.<TaskId>newBuilder().setBoundedContextName(tasksContextName)
                                                         .setKey(tasksShardZero)
                                                         .setTag(commandsToTask)
                                                         .setTargetIdClass(TaskId.class)
                                                         .build(transportFactory);

        commandStreamToTasksOne =
                CommandShardedStream.<TaskId>newBuilder().setBoundedContextName(tasksContextName)
                                                         .setKey(tasksShardOne)
                                                         .setTag(commandsToTask)
                                                         .setTargetIdClass(TaskId.class)
                                                         .build(transportFactory);
    }

    /**
     * Prevents this test environment utility from initialization.
     */
    private ShardedStreamTestEnv() {
    }

    public static CommandShardedStream<ProjectId> streamZeroToProjects() {
        return streamZeroToProjects;
    }

    public static CommandShardedStream<ProjectId> anotherZeroToProjects() {
        return anotherZeroToProjects;
    }

    public static CommandShardedStream<ProjectId> streamOneToProjects() {
        return streamOneToProjects;
    }

    public static CommandShardedStream<ProjectId> anotherOneToProjects() {
        return anotherOneToProjects;
    }

    public static CommandShardedStream<TaskId> commandStreamToTasksZero() {
        return commandStreamToTasksZero;
    }

    public static CommandShardedStream<TaskId> commandStreamToTasksOne() {
        return commandStreamToTasksOne;
    }

    private static CommandShardedStream<ProjectId>
    streamToProject(TransportFactory factory, BoundedContextName contextName,
                    ShardingKey key, DeliveryTag<CommandEnvelope> tag) {
        final CommandShardedStream.Builder<ProjectId> builder = CommandShardedStream.newBuilder();
        return builder.setBoundedContextName(contextName)
                      .setKey(key)
                      .setTag(tag)
                      .setTargetIdClass(ProjectId.class)
                      .build(factory);
    }

    private static ShardingKey shardingKeyOf(AggregateClass<?> taskAggregateClass, int shardIndex) {
        return new ShardingKey(taskAggregateClass,
                               shardIndexOf(shardIndex));
    }

    private static ShardIndex shardIndexOf(int index) {
        return ShardIndex.newBuilder()
                         .setIndex(index)
                         .build();
    }

    public static class ProjectAggregate
            extends Aggregate<ProjectId, StringValue, StringValueVBuilder> {

        protected ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        AggProjectStarted on(AggStartProject cmd) {
            return AggProjectStarted.getDefaultInstance();
        }

        @Apply
        private void on(AggProjectStarted event) {
            //Do nothing for this test.
        }
    }

    public static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {

        @Override
        public BoundedContextName getBoundedContextName() {
            return PROJECTS_CONTEXT;
        }
    }

    public static class TaskAggregate
            extends Aggregate<TaskId, StringValue, StringValueVBuilder> {

        protected TaskAggregate(TaskId id) {
            super(id);
        }

        @Assign
        AggTaskAdded on(AggAddTask cmd) {
            return AggTaskAdded.getDefaultInstance();
        }

        @Apply
        private void on(AggTaskAdded event) {
            //Do nothing for this test.
        }
    }

    public static class TaskAggregateRepository
            extends AggregateRepository<TaskId, TaskAggregate> {
        @Override
        public BoundedContextName getBoundedContextName() {
            return TASKS_CONTEXT;
        }
    }

    public static CommandShardedStream.AbstractBuilder builder() {
        return new ShardedStream.AbstractBuilder() {
            @Override
            protected ShardedStream createStream() {
                throw newIllegalStateException("Method mustn't be called " +
                                                       "in the `AbstractBuilder` tests");
            }
        };
    }
}
