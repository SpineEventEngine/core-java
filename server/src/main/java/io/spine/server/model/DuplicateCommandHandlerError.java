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

package io.spine.server.model;

import com.google.common.base.Joiner;
import io.spine.core.CommandClass;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.command.model.CommandHandlingClass;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * An error thrown on attempt to add a class which declares a
 * {@linkplain CommandHandlerMethod method} that handles a command which is
 * already handled by a class already added to the {@link Model}.
 *
 * @author Alexander Yevsyukov
 */
public class DuplicateCommandHandlerError extends ModelError {

    private static final long serialVersionUID = 0L;

    DuplicateCommandHandlerError(CommandHandlingClass duplicatingClass,
                                 Map<Set<CommandClass>, CommandHandlingClass> registeredHandlers) {
        super(fmt(duplicatingClass, registeredHandlers));
    }

    private static String fmt(CommandHandlingClass duplicatingClass,
                              Map<Set<CommandClass>, CommandHandlingClass> registeredHandlers) {
        checkNotNull(duplicatingClass);
        checkNotNull(registeredHandlers);
        StringBuilder builder = new StringBuilder(512);
        builder.append(format("The class %s declares handler methods for commands that are " +
                              "already handled by other classes.", duplicatingClass));
        for (Set<CommandClass> commandClasses : registeredHandlers.keySet()) {
            if (commandClasses.size() > 1) {
                builder.append(" Commands ");
                builder.append(Joiner.on(", ").join(commandClasses));
                builder.append(" are handled by ");
            } else {
                // One command.
                builder.append(" The command ");
                builder.append(commandClasses.iterator().next());
                builder.append(" is handled by ");
            }
            builder.append(registeredHandlers.get(commandClasses));
        }
        return builder.toString();
    }
}
