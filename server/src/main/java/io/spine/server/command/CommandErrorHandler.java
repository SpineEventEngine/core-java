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

package io.spine.server.command;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.base.Errors;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.string.Stringifiers;
import io.spine.system.server.MarkCommandAsErrored;
import io.spine.system.server.MarkCommandAsRejected;
import io.spine.system.server.SystemGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
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

    private final SystemGateway systemGateway;

    private CommandErrorHandler(SystemGateway systemGateway) {
        this.systemGateway = systemGateway;
    }

    /**
     * Creates a new {@code CommandErrorHandler} with the given {@link SystemGateway}.
     *
     * @param systemGateway {@link SystemGateway} to post system commands into
     * @return new instance of {@code CommandErrorHandler}
     */
    public static CommandErrorHandler with(SystemGateway systemGateway) {
        checkNotNull(systemGateway);
        return new CommandErrorHandler(systemGateway);
    }

    /**
     * Handles an error occurred during dispatching of a command.
     *
     * @return the result of the error handing
     */
    @CanIgnoreReturnValue
    public HandledError handleError(CommandEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);

        boolean rejection = causedByRejection(exception);
        if (rejection) {
            return handleRejection(envelope, exception);
        } else {
            return handleRuntimeError(envelope, exception);
        }
    }

    private HandledError handleRejection(CommandEnvelope envelope, RuntimeException exception) {
        Rejection rejection = Rejection.fromThrowable(envelope, exception);
        markRejected(envelope, rejection);
        return HandledError.ofRejection(exception, envelope);
    }

    private HandledError handleRuntimeError(CommandEnvelope envelope, RuntimeException exception) {
        if (isPreProcessed(exception)) {
            return HandledError.ofPreProcessed();
        } else {
            return handleNewRuntimeError(envelope, exception);
        }
    }

    private HandledError handleNewRuntimeError(CommandEnvelope envelope,
                                               RuntimeException exception) {
        String commandTypeName = envelope.getMessage()
                                         .getClass()
                                         .getName();
        String commandIdAsString = Stringifiers.toString(envelope.getId());
        log().error(format("Error dispatching command (class: %s id: %s).",
                           commandTypeName, commandIdAsString),
                    exception);
        Error error = Errors.causeOf(exception);
        markErrored(envelope, error);
        return HandledError.ofRuntime(exception);
    }

    private static boolean isPreProcessed(RuntimeException exception) {
        return exception instanceof CommandDispatchingException;
    }

    private void markErrored(CommandEnvelope command, Error error) {
        CommandId commandId = command.getId();
        MarkCommandAsErrored systemCommand = MarkCommandAsErrored
                .newBuilder()
                .setId(commandId)
                .setError(error)
                .build();
        postSystem(systemCommand);
    }

    private void markRejected(CommandEnvelope command, Rejection rejection) {
        CommandId commandId = command.getId();

        MarkCommandAsRejected systemCommand = MarkCommandAsRejected
                .newBuilder()
                .setId(commandId)
                .setRejectionEvent(rejection.asEvent())
                .build();
        postSystem(systemCommand);
    }

    private void postSystem(Message systemCommand) {
        systemGateway.postCommand(systemCommand);
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
