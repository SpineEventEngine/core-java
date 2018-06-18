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

import com.google.protobuf.BoolValue;
import io.spine.core.EventEnvelope;
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.given.EventSubscriberTestEnv.FailingSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EventSubscriber should")
class EventSubscriberTest {

    private final TestEventFactory factory = TestEventFactory.newInstance(getClass());

    private EventSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new FailingSubscriber();
    }

    private EventEnvelope createEvent(boolean value) {
        return EventEnvelope.of(factory.createEvent(BoolValue.newBuilder()
                                                             .setValue(value)
                                                             .build()));
    }

    @Test
    @DisplayName("catch exceptions caused by methods")
    void catchExceptionsCausedByMethods() {
        // Create event which should fail.
        final EventEnvelope eventEnvelope = createEvent(false);

        final Set<String> dispatchingResult = subscriber.dispatch(eventEnvelope);

        final FailingSubscriber sub = (FailingSubscriber) this.subscriber;
        assertTrue(sub.isMethodCalled());
        assertEquals(0, dispatchingResult.size());
        assertEquals(eventEnvelope, sub.getLastErrorEnvelope());
        assertNotNull(sub.getLastException());
    }

    @Test
    @DisplayName("dispatch event")
    void dispatchEvent() {
        final EventEnvelope eventEnvelope = createEvent(true);

        final Set<String> dispatchingResult = subscriber.dispatch(eventEnvelope);

        final FailingSubscriber sub = (FailingSubscriber) this.subscriber;
        assertTrue(sub.isMethodCalled());
        assertEquals(1, dispatchingResult.size());
        assertNull(sub.getLastException());
    }

    @Test
    @DisplayName("have log")
    void haveLog() {
        assertEquals(subscriber.getClass()
                               .getName(),
                     subscriber.log()
                               .getName());
    }

    @Test
    @DisplayName("return handled message classes")
    void returnHandledMessageClasses() {
        assertEquals(3, subscriber.getMessageClasses()
                                  .size());
    }
}
