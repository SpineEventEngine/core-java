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
package io.spine.server.procman;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.StringValue;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.ShardingStrategy;
import io.spine.server.delivery.UniformAcrossTargets;
import io.spine.server.event.EventBus;
import io.spine.server.procman.given.PmMessageDeliveryTestEnv.DeliveryPm;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.server.transport.memory.SynchronousInMemTransportFactory;
import io.spine.test.procman.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.server.model.ModelTests.clearModel;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.cannotStartProject;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.createProject;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.projectStarted;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.routeByProjectId;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class PmMessageDeliveryShould {

    @Before
    public void setUp() {
        clearModel();
        DeliveryPm.clearStats();
        setShardingTransport(SynchronousInMemTransportFactory.newInstance());
    }

    @After
    public void tearDown() {
        setShardingTransport(InMemoryTransportFactory.newInstance());
    }

    @Test
    public void dispatch_commands_to_single_shard_in_multithreaded_env() throws
                                                                         Exception {
        dispatchCommandsInParallel(new SingleShardPmRepository());
    }

    @Test
    public void dispatch_events_to_single_shard_in_multithreaded_env() throws
                                                                       Exception {
        dispatchEventsInParallel(new SingleShardPmRepository());
    }

    @Test
    public void dispatch_rejections_to_single_shard_in_multithreaded_env() throws
                                                                           Exception {
        dispatchRejectionsInParallel(new SingleShardPmRepository());
    }

    @Test
    public void dispatch_commands_to_several_shard_in_multithreaded_env() throws
                                                                          Exception {
        dispatchCommandsInParallel(new QuadrupleShardPmRepository());
    }

    @Test
    public void dispatch_events_to_several_shards_in_multithreaded_env() throws
                                                                         Exception {
        dispatchEventsInParallel(new QuadrupleShardPmRepository());
    }

    @Test
    public void dispatch_rejections_to_several_shards_in_multithreaded_env() throws
                                                                             Exception {
        dispatchRejectionsInParallel(new QuadrupleShardPmRepository());
    }

    private static void dispatchCommandsInParallel(ProcessManagerRepository repository) throws
                                                                                   Exception {

        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        boundedContext.register(repository);

        final int totalThreads = 42;
        final int totalCommands = 400;
        final int numberOfShards = repository.getShardingStrategy()
                                             .getNumberOfShards();

        assertTrue(DeliveryPm.getThreadToId()
                                  .isEmpty());

        final CommandBus commandBus = boundedContext.getCommandBus();
        final ExecutorService executorService = newFixedThreadPool(totalThreads);
        final ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();

        for (int i = 0; i < totalCommands; i++) {
            final Command command = createProject();

            builder.add(new Callable<Object>() {
                @Override
                public Object call() {
                    commandBus.post(command, StreamObservers.<Ack>noOpObserver());
                    return 0;
                }
            });
        }

        final List<Callable<Object>> commandPostingJobs = builder.build();
        executorService.invokeAll(commandPostingJobs);

        Thread.sleep(1500);

        verifyStats(totalCommands, numberOfShards);

        cleanUp(repository, boundedContext);
    }

    private static void dispatchEventsInParallel(ProcessManagerRepository repository) throws Exception {

        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        boundedContext.register(repository);

        final int totalThreads = 180;
        final int totalEvents = 800;
        final int numberOfShards = repository.getShardingStrategy()
                                             .getNumberOfShards();

        assertTrue(DeliveryPm.getThreadToId()
                                  .isEmpty());

        final EventBus eventBus = boundedContext.getEventBus();
        final ExecutorService executorService = newFixedThreadPool(totalThreads);
        final ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();

        for (int i = 0; i < totalEvents; i++) {
            final Event event = projectStarted();

            builder.add(new Callable<Object>() {
                @Override
                public Object call() {
                    eventBus.post(event, StreamObservers.<Ack>noOpObserver());
                    return 0;
                }
            });
        }

        final List<Callable<Object>> eventPostingJobs = builder.build();
        executorService.invokeAll(eventPostingJobs);

        Thread.sleep(2500);

        verifyStats(totalEvents, numberOfShards);

        cleanUp(repository, boundedContext);
    }

    private static void dispatchRejectionsInParallel(ProcessManagerRepository repository) throws
                                                                                     Exception {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        boundedContext.register(repository);

        final int totalThreads = 30;
        final int totalRejections = 600;
        final int numberOfShards = repository.getShardingStrategy()
                                             .getNumberOfShards();

        assertTrue(DeliveryPm.getThreadToId()
                                  .isEmpty());

        final RejectionBus rejectionBus = boundedContext.getRejectionBus();
        final ExecutorService executorService = newFixedThreadPool(totalThreads);
        final ImmutableList.Builder<Callable<Object>> builder = ImmutableList.builder();

        for (int i = 0; i < totalRejections; i++) {
            final Rejection rejection = cannotStartProject();

            builder.add(new Callable<Object>() {
                @Override
                public Object call() {
                    rejectionBus.post(rejection, StreamObservers.<Ack>noOpObserver());
                    return 0;
                }
            });
        }

        final List<Callable<Object>> eventPostingJobs = builder.build();
        executorService.invokeAll(eventPostingJobs);

        Thread.sleep(1200);

        verifyStats(totalRejections, numberOfShards);

        cleanUp(repository, boundedContext);
    }

    private static void verifyStats(int totalEvents, int numberOfShards) {
        final Map<Long, Collection<ProjectId>> whoProcessedWhat = DeliveryPm.getThreadToId()
                                                                            .asMap();
        final Collection<ProjectId> actualProjectIds = newHashSet(DeliveryPm.getThreadToId()
                                                                                 .values());
        final Set<Long> actualThreads = whoProcessedWhat.keySet();

        assertEquals(numberOfShards, actualThreads.size());
        assertEquals(totalEvents, actualProjectIds.size());
    }

    private static void cleanUp(ProcessManagerRepository repository,
                                BoundedContext boundedContext) throws Exception {
        repository.close();
        boundedContext.close();
    }

    private static class SingleShardPmRepository
            extends ProcessManagerRepository<ProjectId, DeliveryPm, StringValue> {
        SingleShardPmRepository() {
            getRejectionRouting().replaceDefault(routeByProjectId());
        }

    }

    private static class QuadrupleShardPmRepository
            extends ProcessManagerRepository<ProjectId, DeliveryPm, StringValue> {

        QuadrupleShardPmRepository() {
            getRejectionRouting().replaceDefault(routeByProjectId());
        }

        @Override
        public ShardingStrategy getShardingStrategy() {
            return UniformAcrossTargets.forNumber(4);
        }
    }

    private static void setShardingTransport(InMemoryTransportFactory transport) {
        final InProcessSharding newSharding = new InProcessSharding(transport);
        ServerEnvironment.getInstance()
                         .replaceSharding(newSharding);
    }
}
