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

import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.TenantId;
import io.spine.system.server.MarkCommandAsDispatched;
import io.spine.system.server.ScheduleCommand;
import io.spine.system.server.SystemGateway;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A set of callbacks invoked when a command processing reaches a given point.
 *
 * @author Dmytro Dashenkov
 */
final class CommandFlowWatcher {

    private final SystemGateway systemGateway;

    CommandFlowWatcher(SystemGateway gateway) {
        this.systemGateway = checkNotNull(gateway);
    }

    /**
     * Posts the {@link MarkCommandAsDispatched} system command.
     *
     * @param command the dispatched command
     */
    void onDispatchCommand(CommandEnvelope command) {
        MarkCommandAsDispatched systemCommand = MarkCommandAsDispatched
                .newBuilder()
                .setId(command.getId())
                .build();
        postSystem(systemCommand, command.getTenantId());
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
        postSystem(systemCommand, command.getTenantId());
    }

    private void postSystem(Message systemCommand, TenantId tenantId) {
        SystemGateway tenantAwareGateway = TenantAwareSystemGateway
                .create()
                .atopOf(systemGateway)
                .withTenant(tenantId)
                .build();
        tenantAwareGateway.postCommand(systemCommand);
    }
}
