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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.EventId;
import io.spine.system.server.MarkCausedCommands;
import io.spine.system.server.SystemGateway;

import static com.google.common.base.Preconditions.checkState;

/**
 * A sequence with two or more commands generated in response to an incoming event.
 *
 * <p>The result of the sequence is the system command for event lifecycle aggregate.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class SeveralCommands
        extends OnEvent<MarkCausedCommands, MarkCausedCommands.Builder, SeveralCommands> {

    SeveralCommands(EventId origin, ActorContext actorContext) {
        super(origin, actorContext);
    }

    @CanIgnoreReturnValue
    @SuppressWarnings("CheckReturnValue") // calling builder
    public SeveralCommands addAll(Iterable<? extends Message> commandMessage) {
        for (Message message : commandMessage) {
            add(message);
        }
        checkState(size() > 1, "This sequence must have more than one message");
        return this;
    }

    @Override
    protected MarkCausedCommands.Builder newBuilder() {
        return MarkCausedCommands.newBuilder()
                                 .setId(origin());
    }

    @Override
    @SuppressWarnings("CheckReturnValue") // calling builder
    protected void
    addPosted(MarkCausedCommands.Builder builder, Command command, SystemGateway gateway) {
        CommandId commandId = command.getId();
        builder.addProduced(commandId);
        markReacted(gateway, commandId);
    }
}
