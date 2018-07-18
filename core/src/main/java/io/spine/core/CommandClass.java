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

package io.spine.core;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.type.MessageClass;

import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object for class type references.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandClass extends MessageClass {

    private static final long serialVersionUID = 0L;

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
     * <p>If an instance of {@link Command} is passed to this method, enclosing command message will
     * be un-wrapped to determine the class of the command.
     *
     * <p>If an instance of {@link Any} is passed, it will be unpacked, and the class of the wrapped
     * message will be used.
     *
     * @param commandOrMessage a command for which to get the class
     * @return new instance
     */
    public static CommandClass of(Message commandOrMessage) {
        Message commandMessage = Commands.ensureMessage(commandOrMessage);
        return of(commandMessage.getClass());
    }

    /** Creates immutable set of {@code CommandClass} from the passed set. */
    public static Set<CommandClass> setOf(Iterable<Class<? extends Message>> classes) {
        ImmutableSet.Builder<CommandClass> builder = ImmutableSet.builder();
        for (Class<? extends Message> cls : classes) {
            builder.add(of(cls));
        }
        return builder.build();
    }

    /** Creates immutable set of {@code CommandClass} from the passed classes. */
    @SafeVarargs
    public static Set<CommandClass> setOf(Class<? extends Message>... classes) {
        return setOf(Arrays.asList(classes));
    }
}
