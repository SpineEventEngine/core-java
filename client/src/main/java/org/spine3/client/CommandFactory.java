/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.client;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.time.ZoneOffset;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The factory to generate new {@link Command} instances.
 *
 * @author Alexander Yevsyukov
 */
public class CommandFactory extends ActorRequestFactory<CommandFactory> {

    protected CommandFactory(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates new factory with the same user and bounded context name and new time zone offset.
     *
     * @param zoneOffset the offset of the time zone
     * @return new command factory at new time zone
     */
    @Override
    public CommandFactory switchTimezone(ZoneOffset zoneOffset) {
        return switchTimezone(zoneOffset, newBuilder());
    }

    /**
     * Creates new {@code Command} with the passed message.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * @param message the command message
     * @return new command instance
     */
    public Command createCommand(Message message) {
        checkNotNull(message);
        final CommandContext context = createContext();
        final Command result = Commands.createCommand(message, context);
        return result;
    }

    /**
     * Creates new {@code Command} with the passed message and target entity version.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * @param message       the command message
     * @param targetVersion the ID of the entity for applying commands if {@code null}
     *                      the commands can be applied to any entity
     * @return new command instance
     */
    public Command createCommand(Message message, int targetVersion) {
        checkNotNull(message);
        checkNotNull(targetVersion);

        final CommandContext context = createContext(targetVersion);
        final Command result = Commands.createCommand(message, context);
        return result;
    }

    /**
     * Creates command context for a new command with entity ID.
     */
    protected CommandContext createContext(int targetVersion) {
        return Commands.createContext(getTenantId(), getActor(), getZoneOffset(), targetVersion);
    }

    /**
     * Creates command context for a new command.
     */
    protected CommandContext createContext() {
        return Commands.createContext(getTenantId(), getActor(), getZoneOffset());
    }

    public static class Builder
            extends ActorRequestFactory.AbstractBuilder<CommandFactory, Builder> {

        @Override
        protected Builder thisInstance() {
            return this;
        }

        @Override
        public CommandFactory build() {
            super.build();
            final CommandFactory result = new CommandFactory(this);
            return result;
        }
    }
}
