/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandValidationError;
import io.spine.core.MessageInvalid;
import io.spine.type.TypeName;

import static java.lang.String.format;

/**
 * Reports an attempt to dispatch a command more than once.
 *
 * <p>A command is considered a duplicate when its ID matches the ID of a command 
 * already dispatched.
 *
 * @author Mykhailo Drachuk
 */
public class DuplicateCommandException extends CommandException implements MessageInvalid {

    private static final long serialVersionUID = 0L;

    private DuplicateCommandException(String messageText, Command command, Error error) {
        super(messageText, command, error);
    }

    /**
     * Creates an exception for a duplicate command.
     */
    public static DuplicateCommandException of(Command command) {
        final CommandEnvelope envelope = CommandEnvelope.of(command);
        final Message commandMessage = envelope.getMessage();
        final String errMsg = errorMessage(envelope);
        final Error error = error(commandMessage, errMsg);
        return new DuplicateCommandException(errMsg, command, error);
    }

    /**
     * Creates a duplicate command error.
     */
    public static Error error(Message commandMessage, String errorText) {
        final Error.Builder error =
                Error.newBuilder()
                     .setType(DuplicateCommandException.class.getCanonicalName())
                     .setCode(CommandValidationError.DUPLICATE.getNumber())
                     .setMessage(errorText)
                     .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }

    /**
     * Generates a formatted duplicate command error message.
     */
    private static String errorMessage(CommandEnvelope envelope) {
        return format(
                "The command (class: `%s`, type: `%s`, id: `%s`) cannot be dispatched twice.",
                envelope.getMessageClass()
                        .value()
                        .getName(),
                TypeName.of(envelope.getMessage()),
                Identifier.toString(envelope.getId()));
    }

}
