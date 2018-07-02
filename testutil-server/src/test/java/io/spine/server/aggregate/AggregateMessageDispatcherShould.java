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

package io.spine.server.aggregate;

import com.google.protobuf.FloatValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.server.aggregate.given.AggregateMessageDispatcherTestEnv.MessageLog;
import io.spine.server.command.TestEventFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchCommand;
import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class AggregateMessageDispatcherShould {

    private MessageLog aggregate;

    @Before
    public void setUp() {
        aggregate = new MessageLog(1L);
    }

    @Test
    @DisplayName("dispatch command")
    void dispatchCommandTest() {
        final TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());
        final int messageValue = 2017_07_28;
        final UInt32Value message = UInt32Value.newBuilder()
                                               .setValue(messageValue)
                                               .build();
        final CommandEnvelope commandEnvelope = CommandEnvelope.of(factory.createCommand(message));

        final List<? extends Message> eventMessages = dispatchCommand(aggregate, commandEnvelope);

        assertTrue(aggregate.getState()
                            .getValue()
                            .contains(String.valueOf(messageValue)));
        assertEquals(1, eventMessages.size());
        assertTrue(eventMessages.get(0) instanceof StringValue);
    }

    @Test
    @DisplayName("dispatch event")
    void dispatchEventTest() {
        final TestEventFactory factory = TestEventFactory.newInstance(getClass());
        final float messageValue = 2017.0729f;
        final FloatValue message = FloatValue.newBuilder()
                                             .setValue(messageValue)
                                             .build();
        final EventEnvelope eventEnvelope = EventEnvelope.of(factory.createEvent(message));

        final List<? extends Message> eventMessages = dispatchEvent(aggregate, eventEnvelope);

        assertTrue(aggregate.getState()
                            .getValue()
                            .contains(String.valueOf(messageValue)));
        assertEquals(1, eventMessages.size());
        assertTrue(eventMessages.get(0) instanceof StringValue);
    }
}
