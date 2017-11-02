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

package io.spine.server.command;

import io.spine.annotation.Internal;
import io.spine.core.CommandEnvelope;
import io.spine.core.Commands;
import io.spine.core.Rejection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Rejections.causedByRejection;
import static java.lang.String.format;

/**
 * A utility for handling the errors which happen while
 * {@linkplain io.spine.server.commandbus.CommandDispatcher dispatching}
 * {@linkplain io.spine.core.Command commands}.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class CommandDispatchingErrors {

    private CommandDispatchingErrors() {
        // Prevent utility class instantiation.
    }

    /**
     * Retrieves the {@linkplain Rejection command rejection} from the passed {@code exception} or
     * throws the {@code exception} if it is not caused by a {@code Rejection}.
     *
     * @param envelope  the command which caused the {@code exception}
     * @param exception the {@link Exception} thrown while dispatching the given command
     * @return {@link Rejection} which caused the {@code exception} if any
     * @throws RuntimeException throws the given {@code exception} if it is not caused by
     *                          a {@link Rejection}
     */
    @SuppressWarnings("ProhibitedExceptionDeclared")
    public static Rejection onDispatchingError(RuntimeException exception,
                                               CommandEnvelope envelope) throws RuntimeException {
        checkNotNull(envelope);
        checkNotNull(exception);
        if (causedByRejection(exception)) {
            final Rejection rejection = Commands.rejectWithCause(envelope.getCommand(), exception);
            return rejection;
        } else {
            log().error(format("Error dispatching command (class: %s id: %s).",
                               envelope.getMessage().getClass(),
                               envelope.getId().getUuid()),
                        exception);
            throw exception;
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandDispatchingErrors.class);
    }
}
