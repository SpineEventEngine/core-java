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

import com.google.common.base.Function;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.UserId;
import io.spine.core.given.GivenUserId;
import io.spine.people.PersonName;
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.EventEnricher;
import io.spine.test.event.ProjectCompleted;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarred;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.user.permission.PermissionGrantedEvent;
import io.spine.test.event.user.permission.PermissionRevokedEvent;
import io.spine.test.event.user.sharing.SharingRequestApproved;
import io.spine.time.ZoneId;
import io.spine.time.ZoneOffset;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * @author Alexander Yevsyukov
 */
public class EventEnricherTestEnv {

    /** Prevents instantiation of this utility class. */
    private EventEnricherTestEnv() {}

    static ProjectId newProjectId() {
        final String uuid = newUuid();
        return ProjectId.newBuilder()
                        .setId(uuid)
                        .build();
    }

    public static class GivenEventMessage {

        private static final ProjectId PROJECT_ID = newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreated(PROJECT_ID);
        private static final ProjectStarted PROJECT_STARTED = projectStarted(PROJECT_ID);
        private static final ProjectCompleted PROJECT_COMPLETED = projectCompleted(PROJECT_ID);
        private static final ProjectStarred PROJECT_STARRED = projectStarred(PROJECT_ID);

        /** Prevents instantiation of this utility class. */
        private GivenEventMessage() {}

        public static ProjectCreated projectCreated() {
            return PROJECT_CREATED;
        }

        public static ProjectStarted projectStarted() {
            return PROJECT_STARTED;
        }

        public static ProjectCompleted projectCompleted() {
            return PROJECT_COMPLETED;
        }

        public static ProjectStarred projectStarred() {
            return PROJECT_STARRED;
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

        private static ProjectCompleted projectCompleted(ProjectId id) {
            return ProjectCompleted.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }

        private static ProjectStarred projectStarred(ProjectId id) {
            return ProjectStarred.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        static PermissionGrantedEvent permissionGranted() {
            return PermissionGrantedEvent.newBuilder()
                                         .setGranterUid(newUuid())
                                         .setPermissionId("mock_permission")
                                         .setUserUid(newUuid())
                                         .build();
        }

        static PermissionRevokedEvent permissionRevoked() {
            return PermissionRevokedEvent.newBuilder()
                                         .setPermissionId("old_permission")
                                         .setUserUid(newUuid())
                                         .build();
        }

        static SharingRequestApproved sharingRequestApproved() {
            return SharingRequestApproved.newBuilder()
                                         .setUserUid(newUuid())
                                         .build();
        }
    }

    public static class GivenEvent {

        private static final ProjectId PROJECT_ID = newProjectId();

        private GivenEvent() {}

        private static TestEventFactory eventFactory() {
            final TestEventFactory result =
                    TestEventFactory.newInstance(pack(PROJECT_ID), GivenEvent.class);
            return result;
        }

        public static Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        public static Event projectStarted() {
            final ProjectStarted msg = GivenEventMessage.projectStarted();
            final Event event = eventFactory().createEvent(msg);
            return event;
        }

        public static Event projectCreated(ProjectId projectId) {
            final ProjectCreated msg = GivenEventMessage.projectCreated(projectId);
            final Event event = eventFactory().createEvent(msg);
            return event;
        }

        public static Event permissionGranted() {
            final PermissionGrantedEvent message = GivenEventMessage.permissionGranted();
            final Event permissionGranted = createGenericEvent(message);
            return permissionGranted;
        }

        public static Event permissionRevoked() {
            final PermissionRevokedEvent message = GivenEventMessage.permissionRevoked();
            final Event permissionRevoked = createGenericEvent(message);
            return permissionRevoked;
        }

        public static Event sharingRequestApproved() {
            final SharingRequestApproved message = GivenEventMessage.sharingRequestApproved();
            final Event sharingRequestApproved = createGenericEvent(message);
            return sharingRequestApproved;
        }

        private static Event createGenericEvent(Message eventMessage) {
            final Any wrappedMessage = pack(eventMessage);
            final Event permissionRevoked = eventFactory().createEvent(wrappedMessage);
            return permissionRevoked;
        }
    }

    public static class Enrichment {

        private Enrichment() {}

        /** Creates a new enricher with all required enrichment functions set. */
        public static EventEnricher newEventEnricher() {
            final EventEnricher.Builder builder = EventEnricher.newBuilder();
            builder.add(ProjectId.class, String.class, new GetProjectName())
                   .add(ProjectId.class, UserId.class, new GetProjectOwnerId())
                   .add(EventId.class, String.class, EVENT_ID_TO_STRING)
                   .add(Timestamp.class, String.class, TIMESTAMP_TO_STRING)
                   .add(CommandContext.class, String.class, CMD_CONTEXT_TO_STRING)
                   .add(Any.class, String.class, ANY_TO_STRING)
                   .add(Integer.class, String.class, VERSION_TO_STRING)
                   .add(String.class, ZoneOffset.class, STRING_TO_ZONE_OFFSET)
                   .add(String.class, PersonName.class, STRING_TO_PERSON_NAME)
                   .add(String.class, Integer.class, STRING_TO_INT);
            return builder.build();
        }

        public static class GetProjectName implements Function<ProjectId, String> {
            @Nullable
            @Override
            public String apply(@Nullable ProjectId id) {
                checkNotNull(id);
                final String name = "prj_" + id.getId();
                return name;
            }
        }

        public static class GetProjectOwnerId implements Function<ProjectId, UserId> {
            @Nullable
            @Override
            public UserId apply(@Nullable ProjectId id) {
                checkNotNull(id);
                return GivenUserId.of("po_" + id.getId());
            }
        }

        private static final Function<EventId, String> EVENT_ID_TO_STRING =
                new Function<EventId, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable EventId input) {
                        checkNotNull(input);
                        return input.getValue();
                    }
                };

