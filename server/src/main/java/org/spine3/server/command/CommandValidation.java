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

package org.spine3.server.command;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandValidationError;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.base.Response;
import org.spine3.base.ValidationFailure;
import org.spine3.validation.options.ConstraintViolation;

import java.util.List;
import java.util.Map;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.protobuf.Messages.toAny;

/**
 * Utility class for working with command validation.
 *
 * @author Alexander Yevsyukov
 */
public class CommandValidation {

    private CommandValidation() {
    }

    /**
     * Attribute names for command-related business failures.
     */
    public interface Attribute {
        String COMMAND_TYPE_NAME = "commandType";
    }

    /**
     * Creates a {@code Response} for getting unsupported command, which is a programming error.
     */
    public static Response unsupportedCommand(Message command) {
        final String commandType = command.getDescriptorForType().getFullName();
        final String errMsg = String.format("Commands of the type `%s` are not supported.", commandType);
        final Response response = Response.newBuilder()
                .setError(Error.newBuilder()
                    .setType(CommandValidationError.getDescriptor().getFullName())
                    .setCode(CommandValidationError.UNSUPPORTED_COMMAND.getNumber())
                    .putAllAttributes(commandTypeAttribute(commandType))
                    .setMessage(errMsg))
                .build();
        return response;
    }

    public static boolean isUnsupportedCommand(Response response) {
        if (response.getStatusCase() == Response.StatusCase.ERROR) {
            final Error error = response.getError();
            return error.getCode() == CommandValidationError.UNSUPPORTED_COMMAND.getNumber();
        }
        return false;
    }

    /**
     * Creates a {@code Response} for getting a command with invalid fields (e.g., marked as "required" but not set).
     *
     * @param command an invalid command
     * @param violations constraint violations found in command message
     */
    public static Response invalidCommand(Message command, List<ConstraintViolation> violations) {
        final String commandType = command.getDescriptorForType().getFullName();
        final ValidationFailure failureInstance = ValidationFailure.newBuilder()
                .addAllConstraintViolation(violations)
                .build();
        final Failure.Builder failure = Failure.newBuilder()
                .setInstance(toAny(failureInstance))
                .setTimestamp(getCurrentTime())
                .putAllAttributes(commandTypeAttribute(commandType));
        final Response response = Response.newBuilder()
                .setFailure(failure)
                .build();
        return response;
    }

    /**
     * Creates a {@code Response} for a command with missing namespace attribute, which is required
     * in a multitenant application.
     */
    public static Response unknownNamespace(Message command, CommandContext context) {
        final String commandType = command.getDescriptorForType().getFullName();
        final String errMsg = String.format("Command `%s` (id: `%s`) has no namespace attribute in the context.",
                commandType,
                context.getCommandId().getUuid());
        final Response response = Response.newBuilder()
                .setError(Error.newBuilder()
                    .setType(CommandValidationError.getDescriptor().getFullName())
                    .setCode(CommandValidationError.NAMESPACE_UNKNOWN.getNumber())
                    .setMessage(errMsg)
                    .putAllAttributes(commandTypeAttribute(commandType)))
                .build();
        return response;
    }

    private static Map<String, Value> commandTypeAttribute(String commandType) {
        return ImmutableMap.of(Attribute.COMMAND_TYPE_NAME, Value.newBuilder().setStringValue(commandType).build());
    }
}
