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

import io.spine.annotation.Internal;
import io.spine.core.CommandEnvelope;
import io.spine.core.Rejection;
import io.spine.server.rejection.RejectionBus;
import io.spine.string.Stringifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Commands.rejectWithCause;
import static io.spine.core.Rejections.causedByRejection;
import static java.lang.String.format;

/**
 * The handler of the errors thrown while command dispatching.
 *
 * <p>The {@linkplain CommandDispatcher command dispatchers} may delegate
 * the {@linkplain CommandDispatcher#onError error handling} to an instance of
 * {@code CommandErrorHandler}.
 *
 * @author Dmytro Dashenkov
 * @see #handleError(CommandEnvelope, RuntimeException)
 */
@Internal
public final class CommandErrorHandler {

    private final RejectionBus rejectionBus;

    private CommandErrorHandler(RejectionBus rejectionBus) {
        this.rejectionBus = rejectionBus;
    }

    /**
     * Creates new instance of {@code CommandErrorHandler} with the given {@link RejectionBus}.
     *
     * @param rejectionBus the {@link RejectionBus} to post the command rejections into
     * @return a new instance of {@code CommandErrorHandler}
     */
    public static CommandErrorHandler with(RejectionBus rejectionBus) {
        checkNotNull(rejectionBus);
        return new CommandErrorHandler(rejectionBus);
    }

    /**
     * Handles an error occurred during dispatching of a command.
     *
     * <p>If the given {@code exception} has been caused by
     * a {@linkplain io.spine.base.ThrowableMessage command rejection}, the {@link Rejection} is
     * {@linkplain RejectionBus#post(Rejection) posted} to the {@code RejectionBus}. Otherwise,
     * the given {@code exception} is thrown.
     */
    public void handleError(CommandEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        if (causedByRejection(exception)) {
            handleRejection(envelope, exception);
        } else {
            handleRuntime(envelope, exception);
        }
    }

    private void handleRejection(CommandEnvelope envelope, RuntimeException exception) {
        Rejection rejection = rejectWithCause(envelope.getCommand(), exception);
        rejectionBus.post(rejection);
    }

    private static void handleRuntime(CommandEnvelope envelope, RuntimeException exception) {
        log().error(format("Error dispatching command (class: %s id: %s).",
                           envelope.getMessage().getClass(),
                           Stringifiers.toString(envelope.getId())),
                    exception);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandErrorHandler.class);
    }
}
