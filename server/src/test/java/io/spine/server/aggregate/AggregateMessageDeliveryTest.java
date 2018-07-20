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
package io.spine.server.aggregate;

import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.DeliveryProject;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.SingleShardProjectRepository;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.TripleShardProjectRepository;
import io.spine.server.delivery.AbstractMessageDeliveryTest;
import io.spine.server.delivery.given.ParallelDispatcher;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.test.aggregate.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.cannotStartProject;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.projectCancelled;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.startProject;
import static io.spine.server.delivery.given.MessageDeliveryTestEnv.dispatchWaitTime;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("Aggregate message delivery should")
class AggregateMessageDeliveryTest extends AbstractMessageDeliveryTest {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        DeliveryProject.getStats()
                       .clear();
    }

    @Nested
    @DisplayName("in multithreaded env, dispatch commands")
    class DispatchCommands {

        @Test
        @DisplayName("to single shard")
        void toSingleShard() throws Exception {
            ParallelDispatcher<ProjectId, Command> dispatcher =
                    new ParallelDispatcher<ProjectId, Command>(42, 400, dispatchWaitTime()) {
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
                                   .post(command, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new SingleShardProjectRepository());
        }

        @Test
        @DisplayName("to several shards")
        void toSeveralShards() throws Exception {
            ParallelDispatcher<ProjectId, Command> dispatcher =
                    new ParallelDispatcher<ProjectId, Command>(23, 423, dispatchWaitTime()) {
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
                                   .post(command, StreamObservers.noOpObserver());
                        }
                    };
            dispatcher.dispatchMessagesTo(new TripleShardProjectRepository());
        }

    }

    @Nested
    @DisplayName("in multithreaded env, dispatch events")
    class DispatchEvents {

        @Test
        @DisplayName("to single shard")
        void toSingleShard() throws Exception {
            ParallelDispatcher<ProjectId, Event> dispatcher =
                    new ParallelDispatcher<ProjectId, Event>(130, 500, dispatchWaitTime()) {
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
                                   .post(event, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new SingleShardProjectRepository());
        }

        @Test
        @DisplayName("to several shards")
        void toSeveralShards() throws Exception {
            ParallelDispatcher<ProjectId, Event> dispatcher =
                    new ParallelDispatcher<ProjectId, Event>(190, 900, dispatchWaitTime()) {
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
                                   .post(event, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new TripleShardProjectRepository());
        }
    }

    @Nested
    @DisplayName("in multithreaded env, dispatch rejections")
    class DispatchRejections {

        @Test
        @DisplayName("to single shard")
        void toSingleShard() throws Exception {
            ParallelDispatcher<ProjectId, Rejection> dispatcher =
                    new ParallelDispatcher<ProjectId, Rejection>(36, 12, dispatchWaitTime()) {
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
                                   .post(rejection, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new SingleShardProjectRepository());
        }

        @Test
        @DisplayName("to several shards")
        void toSeveralShards() throws Exception {
            ParallelDispatcher<ProjectId, Rejection> dispatcher =
                    new ParallelDispatcher<ProjectId, Rejection>(40, 603, dispatchWaitTime()) {
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
                                   .post(rejection, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new TripleShardProjectRepository());
        }
    }
}
