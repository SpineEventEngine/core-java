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

package io.spine.server.command.model.given.handler;

import io.spine.base.Identifier;
import io.spine.server.command.Assign;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.event.RefProjectCreated;

/**
 * A command handler which always rejects the passed command.
 *
 * @author Alexander Yevsyukov
 */
public class RejectingHandler extends TestCommandHandler {
    @Assign
    RefProjectCreated handleTest(RefCreateProject cmd) throws EntityAlreadyArchived {
        throw EntityAlreadyArchived
                .newBuilder()
                .setEntityId(Identifier.pack(cmd.getProjectId()))
                .build();
    }
}
