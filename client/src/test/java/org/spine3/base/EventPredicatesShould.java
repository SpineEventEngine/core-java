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

package org.spine3.base;

import com.google.common.base.Predicate;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.EventPredicates.isAfter;
import static org.spine3.base.EventPredicates.isBefore;
import static org.spine3.base.EventPredicates.isBetween;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.test.EventTests.newEventContext;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Tests.newUuidValue;
import static org.spine3.test.TimeTests.Past.minutesAgo;
import static org.spine3.test.TimeTests.Past.secondsAgo;

/**
 * @author Alexander Yevsyukov
 */
public class EventPredicatesShould {

    private static Event createEvent(int minutesAgo) {
        final Event result = Events.createEvent(newUuidValue(),
                                                newEventContext(minutesAgo(minutesAgo)));
        return result;
    }

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(EventPredicates.class);
    }

    @Test
    public void pass_null_tolerance() {
        new NullPointerTester()
                .setDefault(Timestamp.class, getCurrentTime())
                .testAllPublicStaticMethods(EventPredicates.class);
    }

    /*
     * IsAfter tests
     *****************/
    @Test
    public void return_false_from_null_input_in_IsAfter_predicate() {
        assertFalse(isAfter(secondsAgo(5)).apply(null));
    }

    /*
     * IsBefore tests
     *****************/

    @Test
    public void verify_if_an_event_is_after_another() {
        final Predicate<Event> predicate = isAfter(minutesAgo(100));
        assertTrue(predicate.apply(createEvent(20)));
        assertFalse(predicate.apply(createEvent(360)));
    }

    @Test
    public void return_false_from_null_input_in_IsBefore_predicate() {
        assertFalse(isBefore(secondsAgo(5)).apply(null));
    }

    /*
     * IsBefore tests
     *****************/

    @Test
    public void verify_if_an_event_is_before_another() {
        final Predicate<Event> predicate = isBefore(minutesAgo(100));
        assertFalse(predicate.apply(createEvent(20)));
        assertTrue(predicate.apply(createEvent(360)));
    }

    @Test
    public void return_null_from_null_input_in_IsBetween_predicate() {
        assertFalse(isBetween(secondsAgo(5), secondsAgo(1)).apply(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_that_range_start_is_before_end() {
        isBetween(minutesAgo(2), minutesAgo(10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_zero_length_time_range() {
        final Timestamp timestamp = minutesAgo(5);
        isBetween(timestamp, timestamp);
    }

    @Test
    public void verify_if_an_event_is_within_time_range() {
        final Event event = createEvent(5);

        assertTrue(isBetween(minutesAgo(10), minutesAgo(1))
                           .apply(event));

        assertFalse(isBetween(minutesAgo(2), minutesAgo(1))
                            .apply(event));
    }
}
