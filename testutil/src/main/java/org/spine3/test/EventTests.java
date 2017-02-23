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

package org.spine3.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Events.generateId;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.test.Tests.newUuidValue;

/**
 * Utility class for producing events for tests.
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class EventTests {

    private static final TestCommandFactory factory =
            TestCommandFactory.newInstance(EventTests.class);

    private EventTests() {
        // Prevent instantiation of this utility class.
    }

    public static EventContext newEventContext() {
        return newEventContext(getCurrentTime());
    }

    public static EventContext newEventContext(Timestamp time) {
        final EventId eventId = generateId();
        final Any producerId = AnyPacker.pack(newUuidValue());
        final CommandContext cmdContext = factory.createCommandContext();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setProducerId(producerId)
                                                         .setTimestamp(time)
                                                         .setCommandContext(cmdContext);
        return builder.build();
    }

    public static Event newEvent(Message eventMessage) {
        checkNotNull(eventMessage);
        return Events.createEvent(eventMessage, newEventContext());
    }

    public static Event newEvent(Message eventMessage, Timestamp when) {
        checkNotNull(eventMessage);
        checkNotNull(when);
        return Events.createEvent(eventMessage, newEventContext(when));
    }
}
