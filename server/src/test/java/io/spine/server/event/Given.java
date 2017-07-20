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

package io.spine.server.event;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.server.command.TestEventFactory;
import io.spine.test.event.ProjectCompleted;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarred;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.user.permission.PermissionGrantedEvent;
import io.spine.test.event.user.permission.PermissionRevokedEvent;
import io.spine.test.event.user.sharing.SharingRequestApproved;

import static io.spine.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * Test environment classes for the {@code server.event} package.
 */
public class Given {

    private Given() {}

    static ProjectId newProjectId() {
        final String uuid = newUuid();
        return ProjectId.newBuilder()
                        .setId(uuid)
                        .build();
    }

    public static class EventMessage {

        private static final ProjectId PROJECT_ID = newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreated(PROJECT_ID);
        private static final ProjectStarted PROJECT_STARTED = projectStarted(PROJECT_ID);
        private static final ProjectCompleted PROJECT_COMPLETED = projectCompleted(PROJECT_ID);
        private static final ProjectStarred PROJECT_STARRED = projectStarred(PROJECT_ID);

        private EventMessage() {}

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

        public static ProjectCompleted projectCompleted(ProjectId id) {
            return ProjectCompleted.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }

        public static ProjectStarred projectStarred(ProjectId id) {
            return ProjectStarred.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static PermissionGrantedEvent permissionGranted() {
            return PermissionGrantedEvent.newBuilder()
                                         .setGranterUid(newUuid())
                                         .setPermissionId("mock-permission")
                                         .setUserUid(newUuid())
                                         .build();
        }

        public static PermissionRevokedEvent permissionRevoked() {
            return PermissionRevokedEvent.newBuilder()
                                         .setPermissionId("old-permission")
                                         .setUserUid(newUuid())
                                         .build();
        }

        public static SharingRequestApproved sharingRequestApproved() {
            return SharingRequestApproved.newBuilder()
                                         .setUserUid(newUuid())
                                         .build();
        }
    }

    public static class AnEvent {

        private static final ProjectId PROJECT_ID = EventMessage.PROJECT_ID;

        private AnEvent() {}

        private static TestEventFactory eventFactory() {
            final TestEventFactory result = TestEventFactory.newInstance(pack(PROJECT_ID),
                                                                         AnEvent.class);
            return result;
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

        public static Event permissionGranted() {
            final PermissionGrantedEvent message = EventMessage.permissionGranted();
            final Event permissionGranted = createGenericEvent(message);
            return permissionGranted;
        }

        public static Event permissionRevoked() {
            final PermissionRevokedEvent message = EventMessage.permissionRevoked();
            final Event permissionRevoked = createGenericEvent(message);
            return permissionRevoked;
        }

        public static Event sharingRequestApproved() {
            final SharingRequestApproved message = EventMessage.sharingRequestApproved();
            final Event sharingReqquestApproved = createGenericEvent(message);
            return sharingReqquestApproved;
        }

        private static Event createGenericEvent(Message eventMessage) {
            final Any wrappedMessage = pack(eventMessage);
            final Event permissionRevoked = eventFactory().createEvent(wrappedMessage);
            return permissionRevoked;
        }
    }
}
