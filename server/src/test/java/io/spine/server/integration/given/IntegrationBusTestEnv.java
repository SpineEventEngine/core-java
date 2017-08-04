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
package io.spine.server.integration.given;

import com.google.protobuf.StringValue;
import io.spine.Identifier;
import io.spine.core.Event;
import io.spine.core.Subscribe;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.EventSubscriber;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.integration.TransportFactory;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.integration.ProjectId;
import io.spine.test.integration.event.ProjectCreated;
import io.spine.validate.StringValueVBuilder;

import static io.spine.server.command.TestEventFactory.newInstance;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusTestEnv {

    public static BoundedContext contextWithExternalSubscriber(TransportFactory transportFactory) {
        final BoundedContext boundedContext = contextWithTransport(transportFactory);
        boundedContext.register(new ProjectDetailsRepository());
        return boundedContext;
    }

    public static BoundedContext contextWithTransport(TransportFactory transportFactory) {
        final IntegrationBus.Builder builder = IntegrationBus.newBuilder()
                                                             .setTransportFactory(transportFactory);
        final BoundedContext result = BoundedContext.newBuilder()
                                                    .setIntegrationBus(builder)
                                                    .build();
        return result;
    }

    public static Event projectCreated() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(Identifier.newUuid())
                                             .build();
        final TestEventFactory eventFactory = newInstance(AnyPacker.pack(projectId),
                                                          IntegrationBusTestEnv.class);
        return eventFactory.createEvent(ProjectCreated.newBuilder()
                                                      .setProjectId(projectId)
                                                      .build()
        );
    }

    public static class ProjectDetails
            extends Projection<ProjectId, StringValue, StringValueVBuilder> {

        private static ProjectCreated eventCaught = null;

        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        protected ProjectDetails(ProjectId id) {
            super(id);
        }

        @Subscribe(external = true)
        public void on(ProjectCreated event) {
            eventCaught = event;
        }

        public static ProjectCreated getEventCaught() {
            return eventCaught;
        }
    }

    public static class ProjectDetailsRepository
            extends ProjectionRepository<ProjectId, ProjectDetails, StringValue> {
    }

    public static class ExternalSubscriber extends EventSubscriber {

        private static ProjectCreated externalEvent = null;

        @Subscribe(external = true)
        void on(ProjectCreated msg) {
            externalEvent = msg;
        }

        public static ProjectCreated getExternalEvent() {
            return externalEvent;
        }
    }
}
