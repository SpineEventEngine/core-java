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

package io.spine.server.procman.given.pm;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.server.commandbus.Given;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCencelIteration;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmOwnerChanged;

import static io.spine.server.procman.given.pm.TestProcessManager.ID;
import static io.spine.testdata.Sample.messageOfType;

/**
 * Factory of messages to be sent to the Process Manager.
 */
public class GivenMessages {

    private GivenMessages() {
    }

    public static PmCreateProject createProject() {
        return PmCreateProject
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmStartProject startProject() {
        return PmStartProject
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmAddTask addTask() {
        return PmAddTask
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmCencelIteration cancelIteration() {
        return PmCencelIteration
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static RejectionEnvelope entityAlreadyArchived(Class<? extends Message> commandClass) {
        Any id = Identifier.pack(TestProcessManager.class.getName());
        EntityAlreadyArchived rejectionMessage = EntityAlreadyArchived
                .newBuilder()
                .setEntityId(id)
                .build();
        Command command = Given.ACommand.withMessage(messageOfType(commandClass));
        Rejection rejection = Rejections.createRejection(rejectionMessage, command);
        return RejectionEnvelope.of(rejection);
    }

    public static PmOwnerChanged ownerChanged() {
        return PmOwnerChanged
                .newBuilder()
                .setProjectId(ID)
                .build();
    }
}
