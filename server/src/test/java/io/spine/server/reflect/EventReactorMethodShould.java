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

package io.spine.server.reflect;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.spine.core.Event;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.TestEventFactory;
import io.spine.server.reflect.given.EventReactorMethodTestEnv.ReactingAggregate;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class EventReactorMethodShould {

    private Aggregate<?, StringValue, ?> aggregate;
    private TestEventFactory eventFactory;

    @Before
    public void setUp() {
        aggregate = new ReactingAggregate(1L);
        eventFactory = TestEventFactory.newInstance(getClass());
    }

    @Test
    public void invoke_reactor_method() {
        final int value = 100;
        final Event event = eventFactory.createEvent(UInt32Value.newBuilder()
                                                                .setValue(value)
                                                                .build());
        final List<? extends Message> messages =
                EventReactorMethod.invokeFor(aggregate, event.getMessage(), event.getContext());

        final StringValue msg = (StringValue) messages.get(0);
        final String expected = String.valueOf(value);
        assertEquals(expected, msg.getValue());
    }
}
