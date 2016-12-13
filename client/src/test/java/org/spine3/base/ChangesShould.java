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

package org.spine3.base;

import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Timestamps;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings({"ConstantConditions" /* We pass `null` to some of the methods to check handling of preconditions */,
                    "ResultOfMethodCallIgnored" /* ...when methods throw exceptions */})
public class ChangesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Changes.class));
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_previousValue() {
        Changes.of(null, "do_not_accept_null_previousValue");
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_newValue() {
        Changes.of("do_not_accept_null_newValue", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_string_values() {
        final String value = "do_not_accept_equal_values";
        Changes.of(value, value);
    }

    private static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void create_string_value_change() {
        final String previousValue = randomUuid();
        final String newValue = randomUuid();

        final StringChange result = Changes.of(previousValue, newValue);

        assertEquals(previousValue, result.getPreviousValue());
        assertEquals(newValue, result.getNewValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_double_values() {
        final double value = 1961.0412;
        Changes.of(value, value);
    }

    @Test
    public void create_double_value_change() {
        final double s1 = 1957.1004;
        final double s2 = 1957.1103;

        final DoubleChange result = Changes.of(s1, s2);

        assertTrue(Double.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Double.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Timestamp_previousValue() {
        Changes.of(null, Timestamps.getCurrentTime());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Timestamp_newValue() {
        Changes.of(Timestamps.getCurrentTime(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_Timestamp_values() {
        final Timestamp now = Timestamps.getCurrentTime();
        Changes.of(now, now);
    }

    @Test
    public void create_TimestampChange_instance() {
        final Timestamp fiveMinutesAgo = Timestamps.minutesAgo(5);
        final Timestamp now = Timestamps.getCurrentTime();

        final TimestampChange result = Changes.of(fiveMinutesAgo, now);

        assertEquals(fiveMinutesAgo, result.getPreviousValue());
        assertEquals(now, result.getNewValue());
    }
}
