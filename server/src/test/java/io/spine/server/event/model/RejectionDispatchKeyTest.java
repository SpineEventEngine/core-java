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

package io.spine.server.event.model;

import io.spine.core.CommandContext;
import io.spine.core.Subscribe;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.rejection.ProjectRejections.SigCannotCreateProject;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.type.EventClass;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("RejectionDispatchKey should")
final class RejectionDispatchKeyTest extends UtilityClassTest<RejectionDispatchKey> {

    RejectionDispatchKeyTest() {
        super(RejectionDispatchKey.class);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    @DisplayName("create a valid key")
    void createValidKey() {
        assertDoesNotThrow(() -> {
            for (Method method : ValidSubscriber.class.getDeclaredMethods()) {
                RejectionDispatchKey.of(EventClass.from(SigCannotCreateProject.class), method);
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    @DisplayName("not create keys for rejections without a cause")
    void notCreateKeyForRejectionsWithoutCause() {
        assertThrows(IllegalArgumentException.class, () -> {
            for (Method method : InvalidSubscriber.class.getDeclaredMethods()) {
                RejectionDispatchKey.of(EventClass.from(SigCannotCreateProject.class), method);
            }
        });
    }

    private static final class InvalidSubscriber extends AbstractEventSubscriber {

        @Subscribe
        void rejectionWithoutCommand(SigCannotCreateProject rejection) {
            // do nothing.
        }

        @Subscribe
        void rejectionWithNonCommandParam(SigCannotCreateProject rejection,
                                          CommandContext ctx) {
            // do nothing.
        }
    }

    private static final class ValidSubscriber extends AbstractEventSubscriber {

        @Subscribe
        void rejectionWithCommand(SigCannotCreateProject rejection,
                                  SigCreateProject command) {
            // do nothing.
        }

        @Subscribe
        void rejectionWithCommandAndCtx(SigCannotCreateProject rejection,
                                        SigCreateProject cmd,
                                        CommandContext ctx) {
            // do nothing.
        }
    }
}
