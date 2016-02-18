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
import org.spine3.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getMessage;

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

    @CheckReturnValue
    public static CommandBus create(CommandStore store) {
        return new CommandBus(checkNotNull(store));
    }

    protected CommandBus(CommandStore commandStore) {
        this.commandStore = commandStore;
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
        final Message message = (command instanceof Command) ?
                getMessage((Command) command) :
                command;

        if (dispatcherRegistry.hasDispatcherFor(message)) {
            return Responses.ok();
        }

        if (handlerRegistry.hasHandlerFor(message)) {
            return Responses.ok();
        }

        //TODO:2015-12-16:alexander.yevsyukov: Implement command validation for completeness of commands.
        // Presumably, it would be CommandValidator<Class<? extends Message> which would be exposed by
        // corresponding Aggregates or ProcessManagers, and then contributed to validator registry.

        return CommandValidation.unsupportedCommand(message);
    }

    /**
     * Directs a command request to the corresponding handler.
     *
     * @param request the command request to be processed
     * @return a list of the events as the result of handling the command
     * @throws UnsupportedCommandException if there is no handler or dispatcher registered for
     *                                     the class of the passed command
     */
    public List<Event> post(Command request) {

        //TODO:2016-01-24:alexander.yevsyukov: Do not return value.

        checkNotNull(request);

        store(request);

        final CommandClass commandClass = CommandClass.of(request);

        if (dispatcherRegistered(commandClass)) {
            return dispatch(request);
        }

        if (handlerRegistered(commandClass)) {
            final Message command = getMessage(request);
            final CommandContext context = request.getContext();
            return invokeHandler(command, context);
        }

        //TODO:2016-01-24:alexander.yevsyukov: Unify exceptions with messages sent in Response.
        throw new UnsupportedCommandException(getMessage(request));
    }

    private List<Event> dispatch(Command command) {
        final CommandClass commandClass = CommandClass.of(command);
        final CommandDispatcher dispatcher = getDispatcher(commandClass);
        List<Event> result = Collections.emptyList();
        try {
            result = dispatcher.dispatch(command);
        } catch (Exception e) {
            final CommandId commandId = command.getContext().getCommandId();
            log().error("Unable to dispatch command with ID: " + commandId.getUuid(), e);

            updateCommandStatus(commandId, e);
        }
        return result;
    }

    /* package */ final List<Event> invokeHandler(Message command, CommandContext context) {
        final CommandClass commandClass = CommandClass.of(command);
        final CommandHandlerMethod method = getHandler(commandClass);
        List<Event> result = Collections.emptyList();
        try {
            result = method.invoke(command, context);

            setCommandStatusOk(context.getCommandId());

        } catch (InvocationTargetException e) {
            final CommandId commandId = context.getCommandId();
            final String commandIdStr = commandId.getUuid();
            final Throwable cause = e.getCause();

            //noinspection ChainOfInstanceofChecks
            if (cause instanceof Exception) {
                final Exception exception = (Exception) cause;
                log().error(String.format("Exception while handling command ID: `%s`" , commandIdStr), e);
                updateCommandStatus(commandId, exception);
            } else if (cause instanceof FailureThrowable){
                final FailureThrowable failure = (FailureThrowable) cause;
                log().warn(
                        String.format("Business failure ocurred when handling command with ID: `%s`", commandIdStr),
                        failure);
                updateCommandStatus(commandId, failure);
            } else {
                log().error(
                        String.format("Throwable encountered when handling command with ID: `%s`", commandIdStr),
                        cause);
                updateCommandStatus(commandId, Errors.fromThrowable(cause));
            }
        }

        return result;
    }

    private void setCommandStatusOk(CommandId commandId) {
        commandStore.setCommandStatusOk(commandId);
    }

    private void updateCommandStatus(CommandId commandId, Exception exception) {
        commandStore.updateStatus(commandId, exception);
    }

    private void updateCommandStatus(CommandId commandId, FailureThrowable failure) {
        commandStore.updateStatus(commandId, failure.toMessage());
    }

    private void updateCommandStatus(CommandId commandId, org.spine3.base.Error error) {
        commandStore.updateStatus(commandId, error);
    }

    private void store(Command request) {
        commandStore.store(request);
    }

    private boolean dispatcherRegistered(CommandClass cls) {
        final boolean result = dispatcherRegistry.hasDispatcherFor(cls);
        return result;
    }

    private boolean handlerRegistered(CommandClass cls) {
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
