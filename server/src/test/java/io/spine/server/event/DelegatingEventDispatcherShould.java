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

package io.spine.server.event;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.command.TestEventFactory;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Set;

import static io.spine.test.TestValues.newUuidValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class DelegatingEventDispatcherShould {

    private EmptyEventDispatcherDelegate delegate;
    private DelegatingEventDispatcher<String> delegatingDispatcher;

    @Before
    public void setUp() {
        delegate = new EmptyEventDispatcherDelegate();
        delegatingDispatcher = DelegatingEventDispatcher.of(delegate);
    }

    @Test
    public void pass_null_tolerance_test() {
        new NullPointerTester()
                .setDefault(EventDispatcherDelegate.class, new EmptyEventDispatcherDelegate())
                .testAllPublicStaticMethods(DelegatingEventDispatcher.class);
    }

    @Test
    public void delegate_onError() {
        final TestEventFactory factory = TestEventFactory.newInstance(getClass());
        EventEnvelope envelope = EventEnvelope.of(factory.createEvent(newUuidValue()));

        final RuntimeException exception = new RuntimeException("test delegating onError");
        delegatingDispatcher.onError(envelope, exception);

        assertTrue(delegate.onErrorCalled());
        assertEquals(exception, delegate.getLastException());
    }

    /*
     * Test environment
     ********************/

    private static final class EmptyEventDispatcherDelegate
            implements EventDispatcherDelegate<String> {

        private boolean onErrorCalled;

        @Nullable
        private RuntimeException lastException;

        @Override
        public Set<EventClass> getEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public Set<EventClass> getExternalEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public Set<String> dispatchEvent(EventEnvelope envelope) {
            // Do nothing.
            return ImmutableSet.of();
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            onErrorCalled = true;
            lastException = exception;
        }

        private boolean onErrorCalled() {
            return onErrorCalled;
        }

        @Nullable
        private RuntimeException getLastException() {
            return lastException;
        }
    }
}
