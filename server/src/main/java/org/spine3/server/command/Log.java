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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.CommandId;
import org.spine3.base.FailureThrowable;

import static org.spine3.base.Commands.formatCommandTypeAndId;
import static org.spine3.base.Commands.formatMessageTypeAndId;

/**
 * Convenience wrapper for logging errors and warnings.
 *
 * @author Alexander Yevsyukov
 */
class Log {

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandBus.class);
    }

    /** The logger instance used by {@code CommandBus}. */
    static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    void errorDispatching(Exception exception, Command command) {
        final String msg = formatCommandTypeAndId("Unable to dispatch command `%s` (ID: `%s`)", command);
        log().error(msg, exception);
    }

    void errorHandling(Exception exception, Message commandMessage, CommandId commandId) {
        final String msg = formatMessageTypeAndId("Exception while handling command `%s` (ID: `%s`)",
                                                  commandMessage, commandId);
        log().error(msg, exception);
    }

    void failureHandling(FailureThrowable flr, Message commandMessage, CommandId commandId) {
        final String msg = formatMessageTypeAndId("Business failure occurred when handling command `%s` (ID: `%s`)",
                                                  commandMessage, commandId);
        log().warn(msg, flr);
    }

    void errorHandlingUnknown(Throwable throwable, Message commandMessage, CommandId commandId) {
        final String msg = formatMessageTypeAndId("Throwable encountered when handling command `%s` (ID: `%s`)",
                                                  commandMessage, commandId);
        log().error(msg, throwable);
    }

    void errorExpiredCommand(Message commandMsg, CommandId id) {
        final String msg = formatMessageTypeAndId("Expired scheduled command `%s` (ID: `%s`).", commandMsg, id);
        log().error(msg);
    }
}
