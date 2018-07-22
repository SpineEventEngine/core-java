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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.spine.core.CommandClass;
import io.spine.server.command.CommandHandlingClass;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.collect.Sets.intersection;

/**
 * Maps a Java class to its {@link ModelClass}.
 *
 * @author Alexander Yevsyukov
 */
final class ClassMap {

    private final Map<String, ModelClass<?>> classes = Maps.newConcurrentMap();

    /**
     * Returns a unique identifying name of the given class.
     *
     * <p>The returned value is guaranteed to be unique per class and non-null.
     *
     * @param cls the {@link Class} to identity
     * @return a non-null class name
     */
    private static String nameOf(Class<?> cls) {
        return cls.getName();
    }

    /**
     * Obtains a model class for the passed one.
     *
     * <p>If the class is not in the model, creates and puts it into the model using the provided
     * {@code Supplier}.
     *
     * <p>If the passed class handles commands, verifies that it does not handle commands for which
     * there are already handlers registered in the model.
     *
     * @param rawClass raw class for which to obtain a model class
     * @param supplier a supplier of a model class
     * @param commandHandler if {@code true} the passed raw class handles commands
     * @return the instance of the model class corresponding to the raw class
     * @throws DuplicateCommandHandlerError
     *         if the passed class handles commands that are already handled
     */
    ModelClass<?> get(Class<?> rawClass, Supplier<ModelClass<?>> supplier, boolean commandHandler) {
        String key = nameOf(rawClass);
        ModelClass<?> modelClass = classes.get(key);
        if (modelClass == null) {
            modelClass = supplier.get();
            if (commandHandler) {
                checkDuplicates((CommandHandlingClass) modelClass);
            }
            classes.put(key, modelClass);
        }
        return modelClass;
    }

    void clear() {
        classes.clear();
    }

    private void checkDuplicates(CommandHandlingClass candidate)
            throws DuplicateCommandHandlerError {
        Set<CommandClass> candidateCommands = candidate.getCommands();
        ImmutableMap.Builder<Set<CommandClass>, CommandHandlingClass> duplicates =
                ImmutableMap.builder();

        for (ModelClass<?> modelClass : classes.values()) {
            if (modelClass instanceof CommandHandlingClass) {
                CommandHandlingClass commandHandler = (CommandHandlingClass) modelClass;
                Set<CommandClass> alreadyHandled = commandHandler.getCommands();
                Set<CommandClass> intersection = intersection(alreadyHandled, candidateCommands);
                if (intersection.size() > 0) {
                    duplicates.put(intersection, commandHandler);
                }
            }
        }

        ImmutableMap<Set<CommandClass>, CommandHandlingClass> currentHandlers = duplicates.build();
        if (!currentHandlers.isEmpty()) {
            throw new DuplicateCommandHandlerError(candidate, currentHandlers);
        }
    }
}
