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

package io.spine.server.commandbus;

import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.TenantId;
import io.spine.system.server.CommandDispatched;
import io.spine.system.server.GatewayFunction;
import io.spine.system.server.ScheduleCommand;
import io.spine.system.server.SystemGateway;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A set of callbacks invoked when a command processing reaches a given point.
 *
 * @author Dmytro Dashenkov
 */
final class CommandFlowWatcher {

    private final GatewayFunction gateway;

    CommandFlowWatcher(GatewayFunction gateway) {
        this.gateway = checkNotNull(gateway);
    }

    /**
     * Posts the {@link CommandDispatched} system event.
     *
     * @param command the dispatched command
     */
    void onDispatchCommand(CommandEnvelope command) {
        CommandDispatched systemEvent = CommandDispatched
                .newBuilder()
                .setId(command.getId())
                .build();
        postSystemEvent(systemEvent, command.getTenantId());
    }

    /**
     * Posts the {@link ScheduleCommand} system command.
     *
     * @param command the scheduled command
     */
    void onScheduled(CommandEnvelope command) {
        CommandContext context = command.getCommandContext();
        CommandContext.Schedule schedule = context.getSchedule();
        ScheduleCommand systemCommand = ScheduleCommand
                .newBuilder()
                .setId(command.getId())
                .setSchedule(schedule)
                .build();
        postSystemCommand(systemCommand, command.getTenantId());
    }

    private void postSystemEvent(EventMessage systemEvent, TenantId tenantId) {
        SystemGateway gateway = this.gateway.get(tenantId);
        gateway.postEvent(systemEvent);
    }

    private void postSystemCommand(CommandMessage systemCommand, TenantId tenantId) {
        SystemGateway gateway = this.gateway.get(tenantId);
        gateway.postCommand(systemCommand);
    }
}
