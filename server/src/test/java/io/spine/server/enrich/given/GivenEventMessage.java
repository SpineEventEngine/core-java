/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.enrich.given;

import io.spine.base.Identifier;
import io.spine.test.event.ProjectCompleted;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarred;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.user.permission.PermissionGrantedEvent;
import io.spine.test.event.user.permission.PermissionRevokedEvent;
import io.spine.test.event.user.sharing.SharingRequestApproved;

import static io.spine.base.Identifier.newUuid;

public class GivenEventMessage {

    private static final ProjectId PROJECT_ID = Identifier.generate(ProjectId.class);
    private static final ProjectCreated PROJECT_CREATED = projectCreated(PROJECT_ID);
    private static final ProjectStarted PROJECT_STARTED = projectStarted(PROJECT_ID);
    private static final ProjectCompleted PROJECT_COMPLETED = projectCompleted(PROJECT_ID);
    private static final ProjectStarred PROJECT_STARRED = projectStarred(PROJECT_ID);

    /** Prevents instantiation of this utility class. */
    private GivenEventMessage() {
    }

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

    static ProjectCreated projectCreated(ProjectId id) {
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
