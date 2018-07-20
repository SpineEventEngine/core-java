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

import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandValidationError;
import io.spine.server.bus.MessageUnhandled;

/**
 * Exception that is thrown when unsupported command is obtained
 * or in case there is no class for given Protobuf command message.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class UnsupportedCommandException extends CommandException implements MessageUnhandled {

    private static final long serialVersionUID = 0L;

    public UnsupportedCommandException(Command command) {
        super(messageFormat(
                 "No registered handler or dispatcher for the command of class: `%s`. " +
                 "Protobuf type: `%s`.", command),
              command,
              unsupportedCommand(command));
    }

    /** Creates an instance of unsupported command error. */
    private static Error unsupportedCommand(Command command) {
        String format = "Commands of the type `%s` are not supported.";
        CommandValidationError errorCode = CommandValidationError.UNSUPPORTED_COMMAND;
        return createError(format, command, errorCode);
    }
}
