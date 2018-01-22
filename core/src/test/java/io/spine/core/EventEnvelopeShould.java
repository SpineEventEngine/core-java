/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.protobuf.AnyPacker;
import io.spine.test.core.ProjectCreated;
import io.spine.test.core.ProjectId;

import static io.spine.Identifier.newUuid;

/**
 * @author Dmytro Dashenkov
 */
public class EventEnvelopeShould extends MessageEnvelopeShould<Event, EventEnvelope, EventClass> {


    @Override
    protected Event outerObject() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(newUuid())
                                             .build();

        final Message eventMessage = ProjectCreated.newBuilder()
                                                   .setProjectId(projectId)
                                                   .build();

        final EventId.Builder eventIdBuilder = EventId.newBuilder()
                                                       .setValue(newUuid());
        final Event event = Event.newBuilder()
                                 .setId(eventIdBuilder)
                                 .setMessage(AnyPacker.pack(eventMessage))
                                 .setContext(EventContext.getDefaultInstance())
                                 .build();
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
}
