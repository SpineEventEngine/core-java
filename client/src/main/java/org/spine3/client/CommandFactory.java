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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Public API for creating {@link Command} instances, using the {@code ActorRequestFactory}
 * configuration.
 *
 * @see ActorRequestFactory#command()
 */
public final class CommandFactory {

    private final ActorRequestFactory actorRequestFactory;

    CommandFactory(ActorRequestFactory actorRequestFactory) {
        this.actorRequestFactory = checkNotNull(actorRequestFactory);
    }

    /**
     * Creates new {@code Command} with the passed message.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * @param message the command message
     * @return new command instance
     */
    public Command create(Message message) {
        checkNotNull(message);
        final CommandContext context = createContext();
        final Command result = Commands.createCommand(message, context);
        return result;
    }

    /**
     * Creates command context for a new command.
     */
    CommandContext createContext() {
        return Commands.createContext(actorRequestFactory.getTenantId(),
                                      actorRequestFactory.getActor(),
                                      actorRequestFactory.getZoneOffset());
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
    public Command create(Message message, int targetVersion) {
        checkNotNull(message);
        checkNotNull(targetVersion);

        final CommandContext context = createContext(targetVersion);
        final Command result = Commands.createCommand(message, context);
        return result;
    }

    /**
     * Creates command context for a new command with entity ID.
     */
    private CommandContext createContext(int targetVersion) {
        return Commands.createContext(actorRequestFactory.getTenantId(),
                                      actorRequestFactory.getActor(),
                                      actorRequestFactory.getZoneOffset(), targetVersion);
    }
}
