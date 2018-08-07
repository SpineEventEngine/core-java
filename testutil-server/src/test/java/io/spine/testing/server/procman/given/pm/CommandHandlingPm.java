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

package io.spine.testing.server.procman.given.pm;

import io.spine.core.CommandContext;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.TuPmStateVBuilder;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.command.TuAssignTask;
import io.spine.testing.server.given.entity.command.TuCreateTask;
import io.spine.testing.server.given.entity.event.TuTaskAssigned;

import static com.google.protobuf.util.Timestamps.fromNanos;

/**
 * A dummy process manager that handles a {@code TUCreateTask} command and routes a nested
 * command.
 */
public class CommandHandlingPm
        extends ProcessManager<TuTaskId, TuPmState, TuPmStateVBuilder> {

    public static final TuTaskId ID = TuTaskId
            .newBuilder()
            .setValue("handling-pm-id")
            .build();

    public static final TuAssignTask NESTED_COMMAND =
            TuAssignTask.newBuilder()
                        .setId(ID)
                        .build();

    protected CommandHandlingPm(TuTaskId id) {
        super(id);
    }

    public static CommandHandlingPm newInstance() {
        return Given.processManagerOfClass(CommandHandlingPm.class)
                    .withId(ID)
                    .build();
    }

    @Command
    TuAssignTask handle(TuCreateTask command, CommandContext context) {
        return TuAssignTask.newBuilder()
                           .setId(command.getId())
                           .build();
    }

    @Assign
    TuTaskAssigned handle(TuAssignTask command) {
        getBuilder().setTimestamp(fromNanos(123456));
        return TuTaskAssigned.newBuilder()
                             .setId(command.getId())
                             .build();
    }
}
