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
package io.spine.server.procman.given.delivery;

import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

public class GivenMessage {

    /** Prevents instantiation of this test environment class. */
    private GivenMessage() {
    }

    public static Command createProject() {
        ProjectId projectId = projectId();
        Command command = createCommand(PmCreateProject.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build());
        return command;
    }

    public static Event projectStarted() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                TestEventFactory.newInstance(AnyPacker.pack(projectId),
                                             GivenMessage.class
                );

        PmProjectStarted msg = PmProjectStarted.newBuilder()
                                               .setProjectId(projectId)
                                               .build();

        Event result = eventFactory.createEvent(msg);
        return result;
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(Identifier.newUuid())
                        .build();
    }

    private static Command createCommand(CommandMessage cmdMessage) {
        Command result = TestActorRequestFactory.newInstance(GivenMessage.class)
                                                .createCommand(cmdMessage);
        return result;
    }
}
