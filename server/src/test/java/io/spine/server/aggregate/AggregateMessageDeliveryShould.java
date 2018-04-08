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

import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.DeliveryProject;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.SingleShardProjectRepository;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.TripleShardProjectRepository;
import io.spine.server.delivery.given.MessageDeliveryTestEnv.ParallelDispatcher;
import io.spine.server.delivery.given.MessageDeliveryTestEnv.ThreadStats;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.server.transport.memory.SynchronousInMemTransportFactory;
import io.spine.test.aggregate.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.cannotStartProject;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.projectCancelled;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.startProject;
import static io.spine.server.delivery.given.MessageDeliveryTestEnv.dispatchWaitTime;
import static io.spine.server.delivery.given.MessageDeliveryTestEnv.setShardingTransport;
import static io.spine.server.model.ModelTests.clearModel;

/**
 * @author Alex Tymchenko
 */
public class AggregateMessageDeliveryShould {

    @Before
    public void setUp() {
        clearModel();
        DeliveryProject.getStats()
                       .clear();
        setShardingTransport(SynchronousInMemTransportFactory.newInstance());
    }

    @After
    public void tearDown() {
        setShardingTransport(InMemoryTransportFactory.newInstance());
    }

    @Test
    public void dispatch_commands_to_single_shard_in_multithreaded_env() throws
                                                                         Exception {
        final ParallelDispatcher<ProjectId, Command> dispatcher =
                new ParallelDispatcher<ProjectId, Command>(
                        42, 400, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProject.getStats();
                    }

                    @Override
                    protected Command newMessage() {
                        return startProject();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Command command) {
                        context.getCommandBus()
                               .post(command, StreamObservers.<Ack>noOpObserver());
                    }
                };

        dispatcher.dispatchMessagesTo(new SingleShardProjectRepository());
    }

    @Test
    public void dispatch_events_to_single_shard_in_multithreaded_env() throws
                                                                       Exception {
        final ParallelDispatcher<ProjectId, Event> dispatcher =
                new ParallelDispatcher<ProjectId, Event>(
                        130, 500, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProject.getStats();
                    }

                    @Override
                    protected Event newMessage() {
                        return projectCancelled();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Event event) {
                        context.getEventBus()
                               .post(event, StreamObservers.<Ack>noOpObserver());
                    }
                };

        dispatcher.dispatchMessagesTo(new SingleShardProjectRepository());
    }

    @Test
    public void dispatch_rejections_to_single_shard_in_multithreaded_env() throws
                                                                           Exception {
        final ParallelDispatcher<ProjectId, Rejection> dispatcher =
                new ParallelDispatcher<ProjectId, Rejection>(
                        36, 12, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProject.getStats();
                    }

                    @Override
                    protected Rejection newMessage() {
                        return cannotStartProject();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Rejection rejection) {
                        context.getRejectionBus()
                               .post(rejection, StreamObservers.<Ack>noOpObserver());
                    }
                };

        dispatcher.dispatchMessagesTo(new SingleShardProjectRepository());
    }

    @Test
    public void dispatch_commands_to_several_shard_in_multithreaded_env() throws
                                                                          Exception {
        final ParallelDispatcher<ProjectId, Command> dispatcher =
                new ParallelDispatcher<ProjectId, Command>(
                        23, 423, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProject.getStats();
                    }

                    @Override
                    protected Command newMessage() {
                        return startProject();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Command command) {
                        context.getCommandBus()
                               .post(command, StreamObservers.<Ack>noOpObserver());
                    }
                };
        dispatcher.dispatchMessagesTo(new TripleShardProjectRepository());
    }

    @Test
    public void dispatch_events_to_several_shards_in_multithreaded_env() throws
                                                                         Exception {
        final ParallelDispatcher<ProjectId, Event> dispatcher =
                new ParallelDispatcher<ProjectId, Event>(
                        190, 900, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProject.getStats();
                    }

                    @Override
                    protected Event newMessage() {
                        return projectCancelled();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Event event) {
                        context.getEventBus()
                               .post(event, StreamObservers.<Ack>noOpObserver());
                    }
                };

        dispatcher.dispatchMessagesTo(new TripleShardProjectRepository());
    }

    @Test
    public void dispatch_rejections_to_several_shards_in_multithreaded_env() throws
                                                                             Exception {
        final ParallelDispatcher<ProjectId, Rejection> dispatcher =
                new ParallelDispatcher<ProjectId, Rejection>(
                        40, 603, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProject.getStats();
                    }

                    @Override
                    protected Rejection newMessage() {
                        return cannotStartProject();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Rejection rejection) {
                        context.getRejectionBus()
                               .post(rejection, StreamObservers.<Ack>noOpObserver());
                    }
                };

        dispatcher.dispatchMessagesTo(new TripleShardProjectRepository());
    }
}
