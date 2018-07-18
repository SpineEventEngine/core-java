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

package io.spine.system.server;

import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.server.bus.BusFilter;

import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;

/**
 * A {@link BusFilter} which remembers all the accepted command messages and never halts the command
 * processing.
 *
 * @author Dmytro Dashenkov
 */
final class CommandWatcher implements BusFilter<CommandEnvelope> {

    private final Map<CommandId, Message> commandMessages = newHashMap();

    /**
     * Looks up the command message by the command ID.
     */
    <M extends Message> Optional<M> find(CommandId commandId) {
        Message commandMessage = commandMessages.get(commandId);
        @SuppressWarnings("unchecked")
        M result = (M) commandMessage;
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Ack> accept(CommandEnvelope envelope) {
        commandMessages.put(envelope.getId(), envelope.getMessage());
        return Optional.empty();
    }
}
