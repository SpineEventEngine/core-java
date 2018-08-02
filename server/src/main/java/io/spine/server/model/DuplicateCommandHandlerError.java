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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

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
        @SuppressWarnings("MagicNumber") // the buffer size that should cover most cases.
        StringBuilder builder = new StringBuilder(512);

        builder.append(format("The class `%s` declares handler ", duplicatingClass));

        // Do we have more than one command to report?
        long totalDuplicatedCommands =
                registeredHandlers.keySet()
                                  .stream()
                                  .mapToLong(Collection::size)
                                  .sum();
        checkState(totalDuplicatedCommands >= 1);
        builder.append(
                totalDuplicatedCommands == 1
                ? "method for the command which is "
                : "methods for commands that are "
        );
        builder.append("already handled by ");

        // How many handling classes do we have already?
        long totalHandlingClasses = registeredHandlers.values()
                                                      .size();
        builder.append(
                totalHandlingClasses == 1
                ? "another class."
                : "other classes."
        );

        String newLine = format("%n");
        builder.append(newLine);

        // Now list the commands and their handlers.
        for (Set<CommandClass> commandClasses : registeredHandlers.keySet()) {
            builder.append(newLine);
            if (commandClasses.size() > 1) {
                builder.append(" Commands ");
                List<String> commandsBackTicked =
                        commandClasses.stream()
                                      .map(DuplicateCommandHandlerError::backtick)
                                      .collect(toList());
                builder.append(
                        Joiner.on(", ")
                              .join(commandsBackTicked)
                );
                builder.append(" are handled by ");
            } else {
                // One command.
                builder.append(" The command ");
                CommandClass cmdClass = commandClasses.iterator()
                                                      .next();
                builder.append(backtick(cmdClass));
                builder.append(" is handled by ");
            }
            CommandHandlingClass handlingClass = registeredHandlers.get(commandClasses);
            builder.append(backtick(handlingClass));
            builder.append('.');
        }
        return builder.toString();
    }

    private static String backtick(Object obj) {
        return format("`%s`", obj);
    }
}
