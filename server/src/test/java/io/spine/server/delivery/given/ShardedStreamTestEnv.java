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
package io.spine.server.delivery.given;

import com.google.common.base.Function;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.command.Assign;
import io.spine.server.delivery.CommandShardedStream;
import io.spine.server.delivery.DeliveryTag;
import io.spine.server.delivery.ShardIndex;
import io.spine.server.delivery.ShardedStream;
import io.spine.server.delivery.ShardedStreamConsumer;
import io.spine.server.delivery.ShardingKey;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemorySubscriber;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.TaskId;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.validate.StringValueVBuilder;

import static io.spine.core.BoundedContextNames.newName;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Alex Tymchenko
 */
public class ShardedStreamTestEnv {

    private static final TransportFactory transportFactory = InMemoryTransportFactory.newInstance();

    private static final BoundedContextName tasksContextName = newName("Tasks context");
    private static final BoundedContextName projectsContextName = newName("Projects context");

    private static final AggregateClass<?> projectAggregateClass;
    private static final AggregateClass<?> taskAggregateClass;
    private static final ProjectAggregateRepository projectRepo;
    private static final ShardingKey projectsShardZeroKey;
    private static final ShardingKey projectsShardOneKey;
    private static final DeliveryTag<CommandEnvelope> commandsToProjects;
    private static final CommandShardedStream<ProjectId> projectsShardZero;
    private static final CommandShardedStream<ProjectId> anotherProjectsShardZero;
    private static final CommandShardedStream<ProjectId> projectsShardOne;
    private static final CommandShardedStream<ProjectId> anotherProjectsShardOne;
    private static final CommandShardedStream<TaskId> tasksShardZero;
    private static final CommandShardedStream<TaskId> tasksShardOne;

    static {
        projectAggregateClass = asAggregateClass(ProjectAggregate.class);
        taskAggregateClass = asAggregateClass(TaskAggregate.class);
        projectRepo = new ProjectAggregateRepository();
        projectsShardZeroKey = shardingKeyOf(projectAggregateClass, 0);
        projectsShardOneKey = shardingKeyOf(projectAggregateClass, 1);
        commandsToProjects = DeliveryTag.forCommandsOf(projectRepo);

        TaskAggregateRepository taskRepo = new TaskAggregateRepository();
        BoundedContextName tasksContextName = taskRepo.getBoundedContextName();
        ShardingKey tasksShardZeroKey = shardingKeyOf(taskAggregateClass, 0);
        ShardingKey tasksShardOneKey = shardingKeyOf(taskAggregateClass, 1);
        DeliveryTag<CommandEnvelope> commandsToTask = DeliveryTag.forCommandsOf(taskRepo);

        projectsShardZero = streamToProject(transportFactory,
                                            projectsContextName,
                                            projectsShardZeroKey,
                                            commandsToProjects);

        anotherProjectsShardZero = streamToProject(transportFactory,
                                                   projectsContextName,
                                                   projectsShardZeroKey,
                                                   commandsToProjects);

        projectsShardOne = streamToProject(transportFactory,
                                           projectsContextName,
                                           projectsShardOneKey,
                                           commandsToProjects);

        anotherProjectsShardOne = streamToProject(transportFactory,
                                                  projectsContextName,
                                                  projectsShardOneKey,
                                                  commandsToProjects);

        ShardedStreamConsumer<TaskId, CommandEnvelope> taskConsumer = dummyConsumer();
        tasksShardZero = streamToTask(tasksContextName,
                                      tasksShardZeroKey,
                                      commandsToTask,
                                      taskConsumer);
        tasksShardOne = streamToTask(tasksContextName,
                                     tasksShardOneKey,
                                     commandsToTask,
                                     taskConsumer);
    }

    /**
     * Prevents this test environment utility from initialization.
     */
    private ShardedStreamTestEnv() {
    }

    private static CommandShardedStream<TaskId>
    streamToTask(BoundedContextName contextName,
                 ShardingKey key,
                 DeliveryTag<CommandEnvelope> tag,
                 ShardedStreamConsumer<TaskId, CommandEnvelope> consumer) {
        return CommandShardedStream.<TaskId>newBuilder()
                .setBoundedContextName(contextName)
                .setKey(key)
                .setTag(tag)
                .setConsumer(consumer)
                .setTargetIdClass(TaskId.class)
                .build(transportFactory);
    }

    public static CommandShardedStream<ProjectId> projectsShardZero() {
        return projectsShardZero;
    }

    public static CommandShardedStream<ProjectId> anotherProjectsShardZero() {
        return anotherProjectsShardZero;
    }

    public static CommandShardedStream<ProjectId> projectsShardOne() {
        return projectsShardOne;
    }

    public static CommandShardedStream<ProjectId> anotherProjectsShardOne() {
        return anotherProjectsShardOne;
    }

    public static CommandShardedStream<TaskId> tasksShardZero() {
        return tasksShardZero;
    }

    public static CommandShardedStream<TaskId> tasksShardOne() {
        return tasksShardOne;
    }

    public static CommandShardedStream<ProjectId>
    streamToShardWithFactory(TransportFactory factory) {
        CommandShardedStream.Builder<ProjectId> builder = CommandShardedStream.newBuilder();
        ShardedStreamConsumer<ProjectId, CommandEnvelope> consumer = dummyConsumer();
        builder.setBoundedContextName(projectsContextName)
               .setKey(projectsShardZeroKey)
               .setTag(commandsToProjects)
               .setConsumer(consumer)
               .setTargetIdClass(ProjectId.class);
        CommandShardedStream<ProjectId> result = builder.build(factory);
        return result;
    }

    private static CommandShardedStream<ProjectId>
    streamToProject(TransportFactory factory, BoundedContextName contextName,
                    ShardingKey key, DeliveryTag<CommandEnvelope> tag) {
        CommandShardedStream.Builder<ProjectId> builder = CommandShardedStream.newBuilder();
        ShardedStreamConsumer<ProjectId, CommandEnvelope> consumer = dummyConsumer();
        return builder.setBoundedContextName(contextName)
                      .setKey(key)
                      .setTag(tag)
                      .setTargetIdClass(ProjectId.class)
                      .setConsumer(consumer)
                      .build(factory);
    }

    /**
     * Returns a stream consumer which does nothing.
     */
    @SuppressWarnings("ReturnOfNull")   // `null's are returned for simplicity
    private static <I> ShardedStreamConsumer<I, CommandEnvelope> dummyConsumer() {
        return new ShardedStreamConsumer<I, CommandEnvelope>() {

            @Override
            public DeliveryTag<CommandEnvelope> getTag() {
                return null;
            }

            @Override
            public void onNext(I targetId, CommandEnvelope envelope) {

            }

            @Override
            public ShardedStream<I, ?, CommandEnvelope> bindToTransport(
                    BoundedContextName name, ShardingKey key,
                    TransportFactory transportFactory) {
                return null;
            }
        };
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

    public static TransportFactory
    customFactory(Function<StreamObserver<ExternalMessage>, Void> observerCallback) {
        return new InMemoryTransportFactory() {
            @Override
            protected Subscriber newSubscriber(ChannelId channelId) {
                return new InMemorySubscriber(channelId) {
                    @Override
                    public void onMessage(ExternalMessage message) {
                        Iterable<StreamObserver<ExternalMessage>> observers = getObservers();
                        for (StreamObserver<ExternalMessage> observer : observers) {
                            observerCallback.apply(observer);
                        }
                    }
                };
            }
        };
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
            return projectsContextName;
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
            return tasksContextName;
        }
    }
}
