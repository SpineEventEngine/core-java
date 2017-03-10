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
package org.spine3.server.command.error;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandValidationError;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.type.CommandClass;
import org.spine3.type.TypeName;

/**
 * Exception that is thrown when unsupported command is obtained
 * or in case there is no class for given Protobuf command message.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class UnsupportedCommandException extends CommandException {

    private static final long serialVersionUID = 0L;

    public UnsupportedCommandException(Command command) {
        super(messageFormat(command), command, unsupportedCommandError(Commands.getMessage(command)));
    }

    private static String messageFormat(Command command) {
        final CommandClass commandClass = CommandClass.of(command);
        final String typeName = TypeName.ofCommand(command);
        final String result = String.format(
                "There is no registered handler or dispatcher for the command of class: `%s`. Protobuf type: `%s`",
                commandClass, typeName
        );
        return result;
    }

    /** Creates an instance of unsupported command error. */
    private static Error unsupportedCommandError(Message commandMessage) {
        final String commandType = commandMessage.getDescriptorForType().getFullName();
        final String errMsg = String.format("Commands of the type `%s` are not supported.", commandType);
        final Error.Builder error = Error.newBuilder()
                .setType(CommandValidationError.getDescriptor().getFullName())
                .setCode(CommandValidationError.UNSUPPORTED_COMMAND.getNumber())
                .putAllAttributes(commandTypeAttribute(commandMessage))
                .setMessage(errMsg);
        return error.build();
    }
}
