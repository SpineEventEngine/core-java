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

package io.spine.envelope;

import com.google.protobuf.Message;
import io.spine.base.Event;
import io.spine.test.TestEventFactory;
import io.spine.test.envelope.ProjectCreated;
import io.spine.test.envelope.ProjectId;
import io.spine.type.EventClass;

import static io.spine.base.Identifier.newUuid;

/**
 * @author Dmytro Dashenkov
 */
public class EventEnvelopeShould extends MessageEnvelopeShould<Event, EventEnvelope, EventClass> {

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(EventEnvelopeShould.class);

    @Override
    protected Event outerObject() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(newUuid())
                                             .build();

        final Message eventMessage = ProjectCreated.newBuilder()
                                                   .setProjectId(projectId)
                                                   .build();
        return eventFactory.createEvent(eventMessage);
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
