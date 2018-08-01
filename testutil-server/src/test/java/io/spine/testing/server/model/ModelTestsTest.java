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

package io.spine.testing.server.model;

import io.spine.server.command.model.CommandHandlerClass;
import io.spine.server.model.Model;
import io.spine.testing.UtilityClassTest;
import io.spine.testing.server.model.given.ModelTestsTestEnv.DuplicatedCommandHandler;
import io.spine.testing.server.model.given.ModelTestsTestEnv.TestCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.command.model.CommandHandlerClass.asCommandHandlerClass;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("ModelTests utility should")
class ModelTestsTest extends UtilityClassTest<ModelTests> {

    ModelTestsTest() {
        super(ModelTests.class);
    }

    @BeforeEach
    void setUp() {
        // The model should not be polluted by the previously executed tests.
        ModelTests.dropAllModels();
    }

    @Test
    @DisplayName("clear all models")
    void clearModel() {
        // This adds a command handler for `com.google.protobuf.Timestamp`.
        CommandHandlerClass cls1 = asCommandHandlerClass(TestCommandHandler.class);
        assertNotNull(cls1);

        ModelTests.dropAllModels();

        // This should pass as we cleared the model,
        // i.e. there is no registered command handler for `com.google.protobuf.Timestamp`.
        CommandHandlerClass cls2 = asCommandHandlerClass(DuplicatedCommandHandler.class);
        assertNotNull(cls2);
        assertNotEquals(cls1, cls2);
    }

    @Test
    @DisplayName("be the only place for clearing models")
    void theSoleCleaner() {
        assertThrows(SecurityException.class, Model::dropAllModels);
    }
}
