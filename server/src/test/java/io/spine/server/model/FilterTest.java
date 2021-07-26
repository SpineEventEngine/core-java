/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.server.entity.Entity;
import io.spine.server.model.given.filter.CreateProjectCommander;
import io.spine.server.model.given.filter.CreateProjectEventCommander;
import io.spine.server.model.given.filter.ModSplitCommandAggregate;
import io.spine.server.model.given.filter.ModSplitEventAggregate;
import io.spine.server.model.given.filter.ProjectCreatedReactor;
import io.spine.server.model.given.filter.ProjectCreatedSubscriber;
import io.spine.server.model.given.filter.ProjectTasksSubscriber;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Handler methods with field filters should")
class FilterTest {

    @AfterEach
    void clearModel() {
        ModelTests.dropAllModels();
    }

    @Nested
    @DisplayName("be OK on a")
    class Accept {

        @Test
        @DisplayName("`@Subscribe`-r")
        void subscribe() {
            assertValid(ProjectCreatedSubscriber.class);
        }

        @Test
        @DisplayName("`@React`-or")
        void react() {
            assertValid(ProjectCreatedReactor.class);
        }

        @Test
        @DisplayName("event accepting `@Command`-er")
        void eventCommand() {
            assertValid(CreateProjectEventCommander.class);
        }

        @Test
        @DisplayName("command accepting `@Command`-er")
        void commandCommand() {
            assertValid(CreateProjectCommander.class);
        }

        @Test
        @DisplayName("`@Assign`-ed command handler")
        void assign() {
            assertValid(ModSplitCommandAggregate.class);
        }

        @Test
        @DisplayName("`@Apply`-er")
        void apply() {
            assertValid(ModSplitEventAggregate.class);
        }
    }

    @Test
    @DisplayName("be not acceptable for a state `@Subscribe`-r method")
    void noState() {
        assertInvalid(ProjectTasksSubscriber.class);
    }

    @SuppressWarnings({
            "ClassNewInstance", // We don't care about custom exceptions.
            "ResultOfMethodCallIgnored" // Call for side effect.
    })
    private static void assertValid(Class<?> classWithHandler) {
        try {
            Object instance = classWithHandler.newInstance();
            if (Entity.class.isAssignableFrom(classWithHandler)) {
                ((Entity<?, ?>) instance).modelClass();
            }
        } catch (Throwable e) {
            fail(e);
        }
    }

    private static void assertInvalid(Class<?> classWithHandler) {
        Assertions.assertThrows(ModelError.class, classWithHandler::newInstance);
    }
}
