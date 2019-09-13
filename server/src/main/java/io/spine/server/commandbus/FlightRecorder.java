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

package io.spine.server.commandbus;

import io.spine.base.EventMessage;
import io.spine.core.CommandContext;
import io.spine.server.type.CommandEnvelope;
import io.spine.system.server.SystemWriteSide;
import io.spine.system.server.WriteSideFunction;
import io.spine.system.server.event.CommandDispatched;
import io.spine.system.server.event.CommandScheduled;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Records a command log by posting the corresponding events into the
 * {@link io.spine.system.server.SystemContext system context}.
 */
final class FlightRecorder implements CommandFlowWatcher {

    private final WriteSideFunction function;

    FlightRecorder(WriteSideFunction function) {
        this.function = checkNotNull(function);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts the {@link CommandDispatched} system event.
     */
    @Override
    public void onDispatchCommand(CommandEnvelope command) {
        CommandDispatched systemEvent = CommandDispatched
                .newBuilder()
                .setId(command.id())
                .build();
        postSystemEvent(systemEvent, command);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts the {@link CommandScheduled} system event.
     */
    @Override
    public void onScheduled(CommandEnvelope command) {
        CommandContext context = command.context();
        CommandContext.Schedule schedule = context.getSchedule();
        CommandScheduled systemEvent = CommandScheduled
                .newBuilder()
                .setId(command.id())
                .setSchedule(schedule)
                .build();
        postSystemEvent(systemEvent, command);
    }

    private void postSystemEvent(EventMessage systemEvent, CommandEnvelope cmd) {
        SystemWriteSide writeSide = function.get(cmd.tenantId());
        writeSide.postEvent(systemEvent, cmd.asMessageOrigin());
    }
}
