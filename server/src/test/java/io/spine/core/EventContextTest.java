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

package io.spine.core;

import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.server.event.EventFactory;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.type.given.EventsTestEnv.event;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EventContext should")
class EventContextTest {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(EventsTest.class);

    private EventContext context;

    @BeforeEach
    void setUp() {
        CommandEnvelope cmd = generate();
        StringValue producerId = StringValue.of(getClass().getSimpleName());
        EventFactory eventFactory = EventFactory.on(cmd, Identifier.pack(producerId));
        Event event = eventFactory.createEvent(GivenEvent.message(), null);
        context = event.context();
    }

    private static CommandEnvelope generate() {
        return CommandEnvelope.of(requestFactory.generateCommand());
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Method called to throw exception.
    @Nested
    @DisplayName("throw `IllegalStateException` when reading tenant ID")
    class ReportIllegalState {

        @Test
        @DisplayName("of the event without origin")
        void forEventWithoutOrigin() {
            EventContext context = contextWithoutOrigin().build();
            Event event = event(context);
            assertThrowsFor(event);
        }

        @Test
        @DisplayName("of the event whose event context has no origin")
        void forEventContextWithoutOrigin() {
            EventContext context = contextWithoutOrigin()
                    .setEventContext(contextWithoutOrigin())
                    .build();
            Event event = event(context);
            assertThrowsFor(event);
        }

        private void assertThrowsFor(Event event) {
            assertThrows(IllegalStateException.class, event::tenant);
        }

        private EventContext.Builder contextWithoutOrigin() {
            return context.toBuilder()
                          .clearOrigin();
        }
    }

    @Nested
    @DisplayName("obtain")
    class GetFromEventContext {

        @Test
        @DisplayName("actor")
        void actor() {
            assertEquals(context.getCommandContext()
                                .getActorContext()
                                .getActor(), context.actor());
        }

        @Test
        @DisplayName("producer")
        void producer() {
            StringValue msg = unpack(context.getProducerId(), StringValue.class);
            String id = (String) context.producer();
            assertEquals(msg.getValue(), id);
        }
    }
}
