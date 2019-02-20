/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandValidationError;
import io.spine.core.MessageInvalid;
import io.spine.server.type.CommandEnvelope;
import io.spine.type.TypeName;

import static java.lang.String.format;

/**
 * Reports an attempt to dispatch a duplicate command.
 *
 * <p>A command is considered a duplicate when its ID matches the ID of a command which was
 * already dispatched emitting events in a target aggregate.
 */
public class DuplicateCommandException extends CommandException implements MessageInvalid {

    private static final long serialVersionUID = 0L;

    private DuplicateCommandException(String messageText, Command command, Error error) {
        super(messageText, command, error);
    }

    /**
     * Creates an exception for a duplicate command.
     *
     * @param command the duplicate command
     * @return a newly constructed instance
     */
    public static DuplicateCommandException of(Command command) {
        CommandEnvelope envelope = CommandEnvelope.of(command);
        Message commandMessage = envelope.getMessage();
        String errorMessage = aggregateErrorMessage(envelope);
        Error error = error(commandMessage, errorMessage);
        return new DuplicateCommandException(errorMessage, command, error);
    }

    /**
     * Creates a duplicate command error.
     *
     * @param commandMessage the domain message that ended up a duplicate
     * @param errorText      the text to be set as the error message
     * @return a newly constructed instance
     */
    public static Error error(Message commandMessage, String errorText) {
        Error.Builder error = Error.newBuilder()
                                   .setType(DuplicateCommandException.class.getCanonicalName())
                                   .setCode(CommandValidationError.DUPLICATE.getNumber())
                                   .setMessage(errorText)
                                   .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }

    /**
     * Generates a formatted duplicate aggregate command error message.
     *
     * @param envelope the envelope with a command which is considered a duplicate
     * @return a string with details on what happened
     */
    private static String aggregateErrorMessage(CommandEnvelope envelope) {
        return format(
                "The command (class: `%s`, type: `%s`, id: `%s`) cannot be dispatched to a "
                        + "single entity twice.",
                envelope.getMessageClass()
                        .value()
                        .getName(),
                TypeName.of(envelope.getMessage()),
                Identifier.toString(envelope.getId()));
    }
}
