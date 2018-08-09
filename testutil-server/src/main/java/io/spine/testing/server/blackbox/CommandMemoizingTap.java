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

package io.spine.testing.server.blackbox;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.server.bus.BusFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * A {@link BusFilter} which remembers all the accepted command messages and never halts the command
 * processing.
 *
 * @author Dmytro Dashenkov
 */
public final class CommandMemoizingTap implements BusFilter<CommandEnvelope> {

    private final Map<CommandId, Message> commandMessages = newHashMap();
    private final List<Command> commands = Lists.newArrayList();

    /**
     * Looks up the command message by the command ID.
     */
    public <M extends Message> Optional<M> find(CommandId commandId, Class<M> commandClass) {
        Message commandMessage = commandMessages.get(commandId);
        assertThat(commandMessage, instanceOf(commandClass));
        @SuppressWarnings("unchecked") // Checked with an assertion.
        M result = (M) commandMessage;
        return ofNullable(result);
    }

    @Override
    public Optional<Ack> accept(CommandEnvelope envelope) {
        commandMessages.put(envelope.getId(), envelope.getMessage());
        commands.add(envelope.getCommand());
        return Optional.empty();
    }

    /**
     * Obtains immutable list with commands collected by the tap so far.
     */
    public List<Command> commands() {
        return ImmutableList.copyOf(commands);
    }
}
