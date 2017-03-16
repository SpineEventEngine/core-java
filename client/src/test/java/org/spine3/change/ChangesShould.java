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

package org.spine3.change;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Timestamps2;
import org.spine3.test.TimeTests;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

@SuppressWarnings({"ConstantConditions" /* We pass `null` to some of the methods to check handling of preconditions */,
        "ResultOfMethodCallIgnored" /* ...when methods throw exceptions */,
        "ClassWithTooManyMethods" , "OverlyCoupledClass" /* we test many data types and utility methods */})
public class ChangesShould {

    private static final String ERR_PREVIOUS_VALUE_CANNOT_BE_NULL = "do_not_accept_null_previousValue";
    private static final String ERR_NEW_VALUE_CANNOT_BE_NULL = "do_not_accept_null_newValue";
    private static final String ERR_VALUES_CANNOT_BE_EQUAL = "do_not_accept_equal_values";

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Changes.class);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_previousValue() {
        Changes.of(null, ERR_PREVIOUS_VALUE_CANNOT_BE_NULL);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_newValue() {
        Changes.of(ERR_NEW_VALUE_CANNOT_BE_NULL, null);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_byte_string_previousValue() {
        Changes.of(null, ByteString.copyFromUtf8(ERR_PREVIOUS_VALUE_CANNOT_BE_NULL));
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_byte_string_newValue() {
        Changes.of(ByteString.copyFromUtf8(ERR_NEW_VALUE_CANNOT_BE_NULL), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_string_values() {
        final String value = ERR_VALUES_CANNOT_BE_EQUAL;
        Changes.of(value, value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_byte_string_values() {
        final ByteString value = ByteString.copyFromUtf8(ERR_VALUES_CANNOT_BE_EQUAL);
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

    @Test
    public void create_byte_string_value_change() {
        final ByteString previousValue = ByteString.copyFromUtf8(randomUuid());
        final ByteString newValue = ByteString.copyFromUtf8(randomUuid());

        final BytesChange result = Changes.of(previousValue, newValue);

        assertEquals(previousValue, result.getPreviousValue());
        assertEquals(newValue, result.getNewValue());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Timestamp_previousValue() {
        Changes.of(null, Timestamps2.getCurrentTime());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Timestamp_newValue() {
        Changes.of(Timestamps2.getCurrentTime(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_Timestamp_values() {
        final Timestamp now = Timestamps2.getCurrentTime();
        Changes.of(now, now);
    }

    @Test
    public void create_TimestampChange_instance() {
        final Timestamp fiveMinutesAgo = TimeTests.Past.minutesAgo(5);
        final Timestamp now = Timestamps2.getCurrentTime();

        final TimestampChange result = Changes.of(fiveMinutesAgo, now);

        assertEquals(fiveMinutesAgo, result.getPreviousValue());
        assertEquals(now, result.getNewValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_boolean_values() {
        final boolean value = true;
        Changes.of(value, value);
    }

    @Test
    public void create_boolean_value_change() {
        final boolean s1 = true;
        final boolean s2 = false;

        final BooleanChange result = Changes.of(s1, s2);

        assertTrue(Boolean.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Boolean.compare(s2, result.getNewValue()) == 0);
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

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_float_values() {
        final float value = 1543.0f;
        Changes.of(value, value);
    }

    @Test
    public void create_float_value_change() {
        final float s1 = 1473.0219f;
        final float s2 = 1543.0524f;

        final FloatChange result = Changes.of(s1, s2);

        assertTrue(Float.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Float.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_int32_values() {
        final int value = 1614;
        Changes.of(value, value);
    }

    @Test
    public void create_int32_value_change() {
        final int s1 = 1550;
        final int s2 = 1616;

        final Int32Change result = Changes.ofInt32(s1, s2);

        assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_int64_values() {
        final long value = 1666L;
        Changes.of(value, value);
    }

    @Test
    public void create_int64_value_change() {
        final long s1 = 16420225L;
        final long s2 = 17270320L;

        final Int64Change result = Changes.ofInt64(s1, s2);

        assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Long.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_uint32_values() {
        final int value = 1776;
        Changes.ofUInt32(value, value);
    }

    @Test
    public void create_uint32_value_change() {
        final int s1 = 16440925;
        final int s2 = 17100919;

        final UInt32Change result = Changes.ofUInt32(s1, s2);

        assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_uint64_values() {
        final long value = 1690L;
        Changes.ofUInt64(value, value);
    }

    @Test
    public void create_uint64_value_change() {
        final long s1 = 16290414L;
        final long s2 = 16950708L;

        final UInt64Change result = Changes.ofUInt64(s1, s2);

        assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Long.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_sint32_values() {
        final int value = 1694;
        Changes.ofSInt32(value, value);
    }

    @Test
    public void create_sint32_value_change() {
        final int s1 = 16550106;
        final int s2 = 17050816;

        final SInt32Change result = Changes.ofSInt32(s1, s2);

        assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_sint64_values() {
        final long value = 1729L;
        Changes.ofSInt64(value, value);
    }

    @Test
    public void create_sint64_value_change() {
        final long s1 = 1666L;
        final long s2 = 1736L;

        final SInt64Change result = Changes.ofSInt64(s1, s2);

        assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Long.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_fixed32_values() {
        final int value = 1736;
        Changes.ofFixed32(value, value);
    }

    @Test
    public void create_fixed32_value_change() {
        final int s1 = 17070415;
        final int s2 = 17830918;

        final Fixed32Change result = Changes.ofFixed32(s1, s2);

        assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_fixed64_values() {
        final long value = 1755L;
        Changes.ofFixed64(value, value);
    }

    @Test
    public void create_fixed64_value_change() {
        final long s1 = 17240422L;
        final long s2 = 18040212L;

        final Fixed64Change result = Changes.ofFixed64(s1, s2);

        assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Long.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_sfixed32_values() {
        final int value = 1614;
        Changes.ofSfixed32(value, value);
    }

    @Test
    public void create_sfixed32_value_change() {
        final int s1 = 1550;
        final int s2 = 1616;

        final Sfixed32Change result = Changes.ofSfixed32(s1, s2);

        assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_equal_sfixed64_values() {
        final long value = 1666L;
        Changes.ofSfixed64(value, value);
    }

    @Test
    public void create_sfixed64_value_change() {
        final long s1 = 16420225L;
        final long s2 = 17270320L;

        final Sfixed64Change result = Changes.ofSfixed64(s1, s2);

        assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
        assertTrue(Long.compare(s2, result.getNewValue()) == 0);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(ByteString.class, ByteString.EMPTY)
                .setDefault(Timestamp.class, Timestamps2.getCurrentTime())
                .testAllPublicStaticMethods(Changes.class);
    }
}
