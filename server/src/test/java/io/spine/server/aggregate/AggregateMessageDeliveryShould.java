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
package io.spine.server.aggregate;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.grpc.StreamObservers;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.ReactingProject;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.ShardingStrategy;
import io.spine.server.delivery.UniformAcrossTargets;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.server.transport.memory.SynchronousInMemTransportFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggStartProject;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.startProject;
import static io.spine.server.model.ModelTests.clearModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class AggregateMessageDeliveryShould {

    @Test
    public void dispatch_commands_to_single_shard_in_multithreaded_env() throws
                                                                         Exception {
        dispatchCommandsInParallel(new SingleShardProjectRepository());
    }

    @Test
    public void dispatch_commands_to_several_shard_in_multithreaded_env() throws
                                                                         Exception {
        dispatchCommandsInParallel(new TripleShardProjectRepository());
    }

    private static void dispatchCommandsInParallel(AggregateRepository repository) throws Exception {
        clearModel();
        ReactingProject.clearStats();
        setShardingTransport(SynchronousInMemTransportFactory.newInstance());

        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        boundedContext.register(repository);

        final int totalThreads = 42;
        final int totalCommands = 400;
        final int numberOfShards = repository.getShardingStrategy()
                                             .getNumberOfShards();

        assertTrue(ReactingProject.getThreadToId()
                                  .isEmpty());

        final CommandBus commandBus = boundedContext.getCommandBus();
        final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        final ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();

        final Set<ProjectId> targets = newHashSet();
        for (int i = 0; i < totalCommands; i++) {
            final Command command = startProject();
            final AggStartProject message = AnyPacker.unpack(command.getMessage());
            targets.add(message.getProjectId());

            builder.add(new Callable<Object>() {
                @Override
                public Object call() {
                    commandBus.post(command, StreamObservers.<Ack>noOpObserver());
                    return 0;
                }
            });
        }

        final ImmutableList<Integer> groups =
                FluentIterable.from(targets)
                              .transform(
                                      new Function<ProjectId, Integer>() {
                                          @Override
                                          public Integer apply(@Nullable ProjectId input) {
                                              checkNotNull(input);
                                              return input.hashCode() % numberOfShards;
                                          }
                                      })
                              .toList();

        final List<Callable<Object>> commandPostingJobs = builder.build();
        executorService.invokeAll(commandPostingJobs);

        Thread.sleep(1500);

        final Map<Long, Collection<ProjectId>> whoProcessedWhat = ReactingProject.getThreadToId()
                                                                                 .asMap();
        final Collection<ProjectId> actualProjectIds = newHashSet(ReactingProject.getThreadToId()
                                                                                 .values());
        final Set<Long> actualThreads = whoProcessedWhat.keySet();

        assertEquals(numberOfShards, actualThreads.size());
        assertEquals(totalCommands, actualProjectIds.size());

        repository.close();
        boundedContext.close();
        setShardingTransport(InMemoryTransportFactory.newInstance());
    }

    private static class SingleShardProjectRepository
            extends AggregateRepository<ProjectId, ReactingProject> {
    }

    private static class TripleShardProjectRepository
            extends AggregateRepository<ProjectId, ReactingProject> {

        @Override
        public ShardingStrategy getShardingStrategy() {
            return UniformAcrossTargets.forNumber(3);
        }
    }

    private static void setShardingTransport(InMemoryTransportFactory transport) {
        final InProcessSharding newSharding = new InProcessSharding(transport);
        ServerEnvironment.getInstance()
                         .replaceSharding(newSharding);
    }
}

