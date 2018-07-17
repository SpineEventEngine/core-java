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

package io.spine.core.given;

import com.google.protobuf.Message;
import io.spine.base.Time;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventsTest;
import io.spine.server.command.TestEventFactory;
import io.spine.testing.Tests;

import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.testing.TestValues.newUuidValue;
import static io.spine.time.testing.TimeTests.Past.minutesAgo;

/**
 * @author Alexander Yevsyukov
 */
public class GivenEvent {

    public static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(toAny(GivenEvent.class.getSimpleName()),
                                         EventsTest.class);

    /** Prevent instantiation of this utility class. */
    private GivenEvent() {
    }

    public static EventContext context() {
        final Event event = eventFactory.createEvent(Time.getCurrentTime(),
                                                     Tests.nullRef());
        return event.getContext();
    }

    public static Event occurredMinutesAgo(int minutesAgo) {
        final Event result = eventFactory.createEvent(newUuidValue(),
                                                      null,
                                                      minutesAgo(minutesAgo));
        return result;
    }

    public static Event withMessage(Message message) {
        final Event event = eventFactory.createEvent(message);
        return event;
    }

    public static Event withDisabledEnrichmentOf(Message message) {
        final Event event = withMessage(message);
        final Event.Builder builder =
                event.toBuilder()
                     .setContext(event.getContext()
                                      .toBuilder()
                                      .setEnrichment(Enrichment.newBuilder()
                                                               .setDoNotEnrich(true)));
        return builder.build();
    }
}
