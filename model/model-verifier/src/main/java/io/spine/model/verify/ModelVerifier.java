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

package io.spine.model.verify;

import com.google.common.annotations.VisibleForTesting;
import org.gradle.api.Project;

import java.nio.file.Path;

/**
 * A utility for verifying Spine Model elements.
 */
final class ModelVerifier {

    /**
     * Command handler declarations to verify.
     */
    private final CommandHandlerSet commandHandlers;

    /**
     * Entity lifecycle declarations to verify.
     */
    private final EntitiesLifecycle entitiesLifecycle;

    @VisibleForTesting
    ModelVerifier(CommandHandlerSet commandHandlers, EntitiesLifecycle entitiesLifecycle) {
        this.commandHandlers = commandHandlers;
        this.entitiesLifecycle = entitiesLifecycle;
    }

    /**
     * Creates a new {@code ModelVerifier} for the model located at the given path.
     *
     * <p>Entity lifecycle declarations are gathered from the Spine options of the
     * {@linkplain io.spine.type.KnownTypes known types}.
     *
     * @param modelPath
     *         the path with serialized Spine Model
     */
    static ModelVerifier forModel(Path modelPath) {
        CommandHandlerSet commandHandlers = CommandHandlerSet.parse(modelPath);
        EntitiesLifecycle entitiesLifecycle = EntitiesLifecycle.ofKnownTypes();
        return new ModelVerifier(commandHandlers, entitiesLifecycle);
    }

    /**
     * Verifies Spine model upon the given Gradle project.
     *
     * @param project
     *         the project to gather classpath from
     */
    void verifyUpon(Project project) {
        ProjectClassLoader classLoader = new ProjectClassLoader(project);
        commandHandlers.checkAgainst(classLoader);
        entitiesLifecycle.checkLifecycleDeclarations();
    }
}
