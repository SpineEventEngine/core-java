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

import com.google.common.collect.Sets;
import org.spine3.server.type.CommandClass;

import java.util.Set;

/**
 * Manages registration, unregistration, and finding and caching endpoints for commands.
 *
 * @author Alexander Yevsyukov
 */
class CommandEndpointManager {

    private final DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();

    private final HandlerRegistry handlerRegistry = new HandlerRegistry();

    Set<CommandClass> getSupportedCommandClasses() {
        final Set<CommandClass> result = Sets.union(dispatcherRegistry.getCommandClasses(),
                                                    handlerRegistry.getCommandClasses());
        return result;
    }

    void register(CommandDispatcher dispatcher) {
        handlerRegistry.checkNoHandlersRegisteredForCommandsOf(dispatcher);
        dispatcherRegistry.register(dispatcher);
    }

    void undregister(CommandDispatcher dispatcher) {
        dispatcherRegistry.unregister(dispatcher);
    }

    void register(CommandHandler handler) {
        dispatcherRegistry.checkNoDispatchersRegisteredForCommandsOf(handler);
        handlerRegistry.register(handler);
    }

    void unregister(CommandHandler handler) {
        handlerRegistry.unregister(handler);
    }

    boolean isSupportedCommand(CommandClass commandClass) {
        final boolean dispatcherRegistered = dispatcherRegistry.hasDispatcherFor(commandClass);
        final boolean handlerRegistered = handlerRegistry.handlerRegistered(commandClass);
        final boolean isSupported = dispatcherRegistered || handlerRegistered;
        return isSupported;
    }

    boolean hasDispatcherFor(CommandClass commandClass) {
        return dispatcherRegistry.hasDispatcherFor(commandClass);
    }

    boolean handlerRegistered(CommandClass commandClass) {
        return handlerRegistry.handlerRegistered(commandClass);
    }

    CommandDispatcher getDispatcher(CommandClass commandClass) {
        return dispatcherRegistry.getDispatcher(commandClass);
    }

    CommandHandler getHandler(CommandClass commandClass) {
        return handlerRegistry.getHandler(commandClass);
    }

    void unregisterAll() {
        dispatcherRegistry.unregisterAll();
        handlerRegistry.unregisterAll();
    }
}
