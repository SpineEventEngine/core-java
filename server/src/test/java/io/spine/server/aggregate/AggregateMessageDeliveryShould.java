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

import com.google.common.collect.ImmutableList;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.ReactingProject;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.sharding.InProcessSharding;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.server.transport.memory.SynchronousInMemTransportFactory;
import io.spine.test.aggregate.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.startProject;
import static io.spine.server.model.ModelTests.clearModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class AggregateMessageDeliveryShould {

    private BoundedContext boundedContext;
    private ProjectRepository repository;

    @Before
    public void setUp() {
        clearModel();

        setShardingTransport(SynchronousInMemTransportFactory.newInstance());

        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new ProjectRepository();
        boundedContext.register(repository);
    }

    @After
    public void tearDown() throws Exception {
        repository.close();
        boundedContext.close();

        setShardingTransport(InMemoryTransportFactory.newInstance());
    }

    @Test
    public void dispatch_commands_to_single_shard_in_multithreaded_env() throws
                                                                         InterruptedException {
        final int totalThreads = 42;
        final int totalCommands = 400;
        assertTrue(ReactingProject.getThreadToId()
                                  .isEmpty());

        final CommandBus commandBus = boundedContext.getCommandBus();
        final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        final ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();
        for (int i = 0; i < totalCommands; i++) {
            builder.add(new Callable<Object>() {
                @Override
                public Object call() {
                    final Command command = startProject();
                    commandBus.post(command, StreamObservers.<Ack>noOpObserver());
                    return 0;
                }
            });
        }
        final List<Callable<Object>> commandPostingJobs = builder.build();
        executorService.invokeAll(commandPostingJobs);

        Thread.sleep(1500);

        final Map<Long, Collection<ProjectId>> whoProcessedWhat = ReactingProject.getThreadToId()
                                                                                 .asMap();
        final Collection<ProjectId> actualProjectIds = newHashSet(ReactingProject.getThreadToId()
                                                                                 .values());
        final Set<Long> actualThreads = whoProcessedWhat.keySet();

        assertEquals(1, actualThreads.size());
        assertEquals(totalCommands, actualProjectIds.size());
    }

    private static class ProjectRepository extends AggregateRepository<ProjectId, ReactingProject> {
    }

    private static void setShardingTransport(InMemoryTransportFactory transport) {
        final InProcessSharding newSharding = new InProcessSharding(transport);
        ServerEnvironment.getInstance()
                         .replaceSharding(newSharding);
    }
}

