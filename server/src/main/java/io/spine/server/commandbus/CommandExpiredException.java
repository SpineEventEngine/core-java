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

import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandValidationError;

/**
 * Exception that is thrown when a scheduled command was not delivered to the target in time.
 *
 * <p>For example, in the case when a server restarted when it was the time to deliver a
 * scheduled command.
 *
 * @author Alexander Litus
 */
public class CommandExpiredException extends CommandException {

    private static final long serialVersionUID = 0L;

    public CommandExpiredException(Command command) {
        super(messageFormat(
                "A scheduled command expired. Command class: `%s`; Protobuf type: `%s`.", command),
              command,
              commandExpired(command));
    }

    /** Creates an instance of the command expired error. */
    static Error commandExpired(Command command) {
        String format = "Scheduled command of type `%s` expired.";
        CommandValidationError errorCode = CommandValidationError.EXPIRED;
        return createError(format, command, errorCode);
    }
}
