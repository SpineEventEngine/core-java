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
import org.spine3.base.CommandValidationError;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.type.CommandClass;

import static java.lang.String.format;

/**
 * Exception that is thrown when a scheduled command was not delivered to the target in time.
 *
 * <p>For example, in the case when a server restarted when it was the time to deliver a scheduled command.
 *
 * @author Alexander Litus
 */
public class CommandExpiredException extends CommandException {

    public CommandExpiredException(Command command) {
        super(messageFormat(command), command, commandExpiredError(Commands.getMessage(command)));
    }

    private static String messageFormat(Command command) {
        final CommandClass commandClass = CommandClass.of(command);
        final String typeName = TypeUrl.ofCommand(command).getTypeName();
        final String result = format("A scheduled command expired. Command class: `%s`; Protobuf type: `%s`.",
                                     commandClass, typeName);
        return result;
    }

    /** Creates an instance of the command expired error. */
    public static Error commandExpiredError(Message commandMessage) {
        final String errMsg = format("Scheduled command of type `%s` expired.", CommandClass.of(commandMessage));
        final Error.Builder error = Error.newBuilder()
                .setType(CommandValidationError.getDescriptor().getFullName())
                .setMessage(errMsg)
                .putAllAttributes(commandTypeAttribute(commandMessage));
        return error.build();
    }

    private static final long serialVersionUID = 0L;
}
