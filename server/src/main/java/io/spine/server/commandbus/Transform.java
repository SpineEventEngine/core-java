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
import io.spine.core.CommandContext;
import io.spine.server.procman.CommandTransformed;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.Commands.toDispatched;

/**
 * A command sequence containing only one element.
 *
 * @author Alexander Yevsyukov
 */
public final class Transform
        extends OnCommand<CommandTransformed, CommandTransformed.Builder, Transform> {

    Transform(CommandBus commandBus, Message sourceMessage, CommandContext context) {
        super(sourceMessage, context, commandBus);
    }

    /**
     * Sets the message for the target command.
     */
    public io.spine.server.commandbus.Transform to(Message targetMessage) {
        return add(targetMessage);
    }

    /**
     * Posts the command to the bus and returns resulting event.
     */
    @CanIgnoreReturnValue // The resulting event is going to be deprecated in favor of system events.
    public CommandTransformed post() {
        checkState(size() == 1, "The conversion sequence must have exactly one command.");
        CommandTransformed result = postAll();
        //TODO:2018-08-05:alexander.yevsyukov: post system command MarkCommandTransformed
        return result;
    }

    @Override
    protected CommandTransformed.Builder newBuilder() {
        CommandTransformed.Builder result = CommandTransformed
                .newBuilder()
                .setSource(source());
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder method
    @Override
    protected void addPosted(CommandTransformed.Builder builder, Message message,
                             CommandContext context) {
        builder.setProduced(toDispatched(message, context));
    }
}
