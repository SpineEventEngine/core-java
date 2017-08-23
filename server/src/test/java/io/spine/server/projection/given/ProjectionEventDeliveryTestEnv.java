/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableMap;
import io.spine.Identifier;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.command.TestEventFactory;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionEventDelivery;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.event.PrjProjectCreated;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * @author Alex Tymchenko
 */
public class ProjectionEventDeliveryTestEnv {

    /** Prevents instantiation of this utility class. */
    private ProjectionEventDeliveryTestEnv() {}

    public static Event projectCreated() {
        final ProjectId projectId = projectId();
        final TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        pack(projectId),
                        ProjectionEventDeliveryTestEnv.class
                );

        final PrjProjectCreated msg = PrjProjectCreated.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build();

        final Event result = eventFactory.createEvent(msg);
        return result;
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(Identifier.newUuid())
                        .build();
    }

    public static class ProjectDetails extends Projection<ProjectId, Project, ProjectVBuilder> {

        private static PrjProjectCreated receivedEvent = null;

        protected ProjectDetails(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod") // It's fine in tests.
        @Subscribe
        public void on(PrjProjectCreated event) {
            receivedEvent = event;
        }

        @Nullable
        public static PrjProjectCreated getEventReceived() {
            return receivedEvent;
        }
    }

    /**
     * A repository which overrides the delivery strategy for events postponing their delivery
     * to projection instances.
     */
    public static class PostponingRepository
            extends ProjectionRepository<ProjectId, ProjectDetails, Project> {

        private PostponingEventDelivery eventDelivery = null;

        @Override
        public PostponingEventDelivery getEndpointDelivery() {
            if (eventDelivery == null) {
                eventDelivery = new PostponingEventDelivery(this);
            }
            return eventDelivery;
        }
    }

    /**
     * Event delivery strategy which always postpones the delivery, but remembers the event
     * along with the target entity ID.
     */
    public static class PostponingEventDelivery
            extends ProjectionEventDelivery<ProjectId, ProjectDetails> {

        private final Map<ProjectId, EventEnvelope> postponedEvents = newHashMap();

        protected PostponingEventDelivery(PostponingRepository repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(ProjectId id, EventEnvelope envelope) {
            postponedEvents.put(id, envelope);
            return true;
        }

        public Map<ProjectId, EventEnvelope> getPostponedEvents() {
            return ImmutableMap.copyOf(postponedEvents);
        }
    }
}
