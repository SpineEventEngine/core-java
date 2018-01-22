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

package io.spine.server.rejection.given;

import io.spine.change.StringChange;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Rejection;
import io.spine.core.Rejections;
import io.spine.core.TenantId;
import io.spine.server.rejection.RejectionBusShould;
import io.spine.test.rejection.ProjectId;
import io.spine.test.rejection.ProjectRejections;
import io.spine.test.rejection.command.RemoveOwner;
import io.spine.test.rejection.command.UpdateProjectName;
import io.spine.testdata.Sample;

import static io.spine.Identifier.newUuid;

public class Given {

    /** Prevents instantiation of this utility class. */
    private Given() {}

    public static Rejection invalidProjectNameRejection() {
        final ProjectId projectId = newProjectId();
        final ProjectRejections.InvalidProjectName invalidProjectName =
                ProjectRejections.InvalidProjectName.newBuilder()
                                                    .setProjectId(projectId)
                                                    .build();
        final StringChange nameChange = StringChange.newBuilder()
                                                    .setNewValue("Too short")
                                                    .build();
        final UpdateProjectName updateProjectName = UpdateProjectName.newBuilder()
                                                                     .setId(projectId)
                                                                     .setNameUpdate(nameChange)
                                                                     .build();

        final TenantId generatedTenantId = TenantId.newBuilder()
                                                   .setValue(newUuid())
                                                   .build();
        final TestActorRequestFactory factory =
                TestActorRequestFactory.newInstance(RejectionBusShould.class, generatedTenantId);
        final Command command = factory.createCommand(updateProjectName);
        return Rejections.createRejection(invalidProjectName, command);
    }

    public static Rejection missingOwnerRejection() {
        final ProjectId projectId = newProjectId();
        final ProjectRejections.MissingOwner msg = ProjectRejections.MissingOwner.newBuilder()
                                                                                 .setProjectId(projectId)
                                                                                 .build();
        final Command command = io.spine.server.commandbus.Given.ACommand.withMessage(
                Sample.messageOfType(RemoveOwner.class));
        return Rejections.createRejection(msg, command);
    }

    private static ProjectId newProjectId() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(newUuid())
                                             .build();
        return projectId;
    }
}
