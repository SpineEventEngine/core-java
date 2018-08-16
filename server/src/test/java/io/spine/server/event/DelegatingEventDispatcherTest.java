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

package io.spine.server.event;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import io.spine.core.BoundedContextNames;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.event.given.DelegatingEventDispatcherTestEnv.DummyEventDispatcherDelegate;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.integration.ExternalMessages;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.TestValues.newUuidValue;
import static io.spine.util.Exceptions.newIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("DelegatingEventDispatcher should")
class DelegatingEventDispatcherTest {

    private DummyEventDispatcherDelegate delegate;
    private DelegatingEventDispatcher<String> delegatingDispatcher;

    @BeforeEach
    void setUp() {
        delegate = new DummyEventDispatcherDelegate();
        delegatingDispatcher = DelegatingEventDispatcher.of(delegate);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(EventDispatcherDelegate.class, new DummyEventDispatcherDelegate())
                .testAllPublicStaticMethods(DelegatingEventDispatcher.class);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("delegate `onError`")
    void delegateOnError() {
        TestEventFactory factory = TestEventFactory.newInstance(getClass());
        EventEnvelope envelope = EventEnvelope.of(factory.createEvent(newUuidValue()));

        RuntimeException exception = new RuntimeException("test delegating onError");
        delegatingDispatcher.onError(envelope, exception);

        assertTrue(delegate.onErrorCalled());
        assertEquals(exception, delegate.getLastException());
    }

    @Test
    @DisplayName("expose external dispatcher that delegates `onError`")
    void exposeExternalDispatcher() {
        ExternalMessageDispatcher<String> extMessageDispatcher =
                delegatingDispatcher
                        .createExternalDispatcher()
                        .orElseThrow(() -> newIllegalStateException("No external events in %s",
                                                                    delegatingDispatcher));

        TestEventFactory factory = TestEventFactory.newInstance(getClass());
        StringValue eventMsg = newUuidValue();
        Event event = factory.createEvent(eventMsg);
        ExternalMessage externalMessage =
                ExternalMessages.of(event, BoundedContextNames.newName(getClass().getName()));

        ExternalMessageEnvelope externalMessageEnvelope =
                ExternalMessageEnvelope.of(externalMessage, eventMsg);

        RuntimeException exception =
                new RuntimeException("test external dispatcher delegating onError");
        extMessageDispatcher.onError(externalMessageEnvelope, exception);

        assertTrue(delegate.onErrorCalled());
    }
}
