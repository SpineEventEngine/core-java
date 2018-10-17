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
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.EventEnvelope;
import io.spine.core.EventId;
import io.spine.system.server.EventCausedCommands;
import io.spine.system.server.SystemWriteSide;

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
        extends OnEvent<EventCausedCommands, EventCausedCommands.Builder, SeveralCommands> {

    private SeveralCommands(EventId origin, ActorContext actorContext) {
        super(origin, actorContext);
    }

    /**
     * Creates an empty sequence for creating two or more commands in response to the passed event.
     */
    public static SeveralCommands respondMany(EventEnvelope event) {
        return new SeveralCommands(event.getId(), event.getActorContext());
    }

    @CanIgnoreReturnValue
    @SuppressWarnings("CheckReturnValue") // calling builder
    public SeveralCommands addAll(Iterable<? extends CommandMessage> commandMessage) {
        for (CommandMessage message : commandMessage) {
            add(message);
        }
        checkState(size() > 1, "This sequence must have more than one message");
        return this;
    }

    @Override
    protected EventCausedCommands.Builder newBuilder() {
        return EventCausedCommands.newBuilder()
                                  .setId(origin());
    }

    @Override
    @SuppressWarnings("CheckReturnValue") // calling builder
    protected void
    addPosted(EventCausedCommands.Builder builder, Command command, SystemWriteSide gateway) {
        CommandId commandId = command.getId();
        builder.addProduced(commandId);
    }

    /**
     * {@inheritDoc}
     * ­
     * @apiNote Overrides to open the method for outside use.
     */
    @Override
    @CanIgnoreReturnValue
    public EventCausedCommands postAll(CommandBus bus) {
        return super.postAll(bus);
    }
}
