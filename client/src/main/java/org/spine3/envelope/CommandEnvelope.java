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

package org.spine3.envelope;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.type.CommandClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getId;

/**
 * The holder of a {@code Command}, which provides convenient access to its properties.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandEnvelope extends AbstractMessageEnvelope<Command> {

    // The below fields are calculated from the command.

    /** The ID of the command. */
    private final CommandId commandId;

    /** The command message. */
    private final Message commandMessage;

    /** The command class. */
    private final CommandClass commandClass;

    private CommandEnvelope(Command command) {
        super(command);
        this.commandId = getId(command);
        this.commandMessage = Commands.getMessage(command);
        this.commandClass = CommandClass.of(commandMessage);
    }

    /**
     * Creates an instance with the passed command.
     */
    public static CommandEnvelope of(Command command) {
        checkNotNull(command);
        return new CommandEnvelope(command);
    }

    /**
     * Obtains the enclosed command object.
     */
    public Command getCommand() {
        return getOuterObject();
    }

    /**
     * Obtains the ID of the command.
     */
    public CommandId getCommandId() {
        return commandId;
    }

    /**
     * Obtains the command message.
     */
    @Override
    public Message getMessage() {
        return commandMessage;
    }

    /**
     * Obtains the command context.
     */
    public CommandContext getCommandContext() {
        return getOuterObject().getContext();
    }

    /**
     * Obtains the command class.
     */
    public CommandClass getCommandClass() {
        return commandClass;
    }
}
