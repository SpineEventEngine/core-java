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

package io.spine.server.event;

import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.given.EventSubscriberTestEnv.FailingSubscriber;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.DiagnosticMonitor;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.test.event.FailRequested;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EventSubscriber should")
class EventSubscriberTest {

    private final TestEventFactory factory = TestEventFactory.newInstance(getClass());

    private FailingSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new FailingSubscriber();
    }

    @Test
    @MuteLogging
    @DisplayName("catch exceptions caused by methods")
    void catchMethodExceptions() {
        // Create event which should fail.
        EventEnvelope eventEnvelope = createEvent(false);

        BoundedContext context = BoundedContextBuilder
                .assumingTests()
                .build();
        context.registerEventDispatcher(subscriber);
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        context.registerEventDispatcher(monitor);
        subscriber.dispatch(eventEnvelope);

        assertTrue(subscriber.isMethodCalled());
        List<HandlerFailedUnexpectedly> systemEvents = monitor.handlerFailureEvents();
        assertThat(systemEvents)
                .hasSize(1);
        HandlerFailedUnexpectedly systemEvent = systemEvents.get(0);
        assertThat(systemEvent.getHandledSignal())
                .isEqualTo(eventEnvelope.messageId());
        assertThat(systemEvent.getError()).isNotEqualToDefaultInstance();
        Any expectedId = Identifier.pack(subscriber.getClass()
                                                   .getName());
        assertThat(systemEvent.getEntity().getId())
                .isEqualTo(expectedId);
    }

    @Test
    @DisplayName("dispatch event")
    void dispatchEvent() {
        EventEnvelope eventEnvelope = createEvent(true);

        subscriber.dispatch(eventEnvelope);
        assertTrue(subscriber.isMethodCalled());
    }

    @Test
    @DisplayName("expose handled message classes")
    void exposeMessageClasses() {
        assertEquals(3, subscriber.messageClasses()
                                  .size());
    }

    @Test
    @DisplayName("expose handled external message classes")
    void exposeExternalClasses() {
        assertEquals(1, subscriber.externalEventClasses()
                                  .size());
    }

    private EventEnvelope createEvent(boolean shouldFail) {
        Event event = factory.createEvent(FailRequested.newBuilder()
                                                       .setShouldFail(shouldFail)
                                                       .build());
        return EventEnvelope.of(event);
    }
}
