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
package io.spine.server.projection;

import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.delivery.AbstractMessageDeliveryTest;
import io.spine.server.delivery.given.ParallelDispatcher;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.DeliveryProjection;
import io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.SingleShardProjectRepository;
import io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.TripleShardProjectRepository;
import io.spine.test.projection.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.delivery.given.MessageDeliveryTestEnv.dispatchWaitTime;
import static io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.projectCreated;

/**
 * @author Alex Tymchenko
 */
@DisplayName("ProjectionEventDelivery should")
class ProjectionEventDeliveryTest extends AbstractMessageDeliveryTest {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        DeliveryProjection.getStats()
                          .clear();
    }

    @Test
    @DisplayName("dispatch events to single shard in multithreaded env")
    void dispatchToSingleShard() throws Exception {
        ParallelDispatcher<ProjectId, Event> dispatcher =
                new ParallelDispatcher<ProjectId, Event>(
                        180, 819, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProjection.getStats();
                    }

                    @Override
                    protected Event newMessage() {
                        return projectCreated();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Event event) {
                        context.getEventBus()
                               .post(event, noOpObserver());
                    }
                };

        dispatcher.dispatchMessagesTo(new SingleShardProjectRepository());
    }

    @Test
    @DisplayName("dispatch events to several shards in multithreaded env")
    void dispatchToSeveralShards() throws Exception {
        ParallelDispatcher<ProjectId, Event> dispatcher =
                new ParallelDispatcher<ProjectId, Event>(
                        270, 1637, dispatchWaitTime()) {
                    @Override
                    protected ThreadStats<ProjectId> getStats() {
                        return DeliveryProjection.getStats();
                    }

                    @Override
                    protected Event newMessage() {
                        return projectCreated();
                    }

                    @Override
                    protected void postToBus(BoundedContext context, Event event) {
                        context.getEventBus()
                               .post(event, noOpObserver());
                    }
                };

        dispatcher.dispatchMessagesTo(new TripleShardProjectRepository());
    }
}
