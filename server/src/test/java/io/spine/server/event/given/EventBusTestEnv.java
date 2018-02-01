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

package io.spine.server.event.given;

import io.spine.core.Event;
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.EventStreamQuery;
import io.spine.test.event.ProjectArchived;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarted;

import static io.spine.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * Test environment classes for the {@code server.event} package.
 */
public class EventBusTestEnv {

    private static final ProjectId PROJECT_ID = ProjectId.newBuilder()
                                                         .setId(newUuid())
                                                         .build();

    /**
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    public static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    private EventBusTestEnv() {}

    private static class EventMessage {

        private static final ProjectStarted PROJECT_STARTED = projectStarted(PROJECT_ID);

        private EventMessage() {}

        private static ProjectStarted projectStarted() {
            return PROJECT_STARTED;
        }

        private static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        private static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        /**
         * Returns an event message with an unfilled required field.
         */
        private static ProjectArchived projectArchived(ProjectId id) {
            return ProjectArchived.newBuilder()
                                  .setProjectId(id)
                                  .build();
        }
    }

    public static class GivenEvent {

        private static final TestEventFactory factory =
                TestEventFactory.newInstance(pack(PROJECT_ID), GivenEvent.class);

        private GivenEvent() {}

        private static TestEventFactory eventFactory() {
            return factory;
        }

        public static Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        public static Event projectStarted() {
            final ProjectStarted msg = EventMessage.projectStarted();
            final Event event = eventFactory().createEvent(msg);
            return event;
        }

        public static Event projectCreated(ProjectId projectId) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId);
            final Event event = eventFactory().createEvent(msg);
            return event;
        }

        /**
         * Returns an event with an unfilled required field.
         */
        public static Event projectArchived() {
            final ProjectArchived msg = EventMessage.projectArchived(PROJECT_ID);
            final Event event = Event.newBuilder()
                                     .setMessage(pack(msg))
                                     .build();
            return event;
        }

    }
}
