/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import org.spine3.base.CommandContext;
import org.spine3.base.CommandValidationError;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.base.ValidationError;
import org.spine3.server.type.CommandClass;
import org.spine3.type.TypeName;
import org.spine3.validate.ConstraintViolation;

import static org.spine3.base.Identifiers.idToString;

/**
 * The exception for reporting invalid commands.
 *
 * <p>A command is invalid if it's supported (there's a handler for the command), but it's
 * attributes are not populated according to framework conventions or validation constraints.
 *
 * @author Alexander Yevsyukov
 */
public class InvalidCommandException extends CommandException {

    private static final long serialVersionUID = 0L;

    private static final String MSG_VALIDATION_ERROR = "Command message does match validation constrains.";

    private InvalidCommandException(String messageText, Command command, Error error) {
        super(messageText, command, error);
    }

    /**
     * Creates an exception instance for a command message, which has fields that violate validation constraint(s).
     *
     * @param command an invalid command
     * @param violations constraint violations for the command message
     */
    public static InvalidCommandException onConstraintViolations(Command command,
                                                                 Iterable<ConstraintViolation> violations) {
        final Error error = invalidCommandMessageError(Commands.getMessage(command), violations, MSG_VALIDATION_ERROR);
        final String text = MSG_VALIDATION_ERROR + " Message class: " + CommandClass.of(command) +
                " See Error.getValidationError() for details.";
        //TODO:2016-06-09:alexander.yevsyukov: Add more diagnostics on the validation problems discovered.
        return new InvalidCommandException(text, command, error);
    }

    /**
     * Creates an instance of {@code Error} for a command message, which has fields that violate
     * validation constraint(s).
     */
    private static Error invalidCommandMessageError(Message commandMessage,
                                                    Iterable<ConstraintViolation> violations,
                                                    String errorText) {
        final ValidationError validationError = ValidationError.newBuilder()
                .addAllConstraintViolation(violations)
                .build();
        final Error.Builder error = Error.newBuilder()
                .setType(CommandValidationError.getDescriptor().getFullName())
                .setCode(CommandValidationError.INVALID_COMMAND.getNumber())
                .setValidationError(validationError)
                .setMessage(errorText)
                .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }

    /**
     * Creates an exception for a command with missing {@code tenant_id} attribute in the {@code CommandContext},
     * which is required in a multitenant application.
     */
    public static InvalidCommandException onMissingTenantId(Command command) {
        final Message commandMessage = Commands.getMessage(command);
        final CommandContext context = command.getContext();
        final String errMsg = String.format(
                "The command (class: `%s`, type: `%s`, id: `%s`) is posted to multitenant Command Bus, " +
                        "but has no `tenant_id` attribute in the context.",
                TypeName.of(commandMessage).value(),
                CommandClass.of(commandMessage).value(),
                idToString(context.getCommandId()));
        final Error error = unknownTenantError(commandMessage, errMsg);
        return new InvalidCommandException(errMsg, command, error);
    }

    /**
     * Creates an error for a command with missing {@code tenant_id} attribute  attribute in the {@code CommandContext},
     * which is required in a multitenant application.
     */
    public static Error unknownTenantError(Message commandMessage, String errorText) {
        final Error.Builder error = Error.newBuilder()
                .setType(CommandValidationError.getDescriptor().getFullName())
                .setCode(CommandValidationError.TENANT_UNKNOWN.getNumber())
                .setMessage(errorText)
                .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }
}
