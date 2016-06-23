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

package org.spine3.server.projection;

import org.spine3.base.EventContext;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.test.projection.event.ProjectStarted;
import org.spine3.test.projection.event.TaskAdded;

import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;


@SuppressWarnings("EmptyClass")
/* package */ class Given {

    /* package */ static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    /* package */ static class EventMessage {

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

    /* package */ static class Event {

        private Event() {
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event projectCreated(ProjectId projectId) {
            return projectCreated(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectCreated(ProjectId projectId, EventContext context) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId);
            final org.spine3.base.Event event = createEvent(msg, context);
            return event;
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event taskAdded(ProjectId projectId) {
            return taskAdded(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event taskAdded(ProjectId projectId, EventContext context) {
            final TaskAdded msg = EventMessage.taskAdded(projectId);
            final org.spine3.base.Event event = createEvent(msg, context);
            return event;
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event projectStarted(ProjectId projectId) {
            return projectStarted(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectStarted(ProjectId projectId, EventContext context) {
            final ProjectStarted msg = EventMessage.projectStarted(projectId);
            final org.spine3.base.Event event = createEvent(msg, context);
            return event;
        }
    }
}
