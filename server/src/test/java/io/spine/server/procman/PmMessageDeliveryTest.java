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
package io.spine.server.procman;

import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.delivery.AbstractMessageDeliveryTest;
import io.spine.server.delivery.given.ParallelDispatcher;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.server.procman.given.delivery.DeliveryPm;
import io.spine.server.procman.given.delivery.QuadrupleShardPmRepository;
import io.spine.server.procman.given.delivery.SingleShardPmRepository;
import io.spine.test.procman.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.delivery.given.MessageDeliveryTestEnv.dispatchWaitTime;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.createProject;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.projectStarted;
import static io.spine.server.procman.given.delivery.GivenMessage.cannotStartProject;
import static io.spine.server.procman.given.delivery.GivenMessage.createProject;
import static io.spine.server.procman.given.delivery.GivenMessage.projectStarted;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("ProcessManager message delivery in multithreaded env should")
class PmMessageDeliveryTest extends AbstractMessageDeliveryTest {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        DeliveryPm.getStats().clear();
    }

    @Nested
    @DisplayName("dispatch commands")
    class DispatchCommands {

        @Test
        @DisplayName("to single shard")
        void toSingleShard() throws Exception {
            ParallelDispatcher<ProjectId, Command> dispatcher =
                    new ParallelDispatcher<ProjectId, Command>(
                            42, 400, dispatchWaitTime()) {
                        @Override
                        protected ThreadStats<ProjectId> getStats() {
                            return DeliveryPm.getStats();
                        }

                        @Override
                        protected Command newMessage() {
                            return createProject();
                        }

                        @Override
                        protected void postToBus(BoundedContext context, Command command) {
                            context.getCommandBus()
                                   .post(command, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new SingleShardPmRepository());
        }

        @Test
        @DisplayName("to several shards")
        void toSeveralShards() throws Exception {
            ParallelDispatcher<ProjectId, Command> dispatcher =
                    new ParallelDispatcher<ProjectId, Command>(
                            59, 473, dispatchWaitTime()) {
                        @Override
                        protected ThreadStats<ProjectId> getStats() {
                            return DeliveryPm.getStats();
                        }

                        @Override
                        protected Command newMessage() {
                            return createProject();
                        }

                        @Override
                        protected void postToBus(BoundedContext context, Command command) {
                            context.getCommandBus()
                                   .post(command, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new QuadrupleShardPmRepository());
        }
    }

    @Nested
    @DisplayName("dispatch events")
    class DispatchEvents {

        @Test
        @DisplayName("to single shard")
        void toSingleShard() throws Exception {
            ParallelDispatcher<ProjectId, Event> dispatcher =
                    new ParallelDispatcher<ProjectId, Event>(
                            180, 819, dispatchWaitTime()) {
                        @Override
                        protected ThreadStats<ProjectId> getStats() {
                            return DeliveryPm.getStats();
                        }

                        @Override
                        protected Event newMessage() {
                            return projectStarted();
                        }

                        @Override
                        protected void postToBus(BoundedContext context, Event event) {
                            context.getEventBus()
                                   .post(event, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new SingleShardPmRepository());
        }

        @Test
        @DisplayName("to several shards")
        void toSeveralShards() throws Exception {
            ParallelDispatcher<ProjectId, Event> dispatcher =
                    new ParallelDispatcher<ProjectId, Event>(
                            179, 918, dispatchWaitTime()) {
                        @Override
                        protected ThreadStats<ProjectId> getStats() {
                            return DeliveryPm.getStats();
                        }

                        @Override
                        protected Event newMessage() {
                            return projectStarted();
                        }

                        @Override
                        protected void postToBus(BoundedContext context, Event event) {
                            context.getEventBus()
                                   .post(event, StreamObservers.noOpObserver());
                        }
                    };

            dispatcher.dispatchMessagesTo(new QuadrupleShardPmRepository());
        }
    }
}
