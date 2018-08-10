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
import io.spine.system.server.MarkTransformed;
import io.spine.system.server.SystemGateway;

import static com.google.common.base.Preconditions.checkState;

/**
 * A command sequence containing only one element.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public final class Transform
        extends OnCommand<MarkTransformed, MarkTransformed.Builder, Transform> {

    Transform(CommandEnvelope command) {
        super(command.getId(), command.getCommandContext()
                                      .getActorContext());
    }

    /**
     * Sets the message for the target command.
     */
    public Transform to(Message targetMessage) {
        return add(targetMessage);
    }

    /**
     * Posts the command to the bus and returns resulting event.
     */
    @CanIgnoreReturnValue
    public MarkTransformed post(CommandBus bus) {
        checkState(size() == 1, "The transformation sequence must have exactly one command.");
        MarkTransformed result = postAll(bus);
        return result;
    }

    @Override
    protected MarkTransformed.Builder newBuilder() {
        MarkTransformed.Builder result = MarkTransformed
                .newBuilder()
                .setId(origin());
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder method
    @Override
    protected void addPosted(MarkTransformed.Builder builder, Command command,
                             SystemGateway gateway) {
        builder.setProduced(command.getId());
    }
}
