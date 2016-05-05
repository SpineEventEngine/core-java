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
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.failure.FailureThrowable;
import org.spine3.server.type.CommandClass;
import org.spine3.server.validate.MessageValidator;
import org.spine3.validate.options.ConstraintViolation;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
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
    
    private ProblemLog problemLog = new ProblemLog();
    private final CommandStatusService commandStatusService;
    private MessageValidator messageValidator;

    private CommandBus(Builder builder) {
        commandStore = builder.getCommandStore();
        scheduler = builder.getScheduler();
        if (scheduler != null) {
            scheduler.setPostFunction(newPostFunction());
        }
        commandStatusService = new CommandStatusService(commandStore);
        messageValidator = new MessageValidator();
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
     * Verifies that the command message can be posted to this {@code CommandBus}.
     *
     * <p>A command can be posted if its message has either dispatcher or handler registered with
     * this {@code CommandBus}.
     * The message also must satisfy validation constraints defined in its Protobuf type.
     *
     * @param command a command message to check
     * @return the result of {@link Responses#ok()} if the command is valid and supported,
     *         {@link CommandValidation#invalidCommand(Message, List)} or
     *         {@link CommandValidation#unsupportedCommand(Message)} otherwise
     */
    public Response validate(Message command) {
        final CommandClass commandClass = CommandClass.of(command);
        if (isUnsupportedCommand(commandClass)) {
            return unsupportedCommand(command);
        }
        final List<ConstraintViolation> violations = messageValidator.validate(command);
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
     * @param command the command to be processed
     * @throws UnsupportedCommandException if there is neither handler nor dispatcher registered for
     *                                     the class of the passed command
     */
    public void post(Command command) {
        checkNotDefault(command);
        if (isScheduled(command)) {
            schedule(command);
            return;
        }
        store(command);
        final CommandClass commandClass = CommandClass.of(command);
        if (isDispatcherRegistered(commandClass)) {
            dispatch(command);
            return;
        }
        if (isHandlerRegistered(commandClass)) {
            final Message message = getMessage(command);
            final CommandContext context = command.getContext();
            invokeHandler(message, context);
            return;
        }
        throw new UnsupportedCommandException(getMessage(command));
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
        final CommandHandler handler = getHandler(commandClass);
        final CommandId commandId = context.getCommandId();
        try {
            handler.handle(msg, context);
            commandStatusService.setOk(commandId);
        } catch (InvocationTargetException e) {
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
    }

    private CommandHandler getHandler(CommandClass commandClass) {
        final CommandHandler handler = handlerRegistry.getHandler(commandClass);
        return handler;
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

    @VisibleForTesting
    /* package */ void setMessageValidator(MessageValidator messageValidator) {
        this.messageValidator = messageValidator;
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

    private Function<Command, Command> newPostFunction() {
        final Function<Command, Command> function = new Function<Command, Command>() {
            @Nullable
            @Override
            public Command apply(@Nullable Command command) {
                //noinspection ConstantConditions
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
        @Nullable
        private CommandScheduler scheduler;

        public CommandBus build() {
            checkNotNull(commandStore, "Command store must be set.");
            final CommandBus commandBus = new CommandBus(this);
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
}
