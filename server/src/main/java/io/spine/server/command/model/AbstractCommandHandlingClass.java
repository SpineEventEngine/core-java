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

package io.spine.server.command.model;

import com.google.errorprone.annotations.Immutable;
import io.spine.core.CommandClass;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.ModelClass;

import java.util.Set;

/**
 * Abstract base for classes providing message handling information of classes that handle commands.
 *
 * @param <C> the type of a command handling class
 * @author Alexander Yevsyukov
 */
@Immutable(containerOf = "H")
public abstract class AbstractCommandHandlingClass<C, H extends CommandAcceptingMethod<?, ?>>
        extends ModelClass<C>
        implements CommandHandlingClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<CommandClass, H> commands;

    AbstractCommandHandlingClass(Class<? extends C> cls, MethodFactory<H, ?> factory) {
        super(cls);
        this.commands = new MessageHandlerMap<>(cls, factory);
    }

    @Override
    public Set<CommandClass> getCommands() {
        return commands.getMessageClasses();
    }

    /** Obtains the handler method for the passed command class. */
    @Override
    public H getHandler(CommandClass commandClass) {
        return commands.getMethod(commandClass);
    }
}
