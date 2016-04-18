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
import com.google.common.base.Function;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Errors;
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.CommandDispatcher;
import org.spine3.server.CommandHandler;
import org.spine3.server.FailureThrowable;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.validate.MessageValidator;
import org.spine3.type.CommandClass;
import org.spine3.validation.options.ConstraintViolation;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static org.spine3.base.Commands.*;
import static org.spine3.server.command.CommandValidation.invalidCommand;
import static org.spine3.server.command.CommandValidation.unsupportedCommand;
import static org.spine3.validate.Validate.checkNotDefault;

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

    @Nullable
    private final CommandScheduler scheduler;

    private final CommandStatusHelper commandStatus;
    private ProblemLog problemLog = new ProblemLog();

    private CommandBus(Builder builder) {
        commandStore = builder.getCommandStore();
        scheduler = builder.getScheduler();
        if (scheduler != null) {
            scheduler.setPostFunction(newPostFunction());
        }
        commandStatus = new CommandStatusHelper(commandStore);
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
     * Verifies if the command can be posted to this {@code CommandBus}.
     *
     * <p>The command can be posted if it has either dispatcher or handler registered with
     * the command bus.
     *
     * <p>The method intended to be used with the instances of command messages extracted from {@link Command}.
     *
     * <p>If {@code Command} instance is passed, its message is extracted and validated.
     *
     * @param command the command message to check
     * @return the result of {@link Responses#ok()} if the command is supported,
     *         {@link CommandValidation#unsupportedCommand(Message)} otherwise
     */
    public Response validate(Message command) {
        checkNotDefault(command);
        final CommandClass commandClass = CommandClass.of(command);
        if (isUnsupportedCommand(commandClass)) {
            return unsupportedCommand(command);
        }
        final MessageValidator validator = new MessageValidator();
        final List<ConstraintViolation> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            return invalidCommand(command, violations);
        }
        return Responses.ok();
    }

    private boolean isUnsupportedCommand(CommandClass commandClass) {
        final boolean isUnsupported = !isDispatcherRegistered(commandClass) && !isHandlerRegistered(commandClass);
        return isUnsupported;
    }

    /**
     * Directs a command request to the corresponding handler.
     *
     * @param command the command request to be processed
     * @return a list of the events as the result of handling the command
     * @throws UnsupportedCommandException if there is neither handler nor dispatcher registered for
     *                                     the class of the passed command
     */
    public List<Event> post(Command command) {
        //TODO:2016-01-24:alexander.yevsyukov: Do not return value.
        checkNotDefault(command);
        if (isScheduled(command)) {
            schedule(command);
            return emptyList();
        }
        store(command);
        final CommandClass commandClass = CommandClass.of(command);
        if (isDispatcherRegistered(commandClass)) {
            return dispatch(command);
        }
        if (isHandlerRegistered(commandClass)) {
            final Message message = getMessage(command);
            final CommandContext context = command.getContext();
            return invokeHandler(message, context);
        }
        throw new UnsupportedCommandException(getMessage(command));
    }

    private List<Event> dispatch(Command command) {
        final CommandClass commandClass = CommandClass.of(command);
        final CommandDispatcher dispatcher = getDispatcher(commandClass);
        List<Event> result = emptyList();
        try {
            result = dispatcher.dispatch(command);
        } catch (Exception e) {
            problemLog.errorDispatching(e, command);
            commandStatus.setToError(command.getContext().getCommandId(), e);
        }
        return result;
    }

    private List<Event> invokeHandler(Message msg, CommandContext context) {
        final CommandClass commandClass = CommandClass.of(msg);
        final CommandHandlerMethod method = getHandler(commandClass);
        List<Event> result = emptyList();
        try {
            result = method.invoke(msg, context);
            commandStatus.setOk(context.getCommandId());
        } catch (InvocationTargetException e) {
            final CommandId commandId = context.getCommandId();
            final Throwable cause = e.getCause();
            //noinspection ChainOfInstanceofChecks
            if (cause instanceof Exception) {
                final Exception exception = (Exception) cause;
                problemLog.errorHandling(exception, msg, commandId);
                commandStatus.setToError(commandId, exception);
            } else if (cause instanceof FailureThrowable){
                final FailureThrowable failure = (FailureThrowable) cause;
                problemLog.failureHandling(failure, msg, commandId);
                commandStatus.setToFailure(commandId, failure);
            } else {
                problemLog.errorHandlingUnknown(cause, msg, commandId);
                commandStatus.setToError(commandId, Errors.fromThrowable(cause));
            }
        }
        return result;
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

    @VisibleForTesting
    /* package */ void setProblemLog(ProblemLog problemLog) {
        this.problemLog = problemLog;
    }

    /**
     * The helper class for updating command status.
     */
    private static class CommandStatusHelper {

        private final CommandStore commandStore;

        private CommandStatusHelper(CommandStore commandStore) {
            this.commandStore = commandStore;
        }

        private void setOk(CommandId commandId) {
            commandStore.setCommandStatusOk(commandId);
        }

        private void setToError(CommandId commandId, Exception exception) {
            commandStore.updateStatus(commandId, exception);
        }

        private void setToFailure(CommandId commandId, FailureThrowable failure) {
            commandStore.updateStatus(commandId, failure.toMessage());
        }

        private void setToError(CommandId commandId, org.spine3.base.Error error) {
            commandStore.updateStatus(commandId, error);
        }
    }

    private void store(Command request) {
        commandStore.store(request);
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

    private CommandHandlerMethod getHandler(CommandClass cls) {
        return handlerRegistry.getHandler(cls);
    }

    private Function<Command, Command> newPostFunction() {
        final Function<Command, Command> function = new Function<Command, Command>() {
            @Nullable
            @Override
            public Command apply(@Nullable Command command) {
                if (command == null) {
                    return null;
                }
                post(command);
                return command;
            }
        };
        return function;
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

    /**
     * Constructs a command bus.
     */
    public static class Builder {

        private CommandStore commandStore;
        private CommandScheduler scheduler;

        public CommandBus build() {
            checkNotNull(commandStore, "Command store must be set.");
            final CommandBus commandBus = new CommandBus(this);
            return commandBus;
        }

        public Builder setCommandStore(CommandStore commandStore) {
            this.commandStore = commandStore;
            return this;
        }

        public CommandStore getCommandStore() {
            return commandStore;
        }

        public Builder setScheduler(CommandScheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        @Nullable
        public CommandScheduler getScheduler() {
            return scheduler;
        }
    }
}
