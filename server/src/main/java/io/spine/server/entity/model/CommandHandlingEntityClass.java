/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity.model;

import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.command.model.CommandHandlerSignature;
import io.spine.server.command.model.CommandHandlingClass;
import io.spine.server.entity.Entity;
import io.spine.server.model.MessageHandlerMap;

import java.util.Set;

/**
 * Abstract base for entity classes that handle commands.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public abstract class CommandHandlingEntityClass<E extends Entity>
        extends EntityClass<E>
        implements CommandHandlingClass {

    private static final long serialVersionUID = 0L;
    private final MessageHandlerMap<CommandClass, CommandHandlerMethod> commands;

    protected CommandHandlingEntityClass(Class<E> cls) {
        super(cls);
        this.commands = MessageHandlerMap.create(cls, new CommandHandlerSignature());
    }

    @Override
    public Set<CommandClass> getCommands() {
        return commands.getMessageClasses();
    }

    public boolean handlesCommand(CommandClass commandClass) {
        return commands.containsClass(commandClass);
    }

    @Override
    public CommandHandlerMethod getHandler(CommandClass commandClass) {
        return commands.getSingleMethod(commandClass);
    }
}
