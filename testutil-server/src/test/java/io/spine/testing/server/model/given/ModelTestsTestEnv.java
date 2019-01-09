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

package io.spine.testing.server.model.given;

import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.event.EventBus;
import io.spine.server.model.Nothing;
import io.spine.testing.server.given.entity.command.TuRemoveProject;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Kuzmin
 */
public class ModelTestsTestEnv {

    /** Prevents instantiation of this utility class. */
    private ModelTestsTestEnv() {
    }

    public static class TestCommandHandler extends AbstractCommandHandler {
        private TestCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        Nothing handle(TuRemoveProject cmd) {
            return nothing();
        }
    }

    public static class DuplicatedCommandHandler extends AbstractCommandHandler {
        private DuplicatedCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        /**
         * Handles the same command as {@link TestCommandHandler#handle(TuRemoveProject)}.
         */
        @Assign
        Nothing handle(TuRemoveProject cmd) {
            return nothing();
        }
    }
}
