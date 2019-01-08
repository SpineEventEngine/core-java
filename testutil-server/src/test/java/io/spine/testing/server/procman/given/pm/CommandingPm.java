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

package io.spine.testing.server.procman.given.pm;

import io.spine.core.EventContext;
import io.spine.server.command.Command;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.TuPmStateVBuilder;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.command.TuAssignProject;
import io.spine.testing.server.given.entity.event.TuProjectCreated;

/**
 * The dummy process manager that reacts on {@code TUTaskCreated} event and
 * routes a nested command.
 */
public class CommandingPm
        extends ProcessManager<TuProjectId, TuPmState, TuPmStateVBuilder> {

    public static final TuProjectId ID =
            TuProjectId.newBuilder()
                       .setValue("comanding-pm-id")
                       .build();

    protected CommandingPm(TuProjectId id) {
        super(id);
    }

    public static CommandingPm newInstance() {
        return Given.processManagerOfClass(CommandingPm.class)
                    .withId(ID)
                    .build();
    }

    /**
     * Creates the command {@link TuAssignProject} in response to project creation.
     *
     * @implNote Suppose, we do not want to have un-assigned projects.
     */
    @Command
    TuAssignProject on(TuProjectCreated event, EventContext context) {
        return TuAssignProject.newBuilder()
                              .setId(event.getId())
                              .build();
    }
}
