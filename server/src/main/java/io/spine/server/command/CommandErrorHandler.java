/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.base.Errors;
import io.spine.base.EventMessage;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.logging.Logging;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.event.RejectionEnvelope;
import io.spine.system.server.CommandErrored;
import io.spine.system.server.CommandRejected;
import io.spine.system.server.SystemWriteSide;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.command.Rejections.causedByRejection;

/**
 * The handler of the errors thrown while command dispatching.
 *
 * <p>The {@linkplain CommandDispatcher command dispatchers} may delegate
 * the {@linkplain CommandDispatcher#onError error handling} to an instance of
 * {@code CommandErrorHandler}.
 *
 * @see #handle(CommandEnvelope, RuntimeException)
 */
@Internal
public final class CommandErrorHandler implements Logging {

    private final SystemWriteSide systemWriteSide;

    private CommandErrorHandler(SystemWriteSide systemWriteSide) {
        this.systemWriteSide = systemWriteSide;
    }

    /**
     * Creates a new {@code CommandErrorHandler} with the given {@link SystemWriteSide}.
     *
     * @param systemWriteSide {@link SystemWriteSide} to post system commands into
     * @return new instance of {@code CommandErrorHandler}
     */
    public static CommandErrorHandler with(SystemWriteSide systemWriteSide) {
        checkNotNull(systemWriteSide);
        return new CommandErrorHandler(systemWriteSide);
    }

    /**
     * Handles an error occurred during dispatching of a command.
     *
     * @return the result of the error handing
     */
    @CanIgnoreReturnValue
    public CaughtError handle(CommandEnvelope command, RuntimeException exception) {
        checkNotNull(command);
        checkNotNull(exception);
        boolean rejection = causedByRejection(exception);
        CaughtError result = rejection
                             ? handleRejection(command, exception)
                             : handleRuntimeError(command, exception);
        return result;
    }

    private CaughtError handleRejection(CommandEnvelope command, RuntimeException exception) {
        RejectionEnvelope rejection = RejectionEnvelope.from(command, exception);
        markRejected(command, rejection);
        return CaughtError.ofRejection(command, exception);
    }

    private CaughtError handleRuntimeError(CommandEnvelope envelope, RuntimeException exception) {
        CaughtError result = isHandled(exception)
                             ? CaughtError.handled()
                             : handleNewRuntimeError(envelope, exception);
        return result;
    }

    private CaughtError handleNewRuntimeError(CommandEnvelope cmd, RuntimeException exception) {
        String commandTypeName = cmd.getMessage()
                                    .getClass()
                                    .getName();
        String commandId = cmd.idAsString();
        _error(exception, "Error dispatching command (class: `{}` id: {}).",
               commandTypeName, commandId);
        Error error = Errors.causeOf(exception);
        markErrored(cmd, error);
        return CaughtError.ofRuntime(exception);
    }

    /**
     * Handles the passed exception, and if it represents a rejection event it is passed to
     * the consumer. If the exception was not caused by a rejection, or rethrown earlier, it
     * will be rethrown.
     *
     * @param cmd
     *         the command which caused the exception
     * @param exception
     *         the thrown exception
     * @param consumer
     *         the consumer of the rejection event
     */
    public void handle(CommandEnvelope cmd, RuntimeException exception, Consumer<Event> consumer) {
        CaughtError error = handle(cmd, exception);
        error.asRejection()
             .map(RejectionEnvelope::getOuterObject)
             .ifPresent(consumer);
        error.rethrowOnce();
    }

    /**
     * Determined if the given {@code exception} was previously handled by
     * a {@code CommandErrorHandler}.
     *
     * <p>When a {@code CommandErrorHandler} rethrows a caught exception, it always wraps it into
     * a {@link CommandDispatchingException}. If the exception is NOT an instance of
     * {@link CommandDispatchingException}, it is NOT considered handled. Otherwise,
     * the exception IS considered handled.
     *
     * @param exception the {@link RuntimeException} to check
     * @return {@code true} if the given {@code exception} is handled, {@code false} otherwise
     */
    private static boolean isHandled(RuntimeException exception) {
        return exception instanceof CommandDispatchingException;
    }

    private void markErrored(CommandEnvelope command, Error error) {
        CommandId commandId = command.getId();
        CommandErrored systemEvent = CommandErrored
                .newBuilder()
                .setId(commandId)
                .setError(error)
                .build();
        postSystem(systemEvent);
    }

    private void markRejected(CommandEnvelope command, RejectionEnvelope rejection) {
        CommandId commandId = command.getId();

        CommandRejected systemEvent = CommandRejected
                .newBuilder()
                .setId(commandId)
                .setRejectionEvent(rejection.getOuterObject())
                .build();
        postSystem(systemEvent);
    }

    private void postSystem(EventMessage systemEvent) {
        systemWriteSide.postEvent(systemEvent);
    }
}
