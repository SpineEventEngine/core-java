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

package io.spine.server.bc.given;

import com.google.protobuf.Message;
import io.spine.base.EventId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.integration.IntegrationEvent;
import io.spine.server.integration.IntegrationEventContext;
import io.spine.test.bc.ProjectId;
import io.spine.test.bc.event.ProjectCreated;
import io.spine.test.bc.event.ProjectStarted;
import io.spine.test.bc.event.TaskAdded;

import static io.spine.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.time.Time.getCurrentTime;

public class Given {

    private Given() {}

    public static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    public static class EventMessage {

        private EventMessage() {
        }

        public static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static TaskAdded taskAdded(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }

        public static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    public static class AnIntegrationEvent {

        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();
        private static final String TEST_BC_NAME = "Test BC";

        private AnIntegrationEvent() {
        }

        public static IntegrationEvent projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        public static IntegrationEvent projectCreated(ProjectId projectId) {
            final IntegrationEventContext context = createIntegrationEventContext(projectId);
            return projectCreated(projectId, context);
        }

        public static IntegrationEvent projectCreated(ProjectId projectId,
                                                      IntegrationEventContext eventContext) {
            final ProjectCreated event = EventMessage.projectCreated(projectId);
            final IntegrationEvent.Builder builder =
                    IntegrationEvent.newBuilder()
                                    .setContext(eventContext)
                                    .setMessage(AnyPacker.pack(event));
            return builder.build();
        }

        public static IntegrationEventContext createIntegrationEventContext(Message aggregateId) {
            // An integration event ID may have a value which does not follow our internal
            // conventions. We simulate this in the initialization below
            final EventId eventId = EventId.newBuilder()
                                           .setValue("ieid-" + newUuid())
                                           .build();

            final IntegrationEventContext.Builder builder =
                    IntegrationEventContext.newBuilder()
                                           .setEventId(eventId)
                                           .setTimestamp(getCurrentTime())
                                           .setBoundedContextName(TEST_BC_NAME)
                                           .setProducerId(pack(aggregateId));
            return builder.build();
        }
    }
}
