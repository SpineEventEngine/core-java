/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.time.change;

import org.junit.Test;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Timestamps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings({"ConstantConditions" /* We pass `null` to some of the methods to check handling of preconditions */,
        "ResultOfMethodCallIgnored" /* ...when methods throw exceptions */,
        "ClassWithTooManyMethods" , "OverlyCoupledClass" /* we test many data types and utility methods */})
public class ChangesShould {

    private static final String ERR_PREVIOUS_VALUE_CANNOT_BE_NULL = "do_not_accept_null_previousValue";
    private static final String ERR_NEW_VALUE_CANNOT_BE_NULL = "do_not_accept_null_newValue";
    private static final String ERR_VALUES_CANNOT_BE_EQUAL = "do_not_accept_equal_values";

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Changes.class));
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_interval_previousStartValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(null, fourMinutesAgo, now, fiveMinutesAgo);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_interval_newStartValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(fourMinutesAgo, null, now, fiveMinutesAgo);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_interval_previousEndValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(now, fourMinutesAgo, null, fiveMinutesAgo);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_interval_newEndValue() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp fourMinutesAgo = Timestamps.minutesAgo(4);
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.ofInterval(fourMinutesAgo, fiveMinutesAgo, now, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_Timestamp_values() {
        final Timestamp now = Timestamps.getCurrentTime();
        org.spine3.change.Changes.of(now, now);
    }

}
