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

import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.CommandContext;
import io.spine.core.TenantId;
import io.spine.server.type.CommandEnvelope;
import io.spine.system.server.event.CommandDispatched;
import io.spine.system.server.command.ScheduleCommand;
import io.spine.system.server.SystemWriteSide;
import io.spine.system.server.WriteSideFunction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A set of callbacks invoked when a command processing reaches a given point.
 */
final class CommandFlowWatcher {

    private final WriteSideFunction function;

    CommandFlowWatcher(WriteSideFunction function) {
        this.function = checkNotNull(function);
    }

    /**
     * Posts the {@link CommandDispatched} system event.
     *
     * @param command the dispatched command
     */
    void onDispatchCommand(CommandEnvelope command) {
        CommandDispatched systemEvent = CommandDispatched
                .newBuilder()
                .setId(command.id())
                .build();
        postSystemEvent(systemEvent, command.tenantId());
    }

    /**
     * Posts the {@link ScheduleCommand} system command.
     *
     * @param command the scheduled command
     */
    void onScheduled(CommandEnvelope command) {
        CommandContext context = command.context();
        CommandContext.Schedule schedule = context.getSchedule();
        ScheduleCommand systemCommand = ScheduleCommand
                .newBuilder()
                .setId(command.id())
                .setSchedule(schedule)
                .build();
        postSystemCommand(systemCommand, command.tenantId());
    }

    private void postSystemEvent(EventMessage systemEvent, TenantId tenantId) {
        SystemWriteSide writeSide = function.get(tenantId);
        writeSide.postEvent(systemEvent);
    }

    private void postSystemCommand(CommandMessage systemCommand, TenantId tenantId) {
        SystemWriteSide writeSide = function.get(tenantId);
        writeSide.postCommand(systemCommand);
    }
}
