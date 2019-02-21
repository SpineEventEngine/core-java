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

import java.nio.file.Path;

/**
 * Spine model elements that are subject to verification.
 */
final class Model {

    private final CommandHandlerSet commandHandlers;
    private final EntitiesLifecycle entitiesLifecycle;

    @VisibleForTesting
    Model(CommandHandlerSet commandHandlers, EntitiesLifecycle entitiesLifecycle) {
        this.commandHandlers = commandHandlers;
        this.entitiesLifecycle = entitiesLifecycle;
    }

    static Model parse(Path modelPath) {
        CommandHandlerSet commandHandlers = CommandHandlerSet.parse(modelPath);
        EntitiesLifecycle entitiesLifecycle = EntitiesLifecycle.ofKnownTypes();
        return new Model(commandHandlers, entitiesLifecycle);
    }

    void verifyAgainst(ClassLoader classLoader) {
        commandHandlers.checkAgainst(classLoader);
        entitiesLifecycle.checkLifecycleDeclarations();
    }
}
