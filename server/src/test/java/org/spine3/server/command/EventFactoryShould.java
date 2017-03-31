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

package org.spine3.server.command;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.server.command.EventFactory.createEvent;
import static org.spine3.server.command.EventFactory.generateId;
import static org.spine3.test.EventTests.newEventContext;
import static org.spine3.test.Tests.newUuidValue;

public class EventFactoryShould {

    private EventContext context;
    private StringValue stringValue;

    @Before
    public void setUp() {
        stringValue = newUuidValue();
        context = newEventContext();
    }

    @Test
    public void generate_event_id() {
        final EventId result = generateId();

        assertFalse(result.getUuid()
                          .isEmpty());
    }

    @Test
    public void create_event() {
        createEventTest(stringValue);
        createEventTest(Timestamps2.getCurrentTime());
    }

    private void createEventTest(Message msg) {
        final Event event = createEvent(msg, context);

        assertEquals(msg, unpack(event.getMessage()));
        assertEquals(context, event.getContext());
    }

    @Test
    public void create_event_with_Any() {
        final Any msg = AnyPacker.pack(stringValue);
        final Event event = createEvent(msg, context);

        assertEquals(msg, event.getMessage());
        assertEquals(context, event.getContext());
    }
}
