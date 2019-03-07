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

import com.google.common.collect.ImmutableSet;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import java.util.Set;

public class DelegatingCommandDispatcherTestEnv {

    /** Prevents instantiation of this utility class. */
    private DelegatingCommandDispatcherTestEnv() {
    }

    public static final class EmptyCommandDispatcherDelegate
            implements CommandDispatcherDelegate<String> {

        private boolean onErrorCalled;

        @Override
        public Set<CommandClass> getCommandClasses() {
            return ImmutableSet.of();
        }

        @Override
        public String dispatchCommand(CommandEnvelope envelope) {
            return getClass().getName();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            onErrorCalled = true;
        }

        public boolean onErrorCalled() {
            return onErrorCalled;
        }
    }
}
