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
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.system.server.MarkSplit;
import io.spine.system.server.SystemGateway;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@code CommandSequence} of two or more commands which is generated in response to
 * a source command.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public final class Split extends OnCommand<MarkSplit, MarkSplit.Builder, Split> {

    Split(CommandEnvelope command) {
        super(command.getId(), command.getCommandContext()
                                      .getActorContext());
    }

    @CanIgnoreReturnValue
    public Split addAll(Iterable<? extends Message> commandMessages) {
        for (Message message : commandMessages) {
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
    protected MarkSplit.Builder newBuilder() {
        MarkSplit.Builder result = MarkSplit
                .newBuilder()
                .setId(origin());
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder method
    @Override
    protected void addPosted(MarkSplit.Builder builder, Command command,
                             SystemGateway gateway) {
        builder.addProduced(command.getId());
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote Overrides to open the method for outside use.
     */
    @Override
    @CanIgnoreReturnValue
    public MarkSplit postAll(CommandBus bus) {
        return super.postAll(bus);
    }
}
