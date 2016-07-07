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

package org.spine3.server.event;

import com.google.common.base.Function;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.client.UserUtil;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.ProjectId;
import org.spine3.test.event.ProjectStarted;
import org.spine3.users.UserId;

import javax.annotation.Nullable;

import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

/**
 * Is for usage only in tests under the `event` package.
 */
@SuppressWarnings("EmptyClass")
public class Given {

    private Given() {}

    public static class AggregateId {

        private AggregateId() {}

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    public static class EventMessage {

        private static final ProjectId DUMMY_PROJECT_ID = AggregateId.newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreated(DUMMY_PROJECT_ID);
        private static final ProjectStarted PROJECT_STARTED = projectStarted(DUMMY_PROJECT_ID);

        private EventMessage() {}

        public static ProjectCreated projectCreated() {
            return PROJECT_CREATED;
        }

        public static ProjectStarted projectStarted() {
            return PROJECT_STARTED;
        }

        public static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    public static class Event {

        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Event() {}

        /** Creates a new event with default properties. */
        public static org.spine3.base.Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        /** Creates a new event with default properties. */
        public static org.spine3.base.Event projectStarted() {
            final ProjectStarted msg = EventMessage.projectStarted();
            final org.spine3.base.Event event = createEvent(msg, createEventContext(msg.getProjectId()));
            return event;
        }

        /** Creates a new event with the given projectId. */
        public static org.spine3.base.Event projectCreated(ProjectId projectId) {
            return projectCreated(projectId, createEventContext(projectId));
        }

        /** Creates a new event with the given projectId and eventContext. */
        public static org.spine3.base.Event projectCreated(ProjectId projectId, EventContext eventContext) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId);
            final org.spine3.base.Event event = createEvent(msg, eventContext);
            return event;
        }
    }

    public static class Enrichment {

        private Enrichment() {}

        /** Creates a new enricher with all required enrichment functions set. */
        public static EventEnricher newEventEnricher() {
            final EventEnricher.Builder builder = EventEnricher.newBuilder()
                    .addFieldEnrichment(ProjectId.class, String.class, new GetProjectName())
                    .addFieldEnrichment(ProjectId.class, UserId.class, new GetProjectOwnerId())
                    .addFieldEnrichment(EventId.class, String.class, EVENT_ID_TO_STRING)
                    .addFieldEnrichment(Timestamp.class, String.class, TIMESTAMP_TO_STRING)
                    .addFieldEnrichment(CommandContext.class, String.class, CMD_CONTEXT_TO_STRING)
                    .addFieldEnrichment(Any.class, String.class, ANY_TO_STRING)
                    .addFieldEnrichment(Integer.class, String.class, VERSION_TO_STRING)
                    .addFieldEnrichment(EventContext.Attributes.class, String.class, ATTRIBUTES_TO_STRING);
            return builder.build();
        }

        public static class GetProjectName implements Function<ProjectId, String> {
            @Nullable
            @Override
            public String apply(@Nullable ProjectId id) {
                if (id == null) {
                    return null;
                }
                final String name = "Project " + id.getId();
                return name;
            }
        }

        public static class GetProjectOwnerId implements Function<ProjectId, UserId> {
            @Nullable
            @Override
            public UserId apply(@Nullable ProjectId id) {
                if (id == null) {
                    return null;
                }
                return UserUtil.newUserId("Project owner " + id.getId());
            }
        }

        private static final Function<EventId, String> EVENT_ID_TO_STRING =
                new Function<EventId, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable EventId input) {
                        return input == null ? "" : input.getUuid();
                    }
                };

        private static final Function<Timestamp, String> TIMESTAMP_TO_STRING =
                new Function<Timestamp, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable Timestamp input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<CommandContext, String> CMD_CONTEXT_TO_STRING =
                new Function<CommandContext, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable CommandContext input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<Any, String> ANY_TO_STRING =
                new Function<Any, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable Any input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<Integer, String> VERSION_TO_STRING =
                new Function<Integer, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable Integer input) {
                        return input == null ? "" : input.toString();
                    }
                };

        private static final Function<EventContext.Attributes, String> ATTRIBUTES_TO_STRING =
                new Function<EventContext.Attributes, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable EventContext.Attributes input) {
                        return input == null ? "" : input.toString();
                    }
                };
    }
}
