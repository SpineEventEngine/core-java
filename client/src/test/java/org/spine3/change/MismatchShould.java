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

package org.spine3.change;

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.change.Mismatch.getActualString;
import static org.spine3.change.Mismatch.getExpectedString;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Andrey Lavrov
 */
public class MismatchShould {

    private static final String EXPECTED = "expected";
    private static final String ACTUAL = "ACTUAL";
    private static final String NEW_VALUE = "neW ValuE";
    private static final int VERSION = 0;
    private static final String DEFAULT_VALUE = "";
    private static final double DELTA = 0.01;

    @Test
    public void has_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Mismatch.class));
    }

    @Test
    public void set_default_expected_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(null, ACTUAL, NEW_VALUE, VERSION);
        final String expected = mismatch.getExpectedPreviousValue()
                                        .toString();

        assertEquals(DEFAULT_VALUE, expected);
    }

    @Test
    public void set_default_actual_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, null, NEW_VALUE, VERSION);
        final String actual = mismatch.getActualPreviousValue()
                                      .toString();

        assertEquals(DEFAULT_VALUE, actual);
    }

    @Test
    public void return_mismatch_object_with_string_values() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, ACTUAL, NEW_VALUE, VERSION);
        final StringValue expected = unpack(mismatch.getExpectedPreviousValue());
        final StringValue actual = unpack(mismatch.getActualPreviousValue());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(ACTUAL, actual.getValue());
    }

    @Test
    public void return_mismatch_object_with_int32_values() {
        final int expected = 0;
        final int actual = 1;
        final int newValue = 2;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, newValue, VERSION);
        final Int32Value expectedWrapper = unpack(mismatch.getExpectedPreviousValue());
        final Int32Value actualWrapper = unpack(mismatch.getActualPreviousValue());

        assertEquals(expected, expectedWrapper.getValue());
        assertEquals(actual, actualWrapper.getValue());
    }

    @Test
    public void return_mismatch_object_with_int64_values() {
        final long expected = 0L;
        final long actual = 1L;
        final long newValue = 5L;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, newValue, VERSION);
        final Int64Value expectedWrapped = unpack(mismatch.getExpectedPreviousValue());
        final Int64Value actualWrapped = unpack(mismatch.getActualPreviousValue());

        assertEquals(expected, expectedWrapped.getValue());
        assertEquals(actual, actualWrapped.getValue());
    }

    @Test
    public void return_mismatch_object_with_float_values() {
        final float expected = 0.0F;
        final float actual = 1.0F;
        final float newValue = 19.0F;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, newValue, VERSION);
        final FloatValue expectedWrapped = unpack(mismatch.getExpectedPreviousValue());
        final FloatValue actualWrapped = unpack(mismatch.getActualPreviousValue());

        assertEquals(expected, expectedWrapped.getValue(), DELTA);
        assertEquals(actual, actualWrapped.getValue(), DELTA);
    }

    @Test
    public void return_mismatch_object_with_double_values() {
        final double expected = 0.1;
        final double actual = 0.2;
        final double newValue = 0.5;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, newValue, VERSION);
        final DoubleValue expectedWrapped = unpack(mismatch.getExpectedPreviousValue());
        final DoubleValue actualWrapped = unpack(mismatch.getActualPreviousValue());

        assertEquals(expected, expectedWrapped.getValue(), DELTA);
        assertEquals(actual, actualWrapped.getValue(), DELTA);
    }

    @Test
    public void return_mismatch_object_with_boolean_values() {
        final boolean expected = true;
        final boolean actual = false;
        final boolean newValue = false;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, newValue, VERSION);
        final BoolValue expectedWrapped = unpack(mismatch.getExpectedPreviousValue());
        final BoolValue actualWrapped = unpack(mismatch.getActualPreviousValue());

        assertEquals(expected, expectedWrapped.getValue());
        assertEquals(actual, actualWrapped.getValue());
    }

    @Test
    public void set_default_expected_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = Mismatch.of(null, newStringValue(ACTUAL), newStringValue(NEW_VALUE), VERSION);
        final String expected = mismatch.getExpectedPreviousValue()
                                        .toString();

        assertEquals(DEFAULT_VALUE, expected);
    }

    @Test
    public void set_default_actual_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), null, newStringValue(NEW_VALUE), VERSION);
        final String actual = mismatch.getActualPreviousValue()
                                      .toString();

        assertEquals(DEFAULT_VALUE, actual);
    }

    @Test
    public void return_mismatch_object_with_message_values() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), newStringValue(ACTUAL), newStringValue(NEW_VALUE), VERSION);
        final StringValue expected = unpack(mismatch.getExpectedPreviousValue());
        final StringValue actual = unpack(mismatch.getActualPreviousValue());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(ACTUAL, actual.getValue());
    }

    @Test
    public void return_expected_string() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), newStringValue(ACTUAL), newStringValue(NEW_VALUE), VERSION);
        assertEquals(EXPECTED, getExpectedString(mismatch));
    }

    @Test
    public void return_actual_string() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), newStringValue(ACTUAL), newStringValue(NEW_VALUE), VERSION);
        assertEquals(ACTUAL, getActualString(mismatch));
    }
}
