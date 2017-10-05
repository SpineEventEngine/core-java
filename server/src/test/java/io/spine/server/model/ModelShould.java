/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.server.model.given.ModelTestEnv.MAggregate;
import io.spine.server.model.given.ModelTestEnv.MCommandHandler;
import io.spine.server.model.given.ModelTestEnv.MProcessManager;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.command.RefStartProject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests of {@link Model}.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ErrorNotRethrown")
public class ModelShould {

    private final Model model = Model.getInstance();

    @Before
    public void setUp() {
        model.clear();
    }

    @Test
    public void not_allow_duplicated_command_handlers() {
        try {
            model.asAggregateClass(MAggregate.class);
            model.asCommandHandlerClass(MCommandHandler.class);
            failErrorNotThrown();
        } catch (DuplicateCommandHandlerError error) {
            assertContainsClassName(error, RefCreateProject.class);
            assertContainsClassName(error, MAggregate.class);
            assertContainsClassName(error, MCommandHandler.class);
        }
    }

    @Test
    public void not_allow_more_than_one_command_duplication() {
        try {
            model.asAggregateClass(MAggregate.class);
            model.asProcessManagerClass(MProcessManager.class);
            failErrorNotThrown();
        } catch (DuplicateCommandHandlerError error) {
            assertContainsClassName(error, RefCreateProject.class);
            assertContainsClassName(error, RefStartProject.class);
            assertContainsClassName(error, MAggregate.class);
            assertContainsClassName(error, MProcessManager.class);
        }
    }

    @Test
    public void get_default_state_should_not_return_null() {
        final Message defaultState = model.getDefaultState(MAggregate.class);
        assertNotNull("Default state should not be null.", defaultState);
    }

    private static void assertContainsClassName(DuplicateCommandHandlerError error, Class<?> cls) {
        final String errorMessage = error.getMessage();
        assertTrue(errorMessage.contains(cls.getName()));
    }

    private static void failErrorNotThrown() {
        fail(DuplicateCommandHandlerError.class.getName() + " should be thrown");
    }
}
