/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import org.spine3.server.command.error.CommandHandlerAlreadyRegisteredException;
import org.spine3.server.reflect.CommandHandlerMethod;
import org.spine3.server.reflect.HandlerMethod;
import org.spine3.server.reflect.MethodMap;
import org.spine3.server.reflect.MethodRegistry;
import org.spine3.server.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.command.RegistryUtil.checkNotAlreadyRegistered;

/**
 * The registry of command handlers by command class.
 *
 * <p>There can be only one handler per command class.
 *
 * @author Alexander Yevsyukov
 */
class HandlerRegistry {

    private final Map<CommandClass, CommandHandler> handlers = Maps.newConcurrentMap();

    void register(CommandHandler object) {
        checkNotNull(object);

        final MethodMap<CommandHandlerMethod> handlers = CommandHandlerMethod.scan(object);

        if (handlers.isEmpty()) {
            throw new IllegalArgumentException("No command handler methods found in :" + object);
        }

        registerMap(object, handlers);
    }

    void unregister(CommandHandler object) {
        checkNotNull(object);

        final MethodMap<CommandHandlerMethod> handlers = CommandHandlerMethod.scan(object);
        unregisterMap(handlers);
    }

    Set<CommandClass> getCommandClasses() {
        return handlers.keySet();
    }

    /**
     * Registers the passed handlers with the dispatcher.
     *
     * @param object the command handler
     * @param methods map from command classes to corresponding handlers
     */
    private void registerMap(CommandHandler object, MethodMap<CommandHandlerMethod> methods) {
        checkDuplicates(methods);
        putAll(object, methods);
    }

    private void unregisterMap(MethodMap<CommandHandlerMethod> methods) {
        for (Map.Entry<Class<? extends Message>, CommandHandlerMethod> entry : methods.entrySet()) {
            final CommandClass commandClass = CommandClass.of(entry.getKey());
            if (handlerRegistered(commandClass)) {
                final CommandHandler handler = getHandler(commandClass);
                final CommandHandlerMethod registered = getHandlerMethod(handler.getClass(), commandClass.value());
                final CommandHandlerMethod passed = entry.getValue();
                if (registered.equals(passed)) {
                    removeFor(commandClass);
                }
            }
        }
    }

    private void removeFor(CommandClass commandClass) {
        handlers.remove(commandClass);
    }

    private void checkDuplicates(MethodMap<CommandHandlerMethod> handlers) {
        for (Map.Entry<Class<? extends Message>, CommandHandlerMethod> entry : handlers.entrySet()) {
            final CommandClass commandClass = CommandClass.of(entry.getKey());

            if (handlerRegistered(commandClass)) {
                final CommandHandler handler = getHandler(commandClass);
                final HandlerMethod alreadyRegistered = getHandlerMethod(handler.getClass(), commandClass.value());
                throw new CommandHandlerAlreadyRegisteredException(commandClass,
                        alreadyRegistered.getFullName(),
                        entry.getValue().getFullName());
            }
        }
    }

    private static CommandHandlerMethod getHandlerMethod(Class<? extends CommandHandler> clazz,
                                                         Class<? extends Message> commandClass) {
        return MethodRegistry.getInstance()
                             .get(clazz, commandClass, CommandHandlerMethod.factory());
    }

    @CheckReturnValue
    boolean handlerRegistered(CommandClass cls) {
        return handlers.containsKey(cls);
    }

    CommandHandler getHandler(CommandClass cls) {
        return handlers.get(cls);
    }

    private void putAll(CommandHandler object, MethodMap<CommandHandlerMethod> methods) {
        for (Class<? extends Message> commandClass : methods.keySet()) {
            handlers.put(CommandClass.of(commandClass), object);
        }
    }

    void unregisterAll() {
        for (CommandClass commandClass : handlers.keySet()) {
            removeFor(commandClass);
        }
    }

    /**
     * Ensures that no handlers already registered for the command classes of the passed dispatcher.
     *
     * @param dispatcher the dispatcher to check
     * @throws IllegalArgumentException if one or more classes have registered handlers
     */
    @SuppressWarnings("InstanceMethodNamingConvention") // prefer longer name here for clarity.
    void checkNoHandlersRegisteredForCommandsOf(CommandDispatcher dispatcher) {
        final Set<CommandClass> alreadyRegistered = Sets.newHashSet();
        final Set<CommandClass> commandClasses = dispatcher.getCommandClasses();
        for (CommandClass commandClass : commandClasses) {
            if (handlerRegistered(commandClass)) {
                alreadyRegistered.add(commandClass);
            }
        }

        checkNotAlreadyRegistered(
            alreadyRegistered,
            dispatcher,
            "Cannot register dispatcher %s for command class %s which already has registered handler.",
            "Cannot register dispatcher %s for command classes (%s) which already have registered handlers."
        );
    }
}
