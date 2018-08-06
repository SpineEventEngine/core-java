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

package io.spine.testing.server.procman.given;

import io.spine.core.EventContext;
import io.spine.server.command.Command;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.server.TUAssignTask;
import io.spine.testing.server.TUProjectId;
import io.spine.testing.server.TUTaskCreated;
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.TUTaskCreationPmVBuilder;
import io.spine.testing.server.entity.given.Given;

/**
 * The dummy process manager that reacts on {@code TUTaskCreated} event and
 * routes a nested command.
 */
public class CommandingPm
        extends ProcessManager<TUProjectId, TUTaskCreationPm, TUTaskCreationPmVBuilder> {

    public static final TUProjectId ID = TUProjectId.newBuilder()
                                                     .setValue("test pm id")
                                                     .build();

    public static final TUAssignTask NESTED_COMMAND =
            TUAssignTask.newBuilder()
                        .setId(ID)
                        .build();

    protected CommandingPm(TUProjectId id) {
        super(id);
    }

    public static CommandingPm processManager() {
        return Given.processManagerOfClass(CommandingPm.class)
                    .withId(ID)
                    .build();
    }

    @Command
    TUAssignTask on(TUTaskCreated event, EventContext context) {
        return NESTED_COMMAND;
    }
}
