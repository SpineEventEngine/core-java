/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.server.error.CommandHandlerAlreadyRegisteredException;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.internal.CommandHandlerMethod;

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
public class CommandDispatcher {

    private final Map<CommandClass, CommandHandlerMethod> handlersByClass = Maps.newConcurrentMap();

    /**
     * @return singleton instance of {@code CommandDispatcher}
     */
    @CheckReturnValue
    public static CommandDispatcher getInstance() {
        return Singleton.INSTANCE.value;
    }

    /**
     * Registers the passed object of many commands in the dispatcher.
     *
     * @param object a {@code non-null} object
     */
    public void register(Object object) {
        checkNotNull(object);
        final Map<CommandClass, CommandHandlerMethod> handlers = CommandHandlerMethod.scan(object);
        registerMap(handlers);
    }

    public void unregister(Object object) {
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

    /**
     * Directs a command to the corresponding handler.
     *
     * @param command the command to be processed
     * @param context the context of the command
     * @return a list of the event records as the result of handling the command
     * @throws InvocationTargetException   if an exception occurs during command handling
     * @throws UnsupportedCommandException if there is no handler registered for the class of the passed command
     */
    @CheckReturnValue
    List<EventRecord> dispatch(Message command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        final CommandClass commandClass = CommandClass.of(command);
        if (!handlerRegistered(commandClass)) {
            throw new UnsupportedCommandException(command);
        }

        final CommandHandlerMethod method = getHandler(commandClass);
        final List<EventRecord> result = method.invoke(command, context);
        return result;
    }

    private void putAll(Map<CommandClass, CommandHandlerMethod> subscribers) {
        handlersByClass.putAll(subscribers);
    }

    @CheckReturnValue
    public CommandHandlerMethod getHandler(CommandClass cls) {
        return handlersByClass.get(cls);
    }

    @CheckReturnValue
    public boolean handlerRegistered(CommandClass cls) {
        return handlersByClass.containsKey(cls);
    }


    private enum Singleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final CommandDispatcher value = new CommandDispatcher();
    }

}
