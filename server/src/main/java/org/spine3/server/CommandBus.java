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
package org.spine3.server;

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.CommandRequest;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.protobuf.Messages;
import org.spine3.server.error.CommandHandlerAlreadyRegisteredException;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

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

    void register(CommandDispatcher dispatcher) {
        dispatcherRegistry.register(dispatcher);
    }

    void unregister(CommandDispatcher dispatcher) {
        dispatcherRegistry.unregister(dispatcher);
    }

    /**
     * Registers the passed object as a handler of commands.
     *
     * @param object a {@code non-null} object of the required type
     * @throws IllegalArgumentException if the object does not have command handling methods
     */
    void register(CommandHandler object) {
        handlerRegistry.register(object);
    }

    /**
     * Unregisters the handler from the command bus.
     *
     * @param object the object to unregister
     */
    void unregister(CommandHandler object) {
        handlerRegistry.unregister(object);
    }

    /**
     * Directs a command request to the corresponding handler.
     *
     * @param request the command request to be processed
     * @return a list of the event records as the result of handling the command
     * @throws InvocationTargetException   if an exception occurs during command handling
     * @throws UnsupportedCommandException if there is no handler registered for the class of the passed command
     */
    @CheckReturnValue
    List<EventRecord> storeAndDispatch(CommandRequest request)
            throws InvocationTargetException {
        checkNotNull(request);

        store(request);

        final Message command = Messages.fromAny(request.getCommand());
        final CommandContext context = request.getContext();

        final CommandClass commandClass = CommandClass.of(command);
        if (!handlerRegistered(commandClass)) {
            throw new UnsupportedCommandException(command);
        }

        final CommandHandlerMethod method = getHandler(commandClass);
        final List<EventRecord> result = method.invoke(command, context);
        return result;
    }

    private void store(CommandRequest request) {
        commandStore.store(request);
    }

    private boolean handlerRegistered(CommandClass cls) {
        final boolean result = handlerRegistry.handlerRegistered(cls);
        return result;
    }

    @CheckReturnValue
    private CommandHandlerMethod getHandler(CommandClass cls) {
        return handlerRegistry.getHandler(cls);
    }

    @Override
    public void close() throws Exception {
        handlerRegistry.unregisterAll();
        commandStore.close();
    }

    public Response validate(Message command) {
        if (!handlerRegistry.hasHandlerFor(command)) {
            return CommandValidation.unsupportedCommand(command);
        }

        //TODO:2015-12-16:alexander.yevsyukov: Implement command validation for completeness of commands.
        // Presumably, it would be CommandValidator<Class<? extends Message> which would be exposed by
        // corresponding Aggregates or ProcessManagers, and then contributed to validator registry.

        return responseOk();
    }

    private static Response responseOk() {
        return Responses.RESPONSE_OK;
    }

    /**
     * The registry of objects dispatching command request to where they are
     * processed.
     */
    private static class DispatcherRegistry {

        private final Map<CommandClass, CommandDispatcher> dispatchers = Maps.newConcurrentMap();

        void register(CommandDispatcher dispatcher) {
            checkNotNull(dispatcher);
            final Set<CommandClass> commandClasses = checkNotEmpty(dispatcher);

            for (CommandClass commandClass : commandClasses) {
                dispatchers.put(commandClass, dispatcher);
            }
        }

        private static Set<CommandClass> checkNotEmpty(CommandDispatcher dispatcher) {
            final Set<CommandClass> commandClasses = dispatcher.getCommandClasses();
            if (commandClasses.isEmpty()) {
                throw new IllegalArgumentException("No command classes are forwarded by this dispatcher: " + dispatcher);
            }
            return commandClasses;
        }

        void unregister(CommandDispatcher dispatcher) {
            checkNotNull(dispatcher);
            final Set<CommandClass> commandClasses = checkNotEmpty(dispatcher);
            for (CommandClass commandClass : commandClasses) {
                final CommandDispatcher registeredDispatcher = dispatchers.get(commandClass);
                if (dispatcher.equals(registeredDispatcher)) {
                    dispatchers.remove(commandClass);
                } else {
                    log().warn("Another dispatcher (%s) found when trying to unregister %s for command class %s",
                            registeredDispatcher, dispatcher, commandClass);
                }
            }
        }
    }

    /**
     * The {@code HandlerRegistry} contains handlers methods for all command classes
     * processed by the {@code BoundedContext} to which this {@code CommandBus} belongs.
     */
    private static class HandlerRegistry {

        private final Map<CommandClass, CommandHandlerMethod> handlersByClass = Maps.newConcurrentMap();

        void register(CommandHandler object) {
            checkNotNull(object);

            final Map<CommandClass, CommandHandlerMethod> handlers = CommandHandlerMethod.scan(object);

            if (handlers.isEmpty()) {
                throw new IllegalArgumentException("No command handler methods found in :" + object);
            }

            registerMap(handlers);
        }

        void unregister(CommandHandler object) {
            checkNotNull(object);

            final Map<CommandClass, CommandHandlerMethod> subscribers = CommandHandlerMethod.scan(object);
            unregisterMap(subscribers);
        }

        /**
         * Registers the passed handlers with the dispatcher.
         *
         * @param handlers map from command classes to corresponding handlers
         */
        private void registerMap(Map<CommandClass, CommandHandlerMethod> handlers) {
            checkDuplicates(handlers);
            putAll(handlers);
        }

        private void unregisterMap(Map<CommandClass, CommandHandlerMethod> handlers) {
            for (Map.Entry<CommandClass, CommandHandlerMethod> entry : handlers.entrySet()) {
                final CommandClass commandClass = entry.getKey();
                if (handlerRegistered(commandClass)) {
                    final CommandHandlerMethod registered = getHandler(commandClass);
                    final CommandHandlerMethod passed = entry.getValue();
                    if (registered.equals(passed)) {
                        removeFor(commandClass);
                    }
                }
            }
        }

        private void removeFor(CommandClass commandClass) {
            handlersByClass.remove(commandClass);
        }

        private void checkDuplicates(Map<CommandClass, CommandHandlerMethod> handlers) {
            for (Map.Entry<CommandClass, CommandHandlerMethod> entry : handlers.entrySet()) {
                final CommandClass commandClass = entry.getKey();

                if (handlerRegistered(commandClass)) {
                    final MessageHandlerMethod alreadyRegistered = getHandler(commandClass);
                    throw new CommandHandlerAlreadyRegisteredException(commandClass,
                            alreadyRegistered.getFullName(),
                            entry.getValue().getFullName());
                }
            }
        }

        @CheckReturnValue
        private boolean handlerRegistered(CommandClass cls) {
            return handlersByClass.containsKey(cls);
        }

        @CheckReturnValue
        private CommandHandlerMethod getHandler(CommandClass cls) {
            return handlersByClass.get(cls);
        }

        private void putAll(Map<CommandClass, CommandHandlerMethod> subscribers) {
            handlersByClass.putAll(subscribers);
        }

        private void unregisterAll() {
            for (CommandClass commandClass : handlersByClass.keySet()) {
                removeFor(commandClass);
            }
        }

        private boolean hasHandlerFor(Message command) {
            final CommandHandlerMethod method = getHandler(CommandClass.of(command));
            return method != null;
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandBus.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
