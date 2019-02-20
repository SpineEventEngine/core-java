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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.core.Command;
import io.spine.server.type.CommandEnvelope;
import io.spine.system.server.CommandTransformed;
import io.spine.system.server.SystemWriteSide;

import static com.google.common.base.Preconditions.checkState;

/**
 * A command sequence containing only one element.
 */
@Internal
public final class Transform
        extends OnCommand<CommandTransformed, CommandTransformed.Builder, Transform> {

    private Transform(CommandEnvelope command) {
        super(command.getId(), command.getCommandContext()
                                      .getActorContext());
    }

    /**
     * Creates an empty sequence for transforming incoming command into another one.
     */
    public static Transform transform(CommandEnvelope command) {
        return new Transform(command);
    }

    /**
     * Sets the message for the target command.
     */
    public Transform to(CommandMessage targetMessage) {
        return add(targetMessage);
    }

    /**
     * Posts the command to the bus and returns resulting event.
     */
    @CanIgnoreReturnValue
    public CommandTransformed post(CommandBus bus) {
        checkState(size() == 1, "The transformation sequence must have exactly one command.");
        CommandTransformed result = postAll(bus);
        return result;
    }

    @Override
    protected CommandTransformed.Builder newBuilder() {
        CommandTransformed.Builder result = CommandTransformed
                .newBuilder()
                .setId(origin());
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder method
    @Override
    protected
    void addPosted(CommandTransformed.Builder builder, Command command, SystemWriteSide writeSide) {
        builder.setProduced(command.getId());
    }
}
