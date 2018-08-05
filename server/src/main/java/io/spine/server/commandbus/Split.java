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
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.system.server.CommandSplit;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@code CommandSequence} of two or more commands which is generated in response to
 * a source command.
 *
 * @author Alexander Yevsyukov
 */
public final class Split extends OnCommand<CommandSplit, CommandSplit.Builder, Split> {

    Split(CommandEnvelope command) {
        super(command.getId(), command.getMessage(), command.getCommandContext());
    }

    @CanIgnoreReturnValue
    @Override
    public Split add(Message commandMessage) {
        return super.add(commandMessage);
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
    protected void addPosted(CommandSplit.Builder builder, Command command) {
        builder.addProduced(command.getId());
    }

    @Override
    @CanIgnoreReturnValue // The resulting event is going to be deprecated in favor of system events.
    public CommandSplit postAll(CommandBus bus) {
        checkState(size() >= 2,
                   "The split sequence must have at least two commands. " +
                           "For converting a command to another please use " +
                           "`CommandSequence.transform()`."
        );
        CommandSplit split = super.postAll(bus);
        return split;
    }
}
