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

import io.spine.core.BoundedContextName;
import io.spine.server.model.given.ModelTestEnv.MAggregate;
import io.spine.server.model.given.ModelTestEnv.MCommandHandler;
import io.spine.server.model.given.ModelTestEnv.MProcessManager;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.command.RefStartProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tres.quattro.Counter;
import uno.dos.Encounter;

import java.util.Optional;

import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.server.command.model.CommandHandlerClass.asCommandHandlerClass;
import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests of {@link Model}.
 *
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
@SuppressWarnings("ErrorNotRethrown")
@DisplayName("Model should")
class ModelTest {

    @BeforeEach
    void setUp() {
        Model.dropAllModels();
    }

    @SuppressWarnings("CheckReturnValue") // returned values are not used in this test
    @Test
    @DisplayName("check for duplicated command handlers in command handler class")
    void checkDuplicateCmdHandler() {
        try {
            asAggregateClass(MAggregate.class);
            asCommandHandlerClass(MCommandHandler.class);
            failErrorNotThrown();
        } catch (DuplicateCommandHandlerError error) {
            assertContainsClassName(error, RefCreateProject.class);
            assertContainsClassName(error, MAggregate.class);
            assertContainsClassName(error, MCommandHandler.class);
        }
    }

    @SuppressWarnings("CheckReturnValue") // returned values are not used in this test
    @Test
    @DisplayName("check for duplicated command handlers in process manager class")
    void checkDuplicateInProcman() {
        try {
            asAggregateClass(MAggregate.class);
            asProcessManagerClass(MProcessManager.class);
            failErrorNotThrown();
        } catch (DuplicateCommandHandlerError error) {
            assertContainsClassName(error, RefCreateProject.class);
            assertContainsClassName(error, RefStartProject.class);
            assertContainsClassName(error, MAggregate.class);
            assertContainsClassName(error, MProcessManager.class);
        }
    }

    /**
     * Tests that:
     * <ol>
     *   <li>{@code Model} obtains {@code BoundedContextName} specified in a package annotation.
     *   <li>Packages that do not have a common “root”, can be annotated with the same Bounded
     *       Context name.
     * </ol>
     */
    @Test
    @DisplayName("find BoundedContext package annotation")
    void findBoundedContextAnnotation() {
        Optional<BoundedContextName> ctx1 = Model.findContext(Counter.class);
        Optional<BoundedContextName> ctx2 = Model.findContext(Encounter.class);
        assertTrue(ctx1.isPresent());
        assertEquals("Counting", ctx1.get()
                                     .getValue());
        assertEquals(ctx1, ctx2);
    }

    private static void assertContainsClassName(DuplicateCommandHandlerError error, Class<?> cls) {
        String errorMessage = error.getMessage();
        assertTrue(errorMessage.contains(cls.getName()));
    }

    private static void failErrorNotThrown() {
        fail(DuplicateCommandHandlerError.class.getName() + " should be thrown");
    }
}
