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

import io.spine.server.command.Commander;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides information on message handling for a class of {@link Commander}s.
 *
 * @param <C> the type of commanders
 * @author Alexander Yevsyukov
 */
public final class CommanderClass<C extends Commander>
        extends AbstractCommandHandlingClass<C, CommandSubstMethod> {

    private static final long serialVersionUID = 0L;

    private CommanderClass(Class<C> value) {
        //TODO:2018-07-25:alexander.yevsyukov: A commander may have not only Subst methods!
        super(value, CommandSubstMethod.factory());
    }

    public static <C extends Commander>
    CommanderClass<C> asCommanderClass(Class<C> cls) {
        checkNotNull(cls);
        CommanderClass<C> result = (CommanderClass<C>)
                get(cls, CommanderClass.class, () -> new CommanderClass<>(cls));
        return result;
    }
}
