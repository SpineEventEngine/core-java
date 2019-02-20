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

package io.spine.testing.server.aggregate;

import com.google.protobuf.Message;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.aggregate.given.agg.TuMessageLog;
import io.spine.testing.server.log.FloatLogged;
import io.spine.testing.server.log.LogInteger;
import io.spine.testing.server.log.ValueLogged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AggregateMessageDispatcher should")
class AggregateMessageDispatcherTest {

    private TuMessageLog aggregate;

    @BeforeEach
    void setUp() {
        aggregate = new TuMessageLog(1L);
    }

    @Test
    @DisplayName("dispatch command")
    void dispatchCommand() {
        TestActorRequestFactory factory = new TestActorRequestFactory(getClass());
        int messageValue = 2017_07_28;
        LogInteger message = LogInteger.newBuilder()
                                       .setValue(messageValue)
                                       .build();
        CommandEnvelope commandEnvelope = CommandEnvelope.of(factory.createCommand(message));
        List<? extends Message> eventMessages =
                AggregateMessageDispatcher.dispatchCommand(aggregate, commandEnvelope);
        assertTrue(aggregate.getState()
                            .getValue()
                            .contains(String.valueOf(messageValue)));
        assertEquals(1, eventMessages.size());
        assertTrue(eventMessages.get(0) instanceof ValueLogged);
    }

    @Test
    @DisplayName("dispatch event")
    void dispatchEvent() {
        TestEventFactory factory = TestEventFactory.newInstance(getClass());
        float messageValue = 2017.0729f;
        FloatLogged message = FloatLogged.newBuilder()
                                         .setValue(messageValue)
                                         .build();
        EventEnvelope eventEnvelope = EventEnvelope.of(factory.createEvent(message));
        List<? extends Message> eventMessages =
                AggregateMessageDispatcher.dispatchEvent(aggregate, eventEnvelope);
        assertTrue(aggregate.getState()
                            .getValue()
                            .contains(String.valueOf(messageValue)));
        assertEquals(1, eventMessages.size());
        assertTrue(eventMessages.get(0) instanceof ValueLogged);
    }
}
