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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.base.Errors;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.RejectionEventContext;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.server.event.EventFactory;
import io.spine.server.model.EventProducer;
import io.spine.string.Stringifiers;
import io.spine.system.server.MarkCommandAsErrored;
import io.spine.system.server.MarkCommandAsRejected;
import io.spine.system.server.SystemGateway;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.spine.base.Identifier.pack;
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
        if (isPreProcessed(exception)) {
            return HandledError.forPreProcessed();
        } else if(causedByRejection(exception)) {
            return HandledError.forRejection(exception, envelope);
            // TODO:2018-07-30:dmytro.dashenkov: MarkCommandAsRejected.
        } else {
            log().error(format("Error dispatching command (class: %s id: %s).",
                               envelope.getMessage().getClass(),
                               Stringifiers.toString(envelope.getId())),
                        exception);
            Error error = Errors.causeOf(exception);
            markErrored(envelope, error);
            return HandledError.forRuntime(exception);
        }
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

    private void markRejected(CommandEnvelope command, RuntimeException exception) {
        CommandId commandId = command.getId();

        MarkCommandAsRejected systemCommand = MarkCommandAsRejected
                .newBuilder()
                .setId(commandId)
                .setRejectionEventId((EventId) null)
                .build();
        postSystem(systemCommand);
    }

    private void postSystem(Message systemCommand) {
        systemGateway.postCommand(systemCommand);
    }

    private static boolean isPreProcessed(RuntimeException exception) {
        return exception instanceof CommandDispatchingException;
    }

    /**
     * A wrapper for a handled error.
     *
     * <p>{@linkplain #rethrow() Rethrows} the encountered {@link RuntimeException} if it is not
     * caused by a rejection or has already been rethrown by a {@code CommandErrorHandler}.
     */
    public static final class HandledError {

        /**
         * The instance of {@code HandledError} with {@code null} exception.
         */
        private static final HandledError EMPTY = new HandledError(null, null);

        private final @Nullable CommandEnvelope origin;

        /**
         * The handled {@link RuntimeException}.
         */
        private final @Nullable RuntimeException exception;

        private HandledError(@Nullable CommandEnvelope origin,
                             @Nullable RuntimeException exception) {
            this.origin = origin;
            this.exception = exception;
        }

        private static HandledError forPreProcessed() {
            return EMPTY;
        }

        private static HandledError forRuntime(RuntimeException exception) {
            return new HandledError(null, exception);
        }

        private static HandledError forRejection(RuntimeException rejection,
                                                 CommandEnvelope command) {
            return new HandledError(command, rejection);
        }

        /**
         * Rethrows the handled exception if it was <b>not</b> caused by a rejection or
         * rethrown earlier.
         *
         * <p>Otherwise, preforms no action.
         */
        public void rethrow() {
            if (exception != null) {
                throw new CommandDispatchingException(exception);
            }
        }

        /**
         * Converts the handled error into a rejection {@linkplain Event event}.
         *
         * <p>The produced {@link Event} does not have a {@link Version}.
         * The {@linkplain io.spine.core.EventContext#getProducerId() producer ID} is a string with
         * the value equal to {@code "CommandErrorHandler"}.
         *
         * @return the handled rejection event or {@link Optional#empty()} if the handled error is
         * not a command rejection
         */
        Optional<Event> asRejection() {
            if (origin == null) {
                return Optional.empty();
            } else {
                Event event = convertToRejection();
                return Optional.of(event);
            }
        }

        private Event convertToRejection() {
            checkNotNull(origin);
            checkNotNull(exception);

            EventProducer producer = RejectionProducer.INSTANCE;

            ThrowableMessage throwable = (ThrowableMessage) getRootCause(exception);
            Message rejection = throwable.getMessageThrown();
            Any producerId = throwable.producerId()
                                      .orElse(producer.getProducerId());
            EventFactory eventFactory = EventFactory.on(origin, producerId);
            Event result = eventFactory.createRejectionEvent(rejection,
                                                             producer.getVersion(),
                                                             context());
            return result;
        }

        private RejectionEventContext context() {
            checkNotNull(exception);
            checkNotNull(origin);

            Throwable cause = getRootCause(exception);
            String stacktrace = getStackTraceAsString(cause);
            Message commandMessage = origin.getMessage();
            Any commandMessageAny = AnyPacker.pack(commandMessage);
            RejectionEventContext result = RejectionEventContext
                    .newBuilder()
                    .setStacktrace(stacktrace)
                    .setCommandMessage(commandMessageAny)
                    .build();
            return result;
        }
    }

    /**
     * The {@link EventProducer} for rejection events fired from the {@code CommandErrorHandler}.
     */
    private enum RejectionProducer implements EventProducer {

        INSTANCE;

        private static final Any EVENT_PRODUCER_ID =
                pack(CommandErrorHandler.class.getSimpleName());

        @Override
        public Any getProducerId() {
            return EVENT_PRODUCER_ID;
        }

        @Override
        public Version getVersion() {
            return Version.getDefaultInstance();
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
