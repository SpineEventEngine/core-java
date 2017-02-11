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

package org.spine3.server.command;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.server.type.CommandClass;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Commands.getMessage;

/**
 * The holder of a {@code Command}, which provides convenient access to its properties.
 *
 * @author Alexander Yevsyukov
 */
final class CommandEnvelope {

    /** The command that we wrap. */
    private final Command command;

    // The below fields are calculated from the command.

    /** The ID of the command. */
    private final CommandId commandId;

    /** The command message. */
    private final Message commandMessage;

    /** The command class. */
    private final CommandClass commandClass;

    CommandEnvelope(Command command) {
        this.command = checkNotNull(command);
        this.commandId = getId(command);
        this.commandMessage = getMessage(command);
        this.commandClass = CommandClass.of(commandMessage);
    }

    public Command getCommand() {
        return command;
    }

    public CommandId getCommandId() {
        return commandId;
    }

    public Message getCommandMessage() {
        return commandMessage;
    }

    public CommandContext getCommandContext() {
        return command.getContext();
    }

    public CommandClass getCommandClass() {
        return commandClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommandEnvelope)) {
            return false;
        }
        CommandEnvelope that = (CommandEnvelope) o;
        return Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }
}
