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

import com.google.common.base.Optional;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.server.bus.BusFilter;
import io.spine.system.server.ReceiveCommand;
import io.spine.system.server.SystemGateway;

/**
 * @author Dmytro Dashenkov
 */
final class CommandReceivedFilter implements BusFilter<CommandEnvelope> {

    private final SystemGateway systemGateway;

    CommandReceivedFilter(SystemGateway gateway) {
        systemGateway = gateway;
    }

    @Override
    public Optional<Ack> accept(CommandEnvelope envelope) {
        ReceiveCommand systemCommand = systemCommand(envelope.getCommand());
        systemGateway.postCommand(systemCommand);
        return Optional.absent();
    }

    private static ReceiveCommand systemCommand(Command domainCommand) {
        ReceiveCommand result = ReceiveCommand
                .newBuilder()
                .setId(domainCommand.getId())
                .setPayload(domainCommand)
                .build();
        return result;
    }

    @Override
    public void close() {
        // NoOp.
    }
}
