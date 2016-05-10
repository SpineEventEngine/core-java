/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.Internal;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Errors;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.failure.FailureThrowable;
import org.spine3.server.type.CommandClass;
import org.spine3.validate.options.ConstraintViolation;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.*;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 * @author Alexander Litus
 */
public class CommandBus implements AutoCloseable {

    private final DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();

    private final HandlerRegistry handlerRegistry = new HandlerRegistry();

    private final CommandStore commandStore;

    private final CommandStatusService commandStatusService;

    @Nullable
    private final CommandScheduler scheduler;

    /**
     * Is {@code true} if the Bounded Context (to which this Command Bus belongs) is multi-tenant.
     * Therefore commands posted to this Command Bus must have the {@code namespace} attribute.
     */
    private boolean isMultitenant;

    private ProblemLog problemLog = new ProblemLog();

    private CommandBus(Builder builder) {
        commandStore = builder.getCommandStore();
        scheduler = builder.getScheduler();
        commandStatusService = new CommandStatusService(commandStore);
    }

    /**
     * Creates a new builder for command bus.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Registers the passed command dispatcher.
     *
     * @param dispatcher the dispatcher to register
     * @throws IllegalArgumentException if {@link CommandDispatcher#getCommandClasses()} returns empty set
     */
    public void register(CommandDispatcher dispatcher) {
        handlerRegistry.checkNoHandlersRegisteredForCommandsOf(dispatcher);
        dispatcherRegistry.register(dispatcher);
    }

    /**
     * Unregisters dispatching for command classes by the passed dispatcher.
     *
     * <p>If the passed dispatcher deals with commands for which another dispatcher already registered
     * the dispatch entries for such commands will not be unregistered, and warning will be logged.
     *
     * @param dispatcher the dispatcher to unregister
     */
    public void unregister(CommandDispatcher dispatcher) {
        dispatcherRegistry.unregister(dispatcher);
    }

    /**
     * Registers the passed command handler.
     *
     * @param handler a {@code non-null} handler object
     * @throws IllegalArgumentException if the handler does not have command handling methods
     */
    public void register(CommandHandler handler) {
        dispatcherRegistry.checkNoDispatchersRegisteredForCommandsOf(handler);
        handlerRegistry.register(handler);
    }

    /**
     * Unregisters the command handler from the command bus.
     *
     * @param handler the handler to unregister
     */
    public void unregister(CommandHandler handler) {
        handlerRegistry.unregister(handler);
    }

    /**
     * Directs the command to be dispatched or handled.
     *
     * <p>If the command has scheduling attributes, it will be posted for execution by the configured
     * scheduler according to values of those scheduling attributes.
     *
     * <p>If a command does not have neither dispatcher nor handler, the error is returned via
     * {@link StreamObserver#onError(Throwable)} call with {@link UnsupportedCommandException} as the cause.
     *
     * @param command the command to be processed
     * @param responseObserver the observer to return the result of the call
     */
    public void post(Command command, StreamObserver<Response> responseObserver) {
        final CommandClass commandClass = CommandClass.of(command);
        // If the command is not supported, return as error.
        if (!isSupportedCommand(commandClass)) {
            handleUnsupported(command, responseObserver);
            return;
        }
        if (!handleValidation(command, responseObserver)) {
            return;
        }
        if (isScheduled(command)) {
            //TODO:2016-05-08:alexander.yevsyukov: Do store command if it was scheduled and update its status when it's executed.
            // It is needed to make command delivery more reliable. E.g. if a delay is long enough, not stored message
            // could be lost if the server, which holds it in memory is shut down.
            schedule(command);
            responseObserver.onNext(Responses.ok());
            responseObserver.onCompleted();
            return;
        }
        store(command);
        responseObserver.onNext(Responses.ok());
        doPost(command);
        responseObserver.onCompleted();
    }

