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

package org.spine3.server.type;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.Commands;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object for class type references.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandClass extends MessageClass {

    private CommandClass(Class<? extends Message> value) {
        super(value);
    }

    /**
     * Creates a new instance for the passed class value.
     *
     * @param value class reference
     * @return new instance
     */
    public static CommandClass of(Class<? extends Message> value) {
        return new CommandClass(checkNotNull(value));
    }

    /**
     * Creates a new instance for the class of the passed command.
     *
     * <p>If an instance of {@link Command} (which implements {@code Message}) is passed to this method,
     * enclosing command message will be un-wrapped to determine the class of the command.
     *
     * @param command a command for which to get the class
     * @return new instance
     */
    public static CommandClass of(Message command) {
        checkNotNull(command);
        if (command instanceof Command) {
            final Command commandRequest = (Command) command;
            final Message enclosed = Commands.getMessage(commandRequest);
            return of(enclosed.getClass());
        }
        final CommandClass result = of(command.getClass());
        return result;
    }

    /**
     * Creates immutable set of {@code CommandClass} from the passed set.
     */
    public static ImmutableSet<CommandClass> setOf(Set<Class<? extends Message>> classes) {
        final ImmutableSet.Builder<CommandClass> builder = ImmutableSet.builder();
        for (Class<? extends Message> cls : classes) {
            builder.add(of(cls));
        }
        return builder.build();
    }

    /**
     * Creates immutable set of {@code CommandClass} from the passed classes.
     */
    @SafeVarargs
    public static ImmutableSet<CommandClass> setOf(Class<? extends Message>... classes) {
        final ImmutableSet.Builder<CommandClass> builder = ImmutableSet.builder();
        for (Class<? extends Message> cls : classes) {
            builder.add(of(cls));
        }
        return builder.build();
    }
}
