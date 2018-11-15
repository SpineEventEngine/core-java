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

package io.spine.core;

import com.google.protobuf.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.core.given.EventEnvelopeTestEnv.actorContext;
import static io.spine.core.given.EventEnvelopeTestEnv.commandContext;
import static io.spine.core.given.EventEnvelopeTestEnv.event;
import static io.spine.core.given.EventEnvelopeTestEnv.eventMessage;
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

    @Test
    @DisplayName("obtain actor context from command context")
    void actorContextFromCommandContext() {
        CommandContext commandContext = commandContext();
        EventContext context = EventContext.newBuilder()
                                           .setCommandContext(commandContext)
                                           .build();
        Event event = event(eventMessage(), context);
        EventEnvelope envelope = toEnvelope(event);
        assertEquals(commandContext.getActorContext(), envelope.getActorContext());
    }

    @Test
    @DisplayName("obtain actor context from import context")
    void actorContextFromImportContext() {
        EventContext context = EventContext.newBuilder()
                                           .setImportContext(actorContext())
                                           .build();
        Event event = event(eventMessage(), context);
        EventEnvelope envelope = toEnvelope(event);
        ActorContext expectedImportContext = context.getImportContext();
        assertEquals(expectedImportContext, envelope.getActorContext());
    }
}
