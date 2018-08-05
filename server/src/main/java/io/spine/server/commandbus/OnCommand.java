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
import io.spine.core.DispatchedCommand;

import static io.spine.core.Commands.toDispatched;

/**
 * Abstract base for command sequences initiated from a source command.
 *
 * @param <R> the type of the result generated when the command sequence is posted
 * @param <B> the type of the result builder
 * @param <S> the type of the sequence for the return type covariance
 * @author Alexander Yevsyukov
 */
abstract
class OnCommand<R extends Message, B extends Message.Builder, S extends CommandSequence<R, B, S>>
        extends CommandSequence<R, B, S> {

    private final Message sourceMessage;
    private final CommandContext sourceContext;

    OnCommand(Message message, CommandContext context, CommandBus bus) {
        super(context.getActorContext(), bus);
        this.sourceMessage = message;
        this.sourceContext = context;
    }

    protected DispatchedCommand source() {
        return toDispatched(this.sourceMessage, this.sourceContext);
    }
}
