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
import com.google.common.collect.Sets;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.Internal;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandContext.Schedule;
import org.spine3.base.CommandId;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.Namespace;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.BoundedContext;
import org.spine3.server.Statuses;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.failure.FailureThrowable;
import org.spine3.server.type.CommandClass;
import org.spine3.time.Interval;
import org.spine3.util.Environment;
import org.spine3.validate.ConstraintViolation;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.add;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.*;
import static org.spine3.protobuf.Timestamps.isLaterThan;
import static org.spine3.server.command.error.CommandExpiredException.commandExpiredError;
import static org.spine3.time.Intervals.between;
import static org.spine3.time.Intervals.toDuration;
import static org.spine3.validate.Validate.isDefault;

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

    private final CommandScheduler scheduler;

    private final ProblemLog problemLog;

    /**
     * Is true, if the {@code BoundedContext} (to which this {@code CommandBus} belongs) is multi-tenant.
     *
     * <p>If the {@code CommandBus} is multi-tenant, the commands posted must have the {@code namespace} attribute
     * defined.
     */
    private boolean isMultitenant;

    /**
     * Creates a new instance.
     *
     * @param commandStore a store to save commands
     * @return a new instance
     */
    public static CommandBus newInstance(CommandStore commandStore) {
        final CommandScheduler scheduler = createCommandScheduler();
        final ProblemLog log = new ProblemLog();
        final CommandBus commandBus = new CommandBus(checkNotNull(commandStore), scheduler, log);
        return commandBus;
    }

    /**
     * Creates a new instance.
     *
     * @param commandStore a store to save commands
     * @param scheduler a command scheduler
     * @param problemLog a problem logger
     */
    @VisibleForTesting
    /* package */ CommandBus(CommandStore commandStore, CommandScheduler scheduler, ProblemLog problemLog) {
        this.commandStore = checkNotNull(commandStore);
        this.commandStatusService = new CommandStatusService(commandStore);
        this.scheduler = scheduler;
        this.problemLog = problemLog;
        //TODO:2016-05-27:alexander.yevsyukov: This must be handled in parallel. This call blocks the start.
        rescheduleCommands();
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
            scheduleAndStore(command, responseObserver);
            return;
        }
        commandStore.store(command);
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

    /**
     * Returns {@code true} if a command is valid, {@code false} otherwise.
     */
    private boolean handleValidation(Command command, StreamObserver<Response> responseObserver) {
        final Namespace namespace = command.getContext().getNamespace();
        if (isMultitenant() && isDefault(namespace)) {
            final CommandException noNamespace = InvalidCommandException.onMissingNamespace(command);
            storeWithError(command, noNamespace);
            responseObserver.onError(Statuses.invalidArgumentWithCause(noNamespace));
            return false; // and nothing else matters
        }
        final List<ConstraintViolation> violations = CommandValidator.getInstance().validate(command);
        if (!violations.isEmpty()) {
            final CommandException invalidCommand = InvalidCommandException.onConstraintViolations(command, violations);
            storeWithError(command, invalidCommand);
            responseObserver.onError(Statuses.invalidArgumentWithCause(invalidCommand));
            return false;
        }
        return true;
    }

    private void storeWithError(Command command, CommandException exception) {
        commandStore.store(command, exception.getError());
    }

    private void handleUnsupported(Command command, StreamObserver<Response> responseObserver) {
        final CommandException unsupported = new UnsupportedCommandException(command);
        storeWithError(command, unsupported);
        responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
    }

    private void scheduleAndStore(Command command, StreamObserver<Response> responseObserver) {
        scheduler.schedule(command);
        commandStore.store(command, SCHEDULED);
        responseObserver.onNext(Responses.ok());
        responseObserver.onCompleted();
    }

    private void rescheduleCommands() {
        final Iterator<Command> commands = commandStore.iterator(SCHEDULED);
        while (commands.hasNext()) {
            final Command command = commands.next();
            final Timestamp now = getCurrentTime();
            final Timestamp timeToPost = getTimeToPost(command);
            if (isLaterThan(now, /*than*/ timeToPost)) {
                onScheduledCommandExpired(command);
            } else {
                final Interval interval = between(now, timeToPost);
                final Duration newDelay = toDuration(interval);
                final Command commandUpdated = setSchedule(command, newDelay, now);
                scheduler.schedule(commandUpdated);
            }
        }
    }

    private static Timestamp getTimeToPost(Command command) {
        final Schedule schedule = command.getContext().getSchedule();
        final Timestamp timeToPost = add(schedule.getSchedulingTime(), schedule.getDelay());
        return timeToPost;
    }

    private void onScheduledCommandExpired(Command command) {
        // We cannot post this command because there is no handler/dispatcher registered yet.
        // Also, posting it can be undesirable.
        final Message msg = getMessage(command);
        final CommandId id = getId(command);
        problemLog.errorExpiredCommand(msg, id);
        setToError(id, commandExpiredError(msg));
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

    /**
     * Obtains the view {@code Set} of commands that are known to this {@code CommandBus}.
     *
     * <p>This set is changed when command dispatchers or handlers are registered or un-registered.
     */
    public Set<CommandClass> getSupportedCommandClasses() {
        final Set<CommandClass> result = Sets.union(dispatcherRegistry.getCommandClasses(),
                                                    handlerRegistry.getCommandClasses());
        return result;
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
        final CommandId commandId = getId(command);
        try {
            dispatcher.dispatch(command);
        } catch (Exception e) {
            problemLog.errorDispatching(e, command);
            setErrorStatus(commandId, e);
        }
    }

    private void invokeHandler(Message msg, CommandContext context) {
        final CommandClass commandClass = CommandClass.of(msg);
        final CommandHandler handler = handlerRegistry.getHandler(commandClass);
        final CommandId commandId = context.getCommandId();
        try {
            handler.handle(msg, context);
            setOkStatus(commandId);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            onHandlerException(msg, commandId, cause);
        }
    }

    private void onHandlerException(Message msg, CommandId commandId, Throwable cause) {
        //noinspection ChainOfInstanceofChecks
        if (cause instanceof Exception) {
            final Exception exception = (Exception) cause;
            problemLog.errorHandling(exception, msg, commandId);
            setErrorStatus(commandId, exception);
        } else if (cause instanceof FailureThrowable){
            final FailureThrowable failure = (FailureThrowable) cause;
            problemLog.failureHandling(failure, msg, commandId);
            setFailureStatus(commandId, failure);
        } else {
            problemLog.errorHandlingUnknown(cause, msg, commandId);
            final Error error = Errors.fromThrowable(cause);
            setToError(commandId, error);
        }
    }

    private void setOkStatus(CommandId commandId) {
        commandStatusService.setOk(commandId);
    }

    private void setToError(CommandId commandId, Error error) {
        commandStatusService.setToError(commandId, error);
    }

    private void setErrorStatus(CommandId commandId, Exception exception) {
        commandStatusService.setToError(commandId, exception);
    }

    private void setFailureStatus(CommandId commandId, FailureThrowable failure) {
        commandStatusService.setToFailure(commandId, failure);
    }

    /**
     * Sets the multitenancy status of the {@link CommandBus}.
     *
     * <p>A {@link CommandBus} is multi-tenant if its {@link BoundedContext} is multi-tenant.
     */
    @Internal /** Is used by {@link BoundedContext} to set its multitenancy status. */
    public void setMultitenant(boolean isMultitenant) {
        this.isMultitenant = isMultitenant;
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

        /* package */ void errorExpiredCommand(Message commandMsg, CommandId id) {
            final String msg = formatMessageTypeAndId("Expired scheduled command `%s` (ID: `%s`).", commandMsg, id);
            log().error(msg);
        }
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

    private static CommandScheduler createCommandScheduler() {
        if (Environment.getInstance().isAppEngine()) {
            log().error("CommandScheduler for AppEngine is not implemented yet.");
            // TODO:2016-05-13:alexander.litus: load a CommandScheduler for AppEngine dynamically when it is implemented.
            // Return this one for now.
            return new ExecutorCommandScheduler();
        } else {
            return new ExecutorCommandScheduler();
        }
    }

    @Override
    public void close() throws Exception {
        dispatcherRegistry.unregisterAll();
        handlerRegistry.unregisterAll();
        commandStore.close();
        scheduler.shutdown();
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
