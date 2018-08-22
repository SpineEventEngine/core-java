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
package io.spine.server.projection.given;

import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.core.Subscribe;
import io.spine.server.delivery.ShardingStrategy;
import io.spine.server.delivery.UniformAcrossTargets;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.testing.server.TestEventFactory;

import static io.spine.protobuf.AnyPacker.pack;

/**
 * @author Alex Tymchenko
 */
public class ProjectionEventDeliveryTestEnv {

    /** Prevents instantiation of this utility class. */
    private ProjectionEventDeliveryTestEnv() {
    }

    public static Event projectCreated() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        pack(projectId),
                        ProjectionEventDeliveryTestEnv.class
                );

        PrjProjectCreated msg = PrjProjectCreated.newBuilder()
                                                 .setProjectId(projectId)
                                                 .build();

        Event result = eventFactory.createEvent(msg);
        return result;
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(Identifier.newUuid())
                        .build();
    }

    /**
     * A projection class, which remembers the threads in which its handler methods were invoked.
     *
     * <p>Message handlers are invoked via reflection, so some of them are considered unused.
     *
     * <p>The subscriber parameters are not used, as they aren't needed for tests. They are still
     * present, as long as they are required according to the handler declaration rules.
     */
    @SuppressWarnings("unused")
    public static class DeliveryProjection extends Projection<ProjectId, Project, ProjectVBuilder> {

        private static final ThreadStats<ProjectId> stats = new ThreadStats<>();

        protected DeliveryProjection(ProjectId id) {
            super(id);
        }

        @Subscribe
        void on(PrjProjectCreated event) {
            stats.recordCallingThread(getId());
        }

        public static ThreadStats<ProjectId> getStats() {
            return stats;
        }
    }

    public static class SingleShardProjectRepository
            extends ProjectionRepository<ProjectId, DeliveryProjection, Project> {
    }

    public static class TripleShardProjectRepository
            extends ProjectionRepository<ProjectId, DeliveryProjection, Project> {

        @Override
        public ShardingStrategy getShardingStrategy() {
            return UniformAcrossTargets.forNumber(3);
        }
    }
}
