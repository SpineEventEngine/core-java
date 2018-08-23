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

import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.TenantId;
import io.spine.server.bus.BusFilter;
import io.spine.system.server.GatewayFunction;
import io.spine.system.server.MarkCommandAsReceived;
import io.spine.system.server.SystemGateway;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link CommandBus} filter which watches the commands as they appear in the bus.
 *
 * <p>The filter notifies the System bounded context about new commands with
 * a {@link MarkCommandAsReceived} system command.
 *
 * <p>The filter never terminates the command processing, i.e. {@link #accept(CommandEnvelope)}
 * always returns an empty value.
 *
 * @author Dmytro Dashenkov
 */
final class CommandReceivedTap implements BusFilter<CommandEnvelope> {

    private final GatewayFunction gateway;

    CommandReceivedTap(GatewayFunction gateway) {
        this.gateway = checkNotNull(gateway);
    }

    @Override
    public Optional<Ack> accept(CommandEnvelope envelope) {
        MarkCommandAsReceived systemCommand = systemCommand(envelope.getCommand());
        TenantId tenantId = envelope.getTenantId();
        SystemGateway gateway = this.gateway.get(tenantId);
        gateway.postCommand(systemCommand);
        return Optional.empty();
    }

    private static MarkCommandAsReceived systemCommand(Command domainCommand) {
        MarkCommandAsReceived result = MarkCommandAsReceived
                .newBuilder()
                .setId(domainCommand.getId())
                .setPayload(domainCommand)
                .build();
        return result;
    }
}
