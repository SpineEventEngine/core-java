/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.integration;

import org.spine3.test.integration.ProjectId;
import org.spine3.test.integration.event.ProjectCreated;
import org.spine3.test.integration.event.TaskAdded;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestEventContextFactory.createIntegrationEventContext;


@SuppressWarnings("EmptyClass")
/* package */ class Given {

    /*package */ static class AggregateId {

        private AggregateId() {
        }

        /* package */ static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    /* package */ static class EventMessage {

        private EventMessage() {
        }

        /* package */ static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        /* package */ static TaskAdded taskAdded(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }
    }

    /* package */ static class IntegrationEvent {

        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private IntegrationEvent() {
        }

        /** Creates a new {@link IntegrationEvent} with default properties. */
        /* package */ static org.spine3.server.integration.IntegrationEvent projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        /* package */ static org.spine3.server.integration.IntegrationEvent projectCreated(ProjectId projectId) {
            final IntegrationEventContext context = createIntegrationEventContext(projectId);
            return projectCreated(projectId, context);
        }

        /* package */ static org.spine3.server.integration.IntegrationEvent projectCreated(
                ProjectId projectId,
                IntegrationEventContext eventContext) {
            final ProjectCreated event = EventMessage.projectCreated(projectId);
            final org.spine3.server.integration.IntegrationEvent.Builder builder =
                    org.spine3.server.integration.IntegrationEvent
                            .newBuilder()
                            .setContext(eventContext)
                            .setMessage(toAny(event));
            return builder.build();
        }


        /** Creates a new {@link IntegrationEvent} with default properties. */
        /* package */ static org.spine3.server.integration.IntegrationEvent taskAdded() {
            return taskAdded(PROJECT_ID);
        }

        /* package */ static org.spine3.server.integration.IntegrationEvent taskAdded(ProjectId projectId) {
            return taskAdded(projectId, createIntegrationEventContext(projectId));
        }

        /* package */ static org.spine3.server.integration.IntegrationEvent taskAdded(
                ProjectId projectId,
                IntegrationEventContext context) {
            final TaskAdded event = EventMessage.taskAdded(projectId);
            final org.spine3.server.integration.IntegrationEvent.Builder builder =
                    org.spine3.server.integration.IntegrationEvent
                            .newBuilder()
                            .setContext(context)
                            .setMessage(toAny(event));
            return builder.build();
        }
    }
}
