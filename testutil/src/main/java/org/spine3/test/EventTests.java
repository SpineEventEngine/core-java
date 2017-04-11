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
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.protobuf.Timestamps2;

import static org.spine3.test.Tests.newUuidValue;
import static org.spine3.test.TimeTests.Past.minutesAgo;

/**
 * Utility class for producing events for tests.
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class EventTests {

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(EventTests.class);

    private EventTests() {
        // Prevent instantiation of this utility class.
    }

    public static EventContext newEventContext() {
        final Event event = eventFactory.createEvent(Timestamps2.getCurrentTime(),
                                                     Tests.<Version>nullRef());
        return event.getContext();
    }

    public static Event createEventOccurredMinutesAgo(int minutesAgo) {
        final Event result = eventFactory.createEvent(newUuidValue(),
                                                      null,
                                                      minutesAgo(minutesAgo));
        return result;
    }
}
