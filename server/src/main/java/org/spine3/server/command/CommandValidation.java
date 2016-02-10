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
import org.spine3.base.Identifiers;
import org.spine3.base.Response;

import java.util.Map;

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
        final Response response = Response.newBuilder()
                .setError(Error.newBuilder()
                    .setType(CommandValidationError.getDescriptor().getFullName())
                    .setCode(CommandValidationError.UNSUPPORTED_COMMAND.getNumber())
                    .putAllAttributes(commandTypeAttribute(commandType))
                    .setMessage("Command " + commandType + " is not supported."))
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
     * Creates a {@code Response} for a command with missing namespace attribute, which is required
     * in a multitenant application.
     */
    public static Response unknownNamespace(Message command, CommandContext context) {
        final String commandType = command.getDescriptorForType().getFullName();
        final String errMsg = String.format("Command %s (id: %s) has no namespace attribute in the context.", commandType,
                Identifiers.idToString(context.getCommandId()));
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
