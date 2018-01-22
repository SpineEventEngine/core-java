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

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandValidationError;
import io.spine.core.MessageRejection;
import io.spine.type.ClassName;
import io.spine.type.TypeName;

import java.util.Map;

import static java.lang.String.format;

/**
 * Abstract base for exceptions related to commands.
 *
 * @author Alexander Yevsyukov
 */
public abstract class CommandException extends RuntimeException implements MessageRejection {

    /**
     * The name of the attribute of the command type reported in an error.
     *
     * @see #commandTypeAttribute(Message)
     * @see Error
     */
    public static final String ATTR_COMMAND_TYPE_NAME = "commandType";

    private static final long serialVersionUID = 0L;

    private final Command command;
    private final Error error;

    /**
     * Creates a new instance.
     *
     * @param messageText an error message text
     * @param command a related command
     * @param error an error occurred
     */
    protected CommandException(String messageText, Command command, Error error) {
        super(messageText);
        this.command = command;
        this.error = error;
    }

    /**
     * Returns a map with a command type attribute.
     *
     * @param commandMessage a command message to get the type from
     */
    public static Map<String, Value> commandTypeAttribute(Message commandMessage) {
        final String commandType = TypeName.of(commandMessage)
                                           .value();
        final Value value = Value.newBuilder()
                                 .setStringValue(commandType)
                                 .build();
        return ImmutableMap.of(ATTR_COMMAND_TYPE_NAME, value);
    }

    protected static Error createError(String format,
                                       Command command,
                                       CommandValidationError errorCode) {
        final Message commandMessage = CommandEnvelope.of(command)
                                                      .getMessage();

        final String commandType = commandMessage.getDescriptorForType()
                                                 .getFullName();
        final String errMsg = format(format, commandType);
        final String descriptorName = CommandValidationError.getDescriptor()
                                                            .getFullName();
        final Error.Builder error =
                Error.newBuilder()
                     .setType(descriptorName)
                     .setMessage(errMsg)
                     .setCode(errorCode.getNumber())
                     .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }

    /** Returns a related command. */
    public Command getCommand() {
        return command;
    }

    @Override
    public Error asError() {
        return error;
    }

    @Override
    public Throwable asThrowable() {
        return this;
    }

    /**
     * Builds a formatted string for the passed format and a command.
     *
     * <p>The first parameter of the formatted string is a {@link ClassName} of the command message.
     *
     * <p>The second parameter is a {@link TypeName} of the command message.
     */
    protected static String messageFormat(String format, Command command) {
        final CommandEnvelope envelope = CommandEnvelope.of(command);
        final ClassName commandClass = envelope.getMessageClass()
                                               .getClassName();
        final TypeName typeName = envelope.getTypeName();
        final String result = format(format, commandClass, typeName);
        return result;
    }
}
