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

package org.spine3.server.procman;

import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.reflect.MethodMap;

import javax.annotation.CheckReturnValue;

import static org.spine3.server.internal.EventHandlerMethod.PREDICATE;
import static org.spine3.server.internal.EventHandlerMethod.checkModifiers;

/**
 * The registry of method maps for all process manager classes.
 *
 * <p>This registry is used for caching command/event handlers.
 * Process managers register their classes in {@link ProcessManager#init()} method.
 *
 * @author Alexander Litus
 */
/* package */  class Registry {

    private final MethodMap.Registry<ProcessManager> commandHandlers = new MethodMap.Registry<>();
    private final MethodMap.Registry<ProcessManager> eventHandlers = new MethodMap.Registry<>();

    /* package */ void register(Class<? extends ProcessManager> clazz) {
        commandHandlers.register(clazz, CommandHandlerMethod.PREDICATE);
        CommandHandlerMethod.checkModifiers(commandHandlers.get(clazz).values());

        eventHandlers.register(clazz, PREDICATE);
        checkModifiers(eventHandlers.get(clazz).values());
    }

    @CheckReturnValue
    /* package */ boolean contains(Class<? extends ProcessManager> clazz) {
        final boolean result = commandHandlers.contains(clazz) && eventHandlers.contains(clazz);
        return result;
    }

    @CheckReturnValue
    /* package */ MethodMap getCommandHandlers(Class<? extends ProcessManager> clazz) {
        final MethodMap result = commandHandlers.get(clazz);
        return result;
    }

    @CheckReturnValue
    /* package */ MethodMap getEventHandlers(Class<? extends ProcessManager> clazz) {
        final MethodMap result = eventHandlers.get(clazz);
        return result;
    }

    @CheckReturnValue
    /* package */ static Registry getInstance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Registry value = new Registry();
    }
}
