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
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandId;
import io.spine.type.TypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static java.lang.String.format;

/**
 * Convenience wrapper for logging errors and warnings.
 *
 * @author Alexander Yevsyukov
 */
public class Log {

    /** The logger instance used by {@code CommandBus}. */
    static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    public void errorHandling(Exception exception, Message commandMessage, CommandId commandId) {
        String msg = formatMessageTypeAndId(
                "Exception while handling command `%s` (ID: `%s`).",
                commandMessage,
                commandId);
        log().error(msg, exception);
    }

    public void rejectedWith(ThrowableMessage rej, Message commandMessage, CommandId commandId) {
        String msg = formatMessageTypeAndId(
                "The command `%s` (ID: `%s`) was rejected.",
                commandMessage,
                commandId);
        log().warn(msg, rej);
    }

    public void errorHandlingUnknown(Throwable throwable,
                                     Message commandMessage,
                                     CommandId commandId) {
        String msg = formatMessageTypeAndId(
                "Throwable encountered when handling command `%s` (ID: `%s`).",
                commandMessage,
                commandId);
        log().error(msg, throwable);
    }

    void errorExpiredCommand(Message commandMsg, CommandId id) {
        String msg = formatMessageTypeAndId(
                "Expired scheduled command `%s` (ID: `%s`).",
                commandMsg,
                id);
        log().error(msg);
    }

    /**
     * Creates a formatted string with type of the command message and command ID.
     *
     * <p>The {@code format} string must have two {@code %s} format specifiers.
     * The first specifier is for message type name. The second is for command ID.
     *
     * @param format    the format string
     * @param commandId the ID of the command
     * @return formatted string
     */
    private static String formatMessageTypeAndId(String format,
                                                 Message commandMessage,
                                                 CommandId commandId) {
        checkNotNull(format);
        checkNotEmptyOrBlank(format, "format string");

        String cmdType = TypeName.of(commandMessage)
                                       .value();
        String id = Identifier.toString(commandId);
        String result = format(format, cmdType, id);
        return result;
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandBus.class);
    }
}
