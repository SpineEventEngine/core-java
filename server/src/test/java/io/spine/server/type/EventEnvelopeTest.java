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

package io.spine.server.type;

import com.google.protobuf.Message;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.type.given.EventEnvelopeTestEnv.actorContext;
import static io.spine.server.type.given.EventEnvelopeTestEnv.commandContext;
import static io.spine.server.type.given.EventEnvelopeTestEnv.event;
import static io.spine.server.type.given.EventEnvelopeTestEnv.eventContext;
import static io.spine.server.type.given.EventEnvelopeTestEnv.eventMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("EventEnvelope should")
class EventEnvelopeTest extends MessageEnvelopeTest<Event, EventEnvelope, EventClass> {

    @Override
    protected Event outerObject() {
        Message eventMessage = eventMessage();
        Event event = event(eventMessage);
        return event;
    }

    @Override
    protected EventEnvelope toEnvelope(Event obj) {
        return EventEnvelope.of(obj);
    }

    @Override
    protected EventClass getMessageClass(Event obj) {
        return EventClass.of(obj);
    }

    @Nested
    @DisplayName("obtain actor context from")
    class ObtainActorContext {

        @Test
        @DisplayName("command context")
        void fromCommandContext() {
            CommandContext commandContext = commandContext();
            EventContext context = eventContext(commandContext);
            EventEnvelope envelope = envelope(context);
            assertEquals(commandContext.getActorContext(), envelope.getActorContext());
        }

        @Test
        @DisplayName("command context of the origin event")
        void fromCommandContextOfOrigin() {
            CommandContext commandContext = commandContext();
            EventContext originContext = eventContext(commandContext);
            EventContext context = eventContext(originContext);
            EventEnvelope envelope = envelope(context);
            assertEquals(commandContext.getActorContext(), envelope.getActorContext());
        }

        @Test
        @DisplayName("import context")
        void fromImportContext() {
            EventContext context = eventContext(actorContext());
            EventEnvelope envelope = envelope(context);
            ActorContext expectedImportContext = context.getImportContext();
            assertEquals(expectedImportContext, envelope.getActorContext());
        }

        private EventEnvelope envelope(EventContext context) {
            Event event = event(eventMessage(), context);
            return toEnvelope(event);
        }
    }
}
