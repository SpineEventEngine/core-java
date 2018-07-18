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

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;

import static java.lang.String.format;

/**
 * Exception that is thrown when a command could not be posted to the {@code CommandBus} by
 * a {@code ProcessManager}.
 *
 * @author Alexander Yevsyukov
 */
public class CommandPostingException extends RuntimeException {

    private static final long serialVersionUID = 0L;
    private final GeneratedMessageV3 commandMessage;
    private final CommandContext context;

    CommandPostingException(Message message, CommandContext context, Throwable cause) {
        super(
                format("Unable to post command (message: %s, context: %s)", message, context),
                cause
        );
        // The cast is safe since all the messages we use are generated.
        commandMessage = (GeneratedMessageV3) message;
        this.context = context;
    }

    /**
     * Obtains the command message of the command which could not be posted.
     */
    public Message getCommandMessage() {
        return commandMessage;
    }

    /**
     * Obtains the context of the command which could not be posted.
     */
    public CommandContext getContext() {
        return context;
    }
}
