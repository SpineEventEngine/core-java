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
import com.google.protobuf.Value;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandValidationError;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.type.CommandClass;
import org.spine3.type.TypeName;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.ConstraintViolations.ExceptionFactory;

import java.util.Map;

import static java.lang.String.format;
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

    private static final String MSG_VALIDATION_ERROR = "Command message does not match " +
                                                       "validation constrains.";

    private InvalidCommandException(String messageText, Command command, Error error) {
        super(messageText, command, error);
    }

    /**
     * Creates an exception instance for a command message,
     * which has fields that violate validation constraint(s).
     *
     * @param command    an invalid command
     * @param violations constraint violations for the command message
     */
    public static InvalidCommandException onConstraintViolations(
            Command command, Iterable<ConstraintViolation> violations) {

        final ConstraintViolationExceptionFactory helper =
                new ConstraintViolationExceptionFactory(command, violations);
        return helper.newException();
    }

    /**
     * Creates an exception for a command with missing {@code tenant_id} attribute in
     * the {@code CommandContext}, which is required in a multitenant application.
     */
    public static InvalidCommandException onMissingTenantId(Command command) {
        final Message commandMessage = Commands.getMessage(command);
        final CommandContext context = command.getContext();
        final String errMsg = format(
                "The command (class: `%s`, type: `%s`, id: `%s`) is posted to " +
                "multitenant Command Bus, but has no `tenant_id` attribute in the context.",
                CommandClass.of(commandMessage)
                            .value()
                            .getName(),
                TypeName.of(commandMessage),
                idToString(context.getCommandId()));
        final Error error = unknownTenantError(commandMessage, errMsg);
        return new InvalidCommandException(errMsg, command, error);
    }

    /**
     * Creates an error for a command with missing {@code tenant_id}
     * attribute in the {@code CommandContext}, which is required in a multitenant application.
     */
    public static Error unknownTenantError(Message commandMessage, String errorText) {
        final Error.Builder error = Error.newBuilder()
                .setType(CommandValidationError.getDescriptor().getFullName())
                .setCode(CommandValidationError.TENANT_UNKNOWN.getNumber())
                .setMessage(errorText)
                .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }

    public static InvalidCommandException onInapplicableTenantId(Command command) {
        final CommandEnvelope cmd = CommandEnvelope.of(command);
        final TypeName typeName = TypeName.of(cmd.getMessage());
        final String errMsg = format(
                "The command (class: %s, type: %s, id: %s) was posted to single-tenant " +
                "CommandBus, but has tenant_id: %s attribute set in the command context.",
                cmd.getMessageClass(),
                typeName,
                cmd.getCommandId(),
                command.getContext()
                       .getTenantId());
        final Error error = inapplicableTenantError(cmd.getMessage(), errMsg);
        return new InvalidCommandException(errMsg, command, error);
    }

    private static Error inapplicableTenantError(Message commandMessage, String errMsg) {
        final Error.Builder error =
                Error.newBuilder()
                     .setType(CommandValidationError.getDescriptor()
                                                    .getFullName())
                     .setCode(CommandValidationError.TENANT_INAPPLICABLE.getNumber())
                     .setMessage(errMsg)
                     .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }
    /**
     * A helper utility aimed to create an {@code InvalidCommandException} to report the
     * command which field values violate validation constraint(s).
     */
    private static class ConstraintViolationExceptionFactory
                                 extends ExceptionFactory<InvalidCommandException,
                                                          Command,
                                                          CommandClass,
                                                          CommandValidationError> {
        private final CommandClass commandClass;

        protected ConstraintViolationExceptionFactory(Command command,
                                                      Iterable<ConstraintViolation> violations) {
            super(command, violations);
            this.commandClass = CommandClass.of(command);
        }

        @Override
        protected CommandClass getMessageClass() {
            return commandClass;
        }

        @Override
        protected CommandValidationError getErrorCode() {
            return CommandValidationError.INVALID_COMMAND;
        }

        @Override
        protected String getErrorText() {
            return MSG_VALIDATION_ERROR;
        }

        @Override
        protected Map<String, Value> getMessageTypeAttribute(Message commandMessage) {
            return commandTypeAttribute(commandMessage);
        }

        @Override
        protected InvalidCommandException createException(String exceptionMsg,
                                                          Command command,
                                                          Error error) {
            return new InvalidCommandException(exceptionMsg, command, error);
        }
    }
}
