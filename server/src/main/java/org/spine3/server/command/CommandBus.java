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

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.*;
import static org.spine3.server.command.CommandValidation.invalidCommand;
import static org.spine3.server.command.CommandValidation.unsupportedCommand;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public class CommandBus implements AutoCloseable {

    private final DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();
    private final HandlerRegistry handlerRegistry = new HandlerRegistry();

    private final CommandStore commandStore;

    private final CommandStatusService commandStatusService;

    private ProblemLog problemLog = new ProblemLog();

    @CheckReturnValue
    public static CommandBus create(CommandStore store) {
        return new CommandBus(checkNotNull(store));
    }

    protected CommandBus(CommandStore commandStore) {
        this.commandStore = commandStore;
        this.commandStatusService = new CommandStatusService(commandStore);
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
        checkNotNull(command);
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
     * @param request the command request to be processed
     * @throws UnsupportedCommandException if there is neither handler nor dispatcher registered for
     *                                     the class of the passed command
     */
    public void post(Command request) {
        checkNotNull(request);

        store(request);

        final CommandClass commandClass = CommandClass.of(request);

        if (isDispatcherRegistered(commandClass)) {
            dispatch(request);
            return;
        }

        if (isHandlerRegistered(commandClass)) {
            final Message command = getMessage(request);
            final CommandContext context = request.getContext();
            invokeHandler(command, context);
            return;
        }

        throw new UnsupportedCommandException(getMessage(request));
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

    private List<Event> invokeHandler(Message msg, CommandContext context) {
        final CommandClass commandClass = CommandClass.of(msg);
        final CommandHandlerMethod method = getHandler(commandClass);
        List<Event> result = Collections.emptyList();
        try {
            result = method.invoke(msg, context);
            //TODO:2016-04-11:alexander.yevsyukov: The command handler is to post events to EventBus on its own. How?

            commandStatusService.setOk(context.getCommandId());

        } catch (InvocationTargetException e) {
            final CommandId commandId = context.getCommandId();
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

        return result;
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

    @VisibleForTesting
    /* package */ ProblemLog getProblemLog() {
        return problemLog;
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

    @Override
    public void close() throws Exception {
        dispatcherRegistry.unregisterAll();
        handlerRegistry.unregisterAll();
        commandStore.close();
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
