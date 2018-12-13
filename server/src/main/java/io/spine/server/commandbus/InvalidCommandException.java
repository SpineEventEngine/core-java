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

import com.google.protobuf.Message;
import com.google.protobuf.Value;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandValidationError;
import io.spine.core.MessageInvalid;
import io.spine.type.TypeName;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ExceptionFactory;

import java.util.Map;

import static java.lang.String.format;

/**
 * The exception for reporting invalid commands.
 *
 * <p>A command is invalid if it's supported (there's a handler for the command), but it's
 * attributes are not populated according to framework conventions or validation constraints.
 *
 * @author Alexander Yevsyukov
 */
public class InvalidCommandException extends CommandException implements MessageInvalid {

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

        ConstraintViolationExceptionFactory helper =
                new ConstraintViolationExceptionFactory(command, violations);
        return helper.newException();
    }

    /**
     * Creates an exception for a command with missing {@code tenant_id} attribute in
     * the {@code CommandContext}, which is required in a multitenant application.
     */
    public static InvalidCommandException missingTenantId(Command command) {
        CommandEnvelope envelope = CommandEnvelope.of(command);
        Message commandMessage = envelope.getMessage();
        String errMsg = format(
                "The command (class: `%s`, type: `%s`, id: `%s`) is posted to " +
                "multitenant Command Bus, but has no `tenant_id` attribute in the context.",
                CommandClass.of(commandMessage)
                            .value()
                            .getName(),
                TypeName.of(commandMessage),
                Identifier.toString(envelope.getId()));
        Error error = unknownTenantError(commandMessage, errMsg);
        return new InvalidCommandException(errMsg, command, error);
    }

    /**
     * Creates an error for a command with missing {@code tenant_id}
     * attribute in the {@code CommandContext}, which is required in a multitenant application.
     */
    public static Error unknownTenantError(Message commandMessage, String errorText) {
        Error error = Error
                .newBuilder()
                .setType(InvalidCommandException.class.getCanonicalName())
                .setCode(CommandValidationError.TENANT_UNKNOWN.getNumber())
                .setMessage(errorText)
                .putAllAttributes(commandTypeAttribute(commandMessage))
                .build();
        return error;
    }

    public static InvalidCommandException inapplicableTenantId(Command command) {
        CommandEnvelope cmd = CommandEnvelope.of(command);
        TypeName typeName = TypeName.of(cmd.getMessage());
        String errMsg = format(
                "The command (class: %s, type: %s, id: %s) was posted to single-tenant " +
                "CommandBus, but has tenant_id: %s attribute set in the command context.",
                cmd.getMessageClass(),
                typeName,
                cmd.getId(),
                cmd.getTenantId());
        Error error = inapplicableTenantError(cmd.getMessage(), errMsg);
        return new InvalidCommandException(errMsg, command, error);
    }

    private static Error inapplicableTenantError(Message commandMessage, String errMsg) {
        Error error = Error
                .newBuilder()
                .setType(InvalidCommandException.class.getCanonicalName())
                .setCode(CommandValidationError.TENANT_INAPPLICABLE.getNumber())
                .setMessage(errMsg)
                .putAllAttributes(commandTypeAttribute(commandMessage))
                .build();
        return error;
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
