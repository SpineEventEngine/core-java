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
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.CommandRequest;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.protobuf.Messages;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.error.CommandHandlerAlreadyRegisteredException;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.internal.CommandHandlingObject;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public class CommandDispatcher implements AutoCloseable {

    private final HandlerRegistry handlerRegistry = new HandlerRegistry();

    private final CommandStore commandStore;

    @CheckReturnValue
    public static CommandDispatcher create(CommandStore store) {
        return new CommandDispatcher(store);
    }

    protected CommandDispatcher(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

    /**
     * Registers the passed object as a handler of commands.
     *
     * <p>The passed object must be one of the following:
     * <ul>
     *     <li>An object of the class derived from {@link AggregateRepository}.</li>
     *     <li>An object of the class derived from {@link ProcessManagerRepository}.</li>
     * </ul>
     *
     * @param object a {@code non-null} object of the required type
     * @throws IllegalArgumentException if the object is not of required class
     */
    void register(CommandHandlingObject object) {
        handlerRegistry.register(object);
    }

    void unregister(CommandHandlingObject object) {
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
     * The {@code Registry} contains handlers for all command classes processed by the {@code BoundedContext}
     * to which this {@code CommandDispatcher} belongs.
     */
    private static class HandlerRegistry {

        private final Map<CommandClass, CommandHandlerMethod> handlersByClass = Maps.newConcurrentMap();

        void register(CommandHandlingObject object) {
            checkNotNull(object);

            final Map<CommandClass, CommandHandlerMethod> handlers = CommandHandlerMethod.scan(object);
            registerMap(handlers);
        }

        void unregister(CommandHandlingObject object) {
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

}
