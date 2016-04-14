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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import org.spine3.server.CommandDispatcher;
import org.spine3.server.CommandHandler;
import org.spine3.server.error.CommandHandlerAlreadyRegisteredException;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.internal.MessageHandlerMethod;
import org.spine3.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The registry of command handlers by command class.
 *
 * <p>There can be only one handler per command class.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class HandlerRegistry {

    private final Map<CommandClass, CommandHandlerMethod> handlersByClass = Maps.newConcurrentMap();

    /* package */ void register(CommandHandler object) {
        checkNotNull(object);

        final Map<CommandClass, CommandHandlerMethod> handlers = CommandHandlerMethod.scan(object);

        if (handlers.isEmpty()) {
            throw new IllegalArgumentException("No command handler methods found in :" + object);
        }

        registerMap(handlers);
    }

    /* package */ void unregister(CommandHandler object) {
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
                final CommandHandlerMethod registered = getHandlerMethod(commandClass);
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
                final MessageHandlerMethod alreadyRegistered = getHandlerMethod(commandClass);
                throw new CommandHandlerAlreadyRegisteredException(commandClass,
                        alreadyRegistered.getFullName(),
                        entry.getValue().getFullName());
            }
        }
    }

    @CheckReturnValue
    /* package */ boolean handlerRegistered(CommandClass cls) {
        return handlersByClass.containsKey(cls);
    }

    /* package */ CommandHandlerMethod getHandlerMethod(CommandClass cls) {
        return handlersByClass.get(cls);
    }

    private void putAll(Map<CommandClass, CommandHandlerMethod> subscribers) {
        handlersByClass.putAll(subscribers);
    }

    /* package */ void unregisterAll() {
        for (CommandClass commandClass : handlersByClass.keySet()) {
            removeFor(commandClass);
        }
    }

    /* package */ boolean hasHandlerFor(Message command) {
        final CommandHandlerMethod method = getHandlerMethod(CommandClass.of(command));
        return method != null;
    }

    /**
     * Ensures that no handlers already registered for the command classes of the passed dispatcher.
     *
     * @param dispatcher the dispatcher to check
     * @throws IllegalArgumentException if one or more classes have registered handlers
     */
    @SuppressWarnings("InstanceMethodNamingConvention") // prefer longer name here for clarity.
    /* package */ void checkNoHandlersRegisteredForCommandsOf(CommandDispatcher dispatcher) {
        final Set<CommandClass> alreadyRegistered = Sets.newHashSet();
        final Set<CommandClass> commandClasses = dispatcher.getCommandClasses();
        for (CommandClass commandClass : commandClasses) {
            if (handlerRegistered(commandClass)) {
                alreadyRegistered.add(commandClass);
            }
        }

        RegistryUtil.checkNotAlreadyRegistered(alreadyRegistered, dispatcher,
                "Cannot register dispatcher %s for command class %s which already has registered handler.",
                "Cannot register dispatcher %s for command classes (%s) which already have registered handlers.");
    }
}
