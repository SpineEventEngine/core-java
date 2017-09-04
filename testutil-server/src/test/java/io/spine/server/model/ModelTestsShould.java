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

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.event.EventBus;
import io.spine.test.Tests;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexander Yevsyukov
 */
public class ModelTestsShould {

    private final Model model = Model.getInstance();

    @Before
    public void setUp() throws Exception {
        // The model should not be polluted by the previously executed tests.
        model.clear();
    }

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(ModelTests.class);
    }

    @Test
    public void clear_the_model() {

        // This adds a command handler for `com.google.protobuf.Timestamp`.
        model.asCommandHandlerClass(TestCommandHandler.class);

        ModelTests.clearModel();

        // This should pass as we cleared the model,
        // i.e. there is no registered command handler for `com.google.protobuf.Timestamp`.
        model.asCommandHandlerClass(DuplicatedCommandHandler.class);
    }

    private static class TestCommandHandler extends CommandHandler {
        private TestCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        Empty handle(Timestamp cmd) {
            return Empty.getDefaultInstance();
        }
    }

    private static class DuplicatedCommandHandler extends CommandHandler {
        private DuplicatedCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        /**
         * Handles the same command as {@link TestCommandHandler#handle(Timestamp)}.
         */
        @Assign
        Empty handle(Timestamp cmd) {
            return Empty.getDefaultInstance();
        }
    }
}
