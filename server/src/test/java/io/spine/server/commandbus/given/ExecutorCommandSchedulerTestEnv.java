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

package io.spine.server.commandbus.given;

import io.spine.server.commandbus.CommandFlowWatcher;
import io.spine.server.type.CommandEnvelope;

public final class ExecutorCommandSchedulerTestEnv {

    /** Prevents instantiation of this test env class. */
    private ExecutorCommandSchedulerTestEnv() {
    }

    public static final class ThrowingFlowWatcher implements CommandFlowWatcher {

        private boolean onDispatchCalled = false;

        @Override
        public void onDispatchCommand(CommandEnvelope command) {
            onDispatchCalled = true;
            throwException();
        }

        @Override
        public void onScheduled(CommandEnvelope command) {
            throwException();
        }

        public boolean onDispatchCalled() {
            return onDispatchCalled;
        }

        private static void throwException() {
            throw new IllegalStateException("Ignore this error");
        }
    }
}
