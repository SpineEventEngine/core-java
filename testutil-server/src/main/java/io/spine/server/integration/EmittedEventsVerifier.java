/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.integration;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@VisibleForTesting
public abstract class EmittedEventsVerifier {

    public abstract void verify(EmittedEvents events);

    public static EmittedEventsVerifier emitted(final int expectedCount) {
        checkArgument(expectedCount >= 0, "0 or more emitted events must be expected.");

        return new EmittedEventsVerifier() {

            @Override
            public void verify(EmittedEvents events) {
                final int actualCount = events.count();
                final String moreOrLess = compare(actualCount, expectedCount);
                assertEquals("Bounded Context emitted " + moreOrLess + " events than expected",
                             expectedCount, actualCount);
            }
        };
    }

    @SafeVarargs
    public static EmittedEventsVerifier emitted(Class<? extends Message>... eventTypes) {
        checkArgument(eventTypes.length > 0);
        return emitted(asList(eventTypes));
    }

    private static EmittedEventsVerifier emitted(final List<Class<? extends Message>> eventTypes) {
        return new EmittedEventsVerifier() {

            @Override
            public void verify(EmittedEvents events) {
                for (Class<? extends Message> eventType : eventTypes) {
                    final String eventName = eventType.getName();
                    assertTrue(format("Bounded Context did not emit %s event", eventName),
                               events.contain(eventType));
                }
            }
        };
    }

    public static EmittedEventsVerifier
    emitted(final int expectedCount, final Class<? extends Message> eventType) {
        checkArgument(expectedCount >= 0);
        return new EmittedEventsVerifier() {

            @Override
            public void verify(EmittedEvents events) {
                final String eventName = eventType.getName();
                final int actualCount = events.count(eventType);
                final String moreOrLess = compare(actualCount, expectedCount);
                assertEquals(
                        format("Bounded Context emitted %s %s events than expected",
                               moreOrLess, eventName),
                        expectedCount, actualCount);
            }
        };
    }

    private static String compare(int actualCount, int expectedCount) {
        return (expectedCount < actualCount) ? "more" : "less";
    }
}
