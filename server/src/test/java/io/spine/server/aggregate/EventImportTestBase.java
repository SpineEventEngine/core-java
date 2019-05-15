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

package io.spine.server.aggregate;

import io.spine.base.EventMessage;
import io.spine.server.aggregate.given.klasse.EngineId;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.TestEventFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract class EventImportTestBase {

    static EngineId engineId(String value) {
        return EngineId
                .newBuilder()
                .setValue(value)
                .build();
    }

    /**
     * Creates a new event for the passed message.
     *
     * @param eventMessage
     *        the event message
     * @param producerId
     *        the producer for the event. If {@code null}, a test class name will be the ID
     *        of the producer.
     * @return generated event wrapped into the envelope
     */
    EventEnvelope createEvent(EventMessage eventMessage, @Nullable EngineId producerId) {
        TestEventFactory eventFactory = producerId == null
                                        ? TestEventFactory.newInstance(getClass())
                                        : TestEventFactory.newInstance(producerId, getClass());
        EventEnvelope result = EventEnvelope.of(eventFactory.createEvent(eventMessage));
        return result;
    }
}