    /**
     * Directs a command to be dispatched or handled.
     *
     * <p>Logs exceptions which may occur during dispatching or handling and
     * sets the command status to {@code error} or {@code failure} in the storage.
     *
     * @param command a command to post
     */
    /* package */ void doPost(Command command) {
        final Message message = getMessage(command);
        final CommandClass commandClass = CommandClass.of(message);
        final CommandContext commandContext = command.getContext();
        if (isDispatcherRegistered(commandClass)) {
            dispatch(command);
        } else if (isHandlerRegistered(commandClass)) {
            invokeHandler(message, commandContext);
        }
    }

    private boolean handleValidation(Command command, StreamObserver<Response> responseObserver) {
        final List<ConstraintViolation> violations = CommandValidator.getInstance().validate(command);
        if (!violations.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withCause(InvalidCommandException.onConstraintViolations(command, violations))
                            .asRuntimeException()
            );
            return false;
        }
        final CommandContext context = command.getContext();
        if (isMultitenant() && !context.hasNamespace()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withCause(InvalidCommandException.onMissingNamespace(command))
                            .asRuntimeException()
            );
            return false;
        }
        return true;
    }

    private void handleUnsupported(Command command, StreamObserver<Response> responseObserver) {
        final UnsupportedCommandException exception = new UnsupportedCommandException(command);
        storeWithErrorStatus(command, exception);
        responseObserver.onError(
                Status.INVALID_ARGUMENT
                        .withCause(exception)
                        .asRuntimeException()
        );
    }

    /**
     * Stores a command with the error status.
     *
     * @param command a command to store
     * @param exception an exception occurred during command processing
     */
    @VisibleForTesting
    /* package */ void storeWithErrorStatus(Command command, Exception exception) {
        store(command);
        //TODO:2016-05-08:alexander.yevsyukov: Support storage method that can make storing command with error in one call.
        commandStatusService.setToError(command.getContext().getCommandId(), exception);
    }

    /**
     * Checks if a command is supported by the Command Bus.
     *
     * @param commandClass a class of commands to check
     * @return {@code true} if there is a {@link CommandDispatcher} or a {@link CommandHandler} registered
     *      for commands of this type, {@code false} otherwise
     */
    @VisibleForTesting
    /* package */ boolean isSupportedCommand(CommandClass commandClass) {
        final boolean dispatcherRegistered = isDispatcherRegistered(commandClass);
        final boolean handlerRegistered = isHandlerRegistered(commandClass);
        final boolean isSupported = dispatcherRegistered || handlerRegistered;
        return isSupported;
    }

    private boolean isMultitenant() {
        return this.isMultitenant;
    }

    /**
     * Obtains the instance of the {@link CommandStatusService} associated with this command bus.
     */
    public CommandStatusService getCommandStatusService() {
        return commandStatusService;
    }

    private void dispatch(Command command) {
        final CommandClass commandClass = CommandClass.of(command);
        final CommandDispatcher dispatcher = getDispatcher(commandClass);
        final CommandId commandId = command.getContext().getCommandId();
        try {
            dispatcher.dispatch(command);
        } catch (Exception e) {
            problemLog.errorDispatching(e, command);
            commandStatusService.setToError(commandId, e);
        }
    }

    private void invokeHandler(Message msg, CommandContext context) {
        final CommandClass commandClass = CommandClass.of(msg);
        final CommandHandler handler = handlerRegistry.getHandler(commandClass);
        final CommandId commandId = context.getCommandId();
        try {
            handler.handle(msg, context);
            commandStatusService.setOk(commandId);
        } catch (InvocationTargetException e) {
            onHandlerException(msg, commandId, e);
        }
    }

    private void onHandlerException(Message msg, CommandId commandId, InvocationTargetException e) {
        final Throwable cause = e.getCause();
        //noinspection ChainOfInstanceofChecks
        if (cause instanceof Exception) {
            final Exception exception = (Exception) cause;
            problemLog.errorHandling(exception, msg, commandId);
            commandStatusService.setToError(commandId, exception);
        } else if (cause instanceof FailureThrowable){
            final FailureThrowable failure = (FailureThrowable) cause;
            problemLog.failureHandling(failure, msg, commandId);
            commandStatusService.setToFailure(commandId, failure);
        } else {
            problemLog.errorHandlingUnknown(cause, msg, commandId);
            commandStatusService.setToError(commandId, Errors.fromThrowable(cause));
        }
    }

    private void schedule(Command command) {
        if (scheduler != null) {
            scheduler.schedule(command);
        } else {
            throw new IllegalStateException(
                    "Scheduled commands are not supported by this command bus: scheduler is not set.");
        }
    }

    /**
     * Sets the multitenancy status of the {@code CommandBus}.
     *
     * <p>A {@code CommandBus} is multi-tenant if its {@code BoundedContext} is multi-tenant.
     */
    @Internal
    public void setMultitenant(boolean multitenant) {
        this.isMultitenant = multitenant;
    }

    @VisibleForTesting
    /* package */ void setProblemLog(ProblemLog problemLog) {
        this.problemLog = problemLog;
    }

    /**
     * Convenience wrapper for logging errors and warnings.
     */
    /* package */ static class ProblemLog {
        /* package */ void errorDispatching(Exception exception, Command command) {
            final String msg = formatCommandTypeAndId("Unable to dispatch command `%s` (ID: `%s`)", command);
            log().error(msg, exception);
        }

        /* package */ void errorHandling(Exception exception, Message commandMessage, CommandId commandId) {
            final String msg = formatMessageTypeAndId("Exception while handling command `%s` (ID: `%s`)",
                    commandMessage, commandId);
            log().error(msg, exception);
        }

        /* package */ void failureHandling(FailureThrowable flr, Message commandMessage, CommandId commandId) {
            final String msg = formatMessageTypeAndId("Business failure occurred when handling command `%s` (ID: `%s`)",
                    commandMessage, commandId);
            log().warn(msg, flr);
        }

        /* package */ void errorHandlingUnknown(Throwable throwable, Message commandMessage, CommandId commandId) {
            final String msg = formatMessageTypeAndId("Throwable encountered when handling command `%s` (ID: `%s`)",
                    commandMessage, commandId);
            log().error(msg, throwable);
        }
    }

    /**
     * Stores a command to the storage.
     *
     * @param command a command to store
     */
    @VisibleForTesting
    /* package */ void store(Command command) {
        commandStore.store(command);
    }

    private boolean isDispatcherRegistered(CommandClass cls) {
        final boolean result = dispatcherRegistry.hasDispatcherFor(cls);
        return result;
    }

    private boolean isHandlerRegistered(CommandClass cls) {
        final boolean result = handlerRegistry.handlerRegistered(cls);
        return result;
    }

    private CommandDispatcher getDispatcher(CommandClass commandClass) {
        return dispatcherRegistry.getDispatcher(commandClass);
    }

    @Override
    public void close() throws Exception {
        dispatcherRegistry.unregisterAll();
        handlerRegistry.unregisterAll();
        commandStore.close();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * Constructs a Command Bus.
     */
    public static class Builder {

        private CommandStore commandStore;
        @Nullable
        private CommandScheduler scheduler;

        public CommandBus build() {
            checkNotNull(commandStore, "Command store must be set.");
            final CommandBus commandBus = new CommandBus(this);
            if (scheduler != null) {
                scheduler.setCommandBus(commandBus);
            }
            return commandBus;
        }

        /**
         * Sets a command store, which is required.
         */
        public Builder setCommandStore(CommandStore commandStore) {
            this.commandStore = commandStore;
            return this;
        }

        public CommandStore getCommandStore() {
            return commandStore;
        }

        /**
         * Sets a command scheduler, which is not required.
         */
        public Builder setScheduler(CommandScheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        @Nullable
        public CommandScheduler getScheduler() {
            return scheduler;
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandBus.class);
    }

    /**
     * The logger instance used by {@code CommandBus}.
     */
    @VisibleForTesting
    /* package */ static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
