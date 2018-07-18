/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.change.StringChange;
import io.spine.core.Command;
import io.spine.core.Rejection;
import io.spine.core.Rejections;
import io.spine.core.TenantId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.rejection.StandardRejections.CannotModifyDeletedEntity;
import io.spine.server.rejection.RejectionBusTest;
import io.spine.test.rejection.ProjectId;
import io.spine.test.rejection.ProjectRejections;
import io.spine.test.rejection.command.RjRemoveOwner;
import io.spine.test.rejection.command.RjUpdateProjectName;
import io.spine.test.rejection.command.RjUpdateProjectNameVBuilder;
import io.spine.testdata.Sample;
import io.spine.testing.client.TestActorRequestFactory;

import static io.spine.base.Identifier.newUuid;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Grankin
 */
public class Given {

    /** Prevents instantiation of this utility class. */
    private Given() {
    }

    public static Rejection invalidProjectNameRejection() {
        ProjectId projectId = newProjectId();
        ProjectRejections.InvalidProjectName invalidProjectName =
                ProjectRejections.InvalidProjectName.newBuilder()
                                                    .setProjectId(projectId)
                                                    .build();
        StringChange nameChange = StringChange.newBuilder()
                                                    .setNewValue("Too short")
                                                    .build();
        RjUpdateProjectName updateProjectName =
                RjUpdateProjectNameVBuilder.newBuilder()
                                           .setId(projectId)
                                           .setNameUpdate(nameChange)
                                           .build();
        TenantId generatedTenantId = TenantId.newBuilder()
                                                   .setValue(newUuid())
                                                   .build();
        TestActorRequestFactory factory =
                TestActorRequestFactory.newInstance(RejectionBusTest.class, generatedTenantId);
        Command command = factory.createCommand(updateProjectName);
        return Rejections.createRejection(invalidProjectName, command);
    }

    public static Rejection missingOwnerRejection() {
        ProjectId projectId = newProjectId();
        ProjectRejections.MissingOwner msg =
                ProjectRejections.MissingOwner.newBuilder()
                                              .setProjectId(projectId)
                                              .build();
        Command command = io.spine.server.commandbus.Given.ACommand.withMessage(
                Sample.messageOfType(RjRemoveOwner.class));
        return Rejections.createRejection(msg, command);
    }

    public static Rejection cannotModifyDeletedEntity(Class<? extends Message> commandMessage) {
        ProjectId projectId = newProjectId();
        Any idAny = AnyPacker.pack(projectId);
        CannotModifyDeletedEntity rejectionMsg = CannotModifyDeletedEntity.newBuilder()
                                                                                .setEntityId(idAny)
                                                                                .build();
        Command command = io.spine.server.commandbus.Given.ACommand.withMessage(
                Sample.messageOfType(commandMessage));
        return Rejections.createRejection(rejectionMsg, command);
    }

    private static ProjectId newProjectId() {
        ProjectId projectId = ProjectId.newBuilder()
                                             .setId(newUuid())
                                             .build();
        return projectId;
    }
}
