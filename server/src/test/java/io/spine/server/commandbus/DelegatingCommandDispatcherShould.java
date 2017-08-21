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

package io.spine.server.commandbus;

import com.google.common.collect.ImmutableSet;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class DelegatingCommandDispatcherShould {

    @Test
    public void delegate_onError() throws Exception {
        final EmptyCommandDispatcherDelegate delegate = new EmptyCommandDispatcherDelegate();

        final DelegatingCommandDispatcher<String> delegatingDispatcher =
                DelegatingCommandDispatcher.of(delegate);

        final CommandEnvelope commandEnvelope =
                TestActorRequestFactory.newInstance(getClass()).generateEnvelope();

        delegatingDispatcher.onError(commandEnvelope, new RuntimeException(getClass().getName()));

        assertTrue(delegate.onErrorCalled());
    }

    private static final class EmptyCommandDispatcherDelegate
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

        boolean onErrorCalled() {
            return onErrorCalled;
        }
    }
}
