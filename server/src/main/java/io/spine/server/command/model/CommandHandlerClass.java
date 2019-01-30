/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import io.spine.base.EventMessage;
import io.spine.server.command.AbstractCommandHandler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides message handling information on a command handler class.
 *
 * @param <C> the type of command handlers
 */
public final class CommandHandlerClass<C extends AbstractCommandHandler>
        extends AbstractCommandHandlingClass<C, EventMessage, CommandHandlerMethod> {

    private static final long serialVersionUID = 0L;

    private CommandHandlerClass(Class<C> cls) {
        super(cls, new CommandHandlerSignature());
    }

    /**
     * Obtains command handler class for the passed raw class.
     */
    public static <C extends AbstractCommandHandler>
    CommandHandlerClass<C> asCommandHandlerClass(Class<C> cls) {
        checkNotNull(cls);
        CommandHandlerClass<C> result = (CommandHandlerClass<C>)
                get(cls, CommandHandlerClass.class, () -> new CommandHandlerClass<>(cls));
        return result;
    }
}
