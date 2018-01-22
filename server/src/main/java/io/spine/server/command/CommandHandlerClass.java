/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.command;

import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.ModelClass;

import java.util.Set;

/**
 * Provides message handling information on a command handler class.
 *
 * @param <C> the type of command handlers
 * @author Alexander Yevsyukov
 */
@Internal
public final class CommandHandlerClass<C extends CommandHandler>
        extends ModelClass<C>
        implements CommandHandlingClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<CommandClass, CommandHandlerMethod> commands;

    private CommandHandlerClass(Class<? extends C> cls) {
        super(cls);
        this.commands = new MessageHandlerMap<>(cls, CommandHandlerMethod.factory());
    }

    public static ModelClass<?> of(Class<? extends CommandHandler> cls) {
        return new CommandHandlerClass<>(cls);
    }

    CommandHandlerMethod getHandler(CommandClass commandClass) {
        return commands.getMethod(commandClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<CommandClass> getCommands() {
        return commands.getMessageClasses();
    }
}
