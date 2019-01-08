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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableSet;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.test.event.EvTeamCreated;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

public class DelegatingEventDispatcherTestEnv {

    /** Prevents instantiation of this utility class. */
    private DelegatingEventDispatcherTestEnv() {
    }

    public static final class DummyEventDispatcherDelegate
            implements EventDispatcherDelegate<String> {

        private boolean onErrorCalled;

        private @Nullable RuntimeException lastException;

        @Override
        public Set<EventClass> getEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public Set<EventClass> getExternalEventClasses() {
            // Return at least one event class so that we can create external delegate.
            return EventClass.setOf(EvTeamCreated.class);
        }

        @Override
        public Set<String> dispatchEvent(EventEnvelope envelope) {
            // Do nothing.
            return ImmutableSet.of(getClass().getName());
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            onErrorCalled = true;
            lastException = exception;
        }

        public boolean onErrorCalled() {
            return onErrorCalled;
        }

        public @Nullable RuntimeException getLastException() {
            return lastException;
        }
    }
}
