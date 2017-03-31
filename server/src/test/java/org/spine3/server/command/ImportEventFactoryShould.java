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
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.protobuf.AnyPacker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.newUuidValue;
import static org.spine3.validate.Validate.isNotDefault;

public class ImportEventFactoryShould {

    private final Any producerId = AnyPacker.pack(newStringValue(getClass().getSimpleName()));

    @Test
    public void create_import_event() {
        final StringValue stringValue = newUuidValue();
        final Event event = ImportEventFactory.createEvent(stringValue, producerId);

        assertEquals(stringValue, unpack(event.getMessage()));
        assertEquals(unpack(producerId), unpack(event.getContext()
                                                     .getProducerId()));
    }

    @Test
    public void create_import_event_context() {
        final EventContext context = ImportEventFactory.createEventContext(producerId);

        assertEquals(unpack(producerId), unpack(context.getProducerId()));
        assertTrue(isNotDefault(context.getEventId()));
        assertTrue(isNotDefault(context.getTimestamp()));
    }
}
