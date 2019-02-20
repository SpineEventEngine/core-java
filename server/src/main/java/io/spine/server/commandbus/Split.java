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
import io.spine.system.server.CommandSplit;
import io.spine.system.server.SystemWriteSide;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@code CommandSequence} of two or more commands which is generated in response to
 * a source command.
 */
@Internal
public final class Split extends OnCommand<CommandSplit, CommandSplit.Builder, Split> {

    private Split(CommandEnvelope command) {
        super(command.getId(), command.getCommandContext()
                                      .getActorContext());
    }

    /**
     * Creates an empty sequence for splitting the source command into several ones.
     */
    public static Split split(CommandEnvelope command) {
        return new Split(command);
    }

    @CanIgnoreReturnValue
    public Split addAll(Iterable<? extends CommandMessage> commandMessages) {
        for (CommandMessage message : commandMessages) {
            add(message);
        }
        checkState(size() >= 2,
                   "The split sequence must have at least two commands. " +
                           "For converting a command to another please use " +
                           "`CommandSequence.transform()`."
        );
        return this;
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    protected CommandSplit.Builder newBuilder() {
        CommandSplit.Builder result = CommandSplit
                .newBuilder()
                .setId(origin());
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder method
    @Override
    protected
    void addPosted(CommandSplit.Builder builder, Command command, SystemWriteSide writeSide) {
        builder.addProduced(command.getId());
    }

    /**
     * {@inheritDoc}
     * &nbsp;
     * @apiNote Overrides to open the method for outside use.
     */
    @Override
    @CanIgnoreReturnValue
    public CommandSplit postAll(CommandBus bus) {
        return super.postAll(bus);
    }
}
