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

package io.spine.base.given;

import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.base.EventsShould;
import io.spine.base.Version;
import io.spine.protobuf.Wrapper;
import io.spine.server.command.TestEventFactory;
import io.spine.test.Tests;
import io.spine.time.Time;

import static io.spine.test.TimeTests.Past.minutesAgo;
import static io.spine.test.Values.newUuidValue;

/**
 * @author Alexander Yevsyukov
 */
public class Given {

    public static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(Wrapper.forString()
                                                .pack(Given.class.getSimpleName()),
                                         EventsShould.class);

    private Given() {
        // Prevent instantiation of this utility class.
    }

    public static EventContext newEventContext() {
        final Event event = eventFactory.createEvent(Time.getCurrentTime(),
                                                     Tests.<Version>nullRef());
        return event.getContext();
    }

    public static Event eventOccurredMinutesAgo(int minutesAgo) {
        final Event result = eventFactory.createEvent(newUuidValue(),
                                                      null,
                                                      minutesAgo(minutesAgo));
        return result;
    }
}