        private static final Function<Timestamp, String> TIMESTAMP_TO_STRING =
                new Function<Timestamp, String>() {
                    @Override
                    public String apply(@Nullable Timestamp input) {
                        checkNotNull(input);
                        return input.toString();
                    }
                };

        private static final Function<CommandContext, String> CMD_CONTEXT_TO_STRING =
                new Function<CommandContext, String>() {
                    @Override
                    public String apply(@Nullable CommandContext input) {
                        return checkNotNull(input).toString();
                    }
                };

        private static final Function<Any, String> ANY_TO_STRING =
                new Function<Any, String>() {
                    @Override
                    public String apply(@Nullable Any input) {
                        return checkNotNull(input).toString();
                    }
                };

        private static final Function<Integer, String> VERSION_TO_STRING =
                new Function<Integer, String>() {
                    @Override
                    public String apply(@Nullable Integer input) {
                        return checkNotNull(input).toString();
                    }
                };

        private static final Function<String, ZoneOffset> STRING_TO_ZONE_OFFSET =
                new Function<String, ZoneOffset>() {
                    @Nullable
                    @Override
                    public ZoneOffset apply(@Nullable String input) {
                        checkNotNull(input);
                        return ZoneOffset.newBuilder()
                                         .setId(ZoneId.newBuilder()
                                                      .setValue(input))
                                         .build();
                    }
                };

        private static final Function<String, PersonName> STRING_TO_PERSON_NAME =
                new Function<String, PersonName>() {
                    @Nullable
                    @Override
                    public PersonName apply(@Nullable String input) {
                        checkNotNull(input);
                        return PersonName.newBuilder()
                                         .setFamilyName(input)
                                         .build();
                    }
                };

        private static final Function<String, Integer> STRING_TO_INT =
                new Function<String, Integer>() {
                    @Override
                    public Integer apply(@Nullable String input) {
                        return 0;
                    }
                };
    }
}
