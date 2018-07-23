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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.base.Errors;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.Rejection;
import io.spine.server.rejection.RejectionBus;
import io.spine.string.Stringifiers;
import io.spine.system.server.MarkCommandAsErrored;
import io.spine.system.server.MarkCommandAsRejected;
import io.spine.system.server.SystemGateway;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
    private final SystemGateway systemGateway;

    private CommandErrorHandler(Builder builder) {
        this.rejectionBus = builder.rejectionBus;
        this.systemGateway = builder.systemGateway;
    }

    /**
     * Handles an error occurred during dispatching of a command.
     *
     * <p>If the given {@code exception} has been caused by
     * a {@linkplain io.spine.base.ThrowableMessage command rejection}, the {@link Rejection} is
     * {@linkplain RejectionBus#post(Rejection) posted} to the {@code RejectionBus}. Otherwise,
     * the given {@code exception} is thrown.
     *
     * @return the result of the error handing
     */
    @CanIgnoreReturnValue
    public HandledError handleError(CommandEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        if (causedByRejection(exception)) {
            return handleRejection(envelope, exception);
        } else {
            return handleRuntime(envelope, exception);
        }
    }

    private HandledError handleRejection(CommandEnvelope envelope, RuntimeException exception) {
        Rejection rejection = rejectWithCause(envelope.getCommand(), exception);
        rejectionBus.post(rejection);
        markRejected(envelope, rejection);
        return HandledError.forRejection();
    }

    private void markRejected(CommandEnvelope command, Rejection rejection) {
        CommandId commandId = command.getId();
        MarkCommandAsRejected systemCommand = MarkCommandAsRejected
                .newBuilder()
                .setId(commandId)
                .setRejection(rejection)
                .build();
        postSystem(systemCommand);
    }

    private HandledError handleRuntime(CommandEnvelope envelope, RuntimeException exception) {
        log().error(format("Error dispatching command (class: %s id: %s).",
                           envelope.getMessage().getClass(),
                           Stringifiers.toString(envelope.getId())),
                    exception);
        Error error = Errors.causeOf(exception);
        markErrored(envelope, error);
        return HandledError.forRuntime(exception);
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

    private void postSystem(Message systemCommand) {
        systemGateway.postCommand(systemCommand);
    }

    /**
     * A result of an error handling.
     */
    public static final class HandledError {

        private static final HandledError EMPTY = new HandledError(null);

        /**
         * The handled {@link RuntimeException}.
         *
         * <p>If the handled error was caused by a {@link Rejection}, this field is equal
         * to {@code null}. Otherwise the field is non-null.
         */
        private final @MonotonicNonNull RuntimeException exception;

        private HandledError(@Nullable RuntimeException exception) {
            this.exception = exception;
        }

        private static HandledError forRejection() {
            return EMPTY;
        }

        private static HandledError forRuntime(RuntimeException exception) {
            return new HandledError(exception);
        }

        /**
         * Rethrows the handled exception if it was <b>not</b> caused by a {@link Rejection}.
         *
         * <p>If the exception was caused by a {@link Rejection}, preforms no action.
         */
        public void rethrowIfRuntime() {
            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * Creates a new instance of {@code Builder} for {@code CommandErrorHandler} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code CommandErrorHandler} instances.
     */
    public static final class Builder {

        private RejectionBus rejectionBus;
        private SystemGateway systemGateway;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        public Builder setRejectionBus(RejectionBus rejectionBus) {
            this.rejectionBus = checkNotNull(rejectionBus);
            return this;
        }

        public Builder setSystemGateway(SystemGateway systemGateway) {
            this.systemGateway = checkNotNull(systemGateway);
            return this;
        }

        /**
         * Creates a new instance of {@code CommandErrorHandler}.
         *
         * @return new instance of {@code CommandErrorHandler}
         */
        public CommandErrorHandler build() {
            return new CommandErrorHandler(this);
        }
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
