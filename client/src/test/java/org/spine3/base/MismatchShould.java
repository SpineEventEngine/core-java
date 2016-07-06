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

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.fromAny;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Andrey Lavrov
 */
public class MismatchShould {

    private static final String REQUESTED = "requested";
    private static final String EXPECTED = "expected";
    private static final String ACTUAL = "ACTUAL";
    private static final int VERSION = 0;
    private static final String DEFAULT_VALUE = "";
    public static final double DELTA = 0.01;

    @Test
    public void has_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Mismatch.class));
    }

    @Test
    public void set_default_expected_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(null, ACTUAL, REQUESTED, VERSION);
        final String expected = mismatch.getExpected()
                                        .toString();

        assertEquals(DEFAULT_VALUE, expected);
    }

    @Test
    public void set_default_actual_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, null, REQUESTED, VERSION);
        final String actual = mismatch.getActual()
                                      .toString();

        assertEquals(DEFAULT_VALUE, actual);
    }

    @Test
    public void return_mismatch_object_with_string_values() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, ACTUAL, REQUESTED, VERSION);
        final StringValue expected = fromAny(mismatch.getExpected());
        final StringValue actual = fromAny(mismatch.getActual());
        final StringValue requested = fromAny(mismatch.getRequested());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(ACTUAL, actual.getValue());
        assertEquals(REQUESTED, requested.getValue());
    }

    @Test
    public void return_mismatch_object_with_int32_values() {
        final int expected = 0;
        final int actual = 1;
        final int requested = 2;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, requested, VERSION);
        final Int32Value expectedWrapper = fromAny(mismatch.getExpected());
        final Int32Value actualWrapper = fromAny(mismatch.getActual());
        final Int32Value requestedWrapper = fromAny(mismatch.getRequested());

        assertEquals(expected, expectedWrapper.getValue());
        assertEquals(actual, actualWrapper.getValue());
        assertEquals(requested, requestedWrapper.getValue());
    }

    @Test
    public void return_mismatch_object_with_int64_values() {
        final long expected = 0L;
        final long actual = 1L;
        final long requested = 2L;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, requested, VERSION);
        final Int64Value expectedWrapped = fromAny(mismatch.getExpected());
        final Int64Value actualWrapped = fromAny(mismatch.getActual());
        final Int64Value requestedWrapped = fromAny(mismatch.getRequested());

        assertEquals(expected, expectedWrapped.getValue());
        assertEquals(actual, actualWrapped.getValue());
        assertEquals(requested, requestedWrapped.getValue());
    }

    @Test
    public void return_mismatch_object_with_float_values() {
        final float expected = 0.0F;
        final float actual = 1.0F;
        final float requested = 2.0F;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, requested, VERSION);
        final FloatValue expectedWrapped = fromAny(mismatch.getExpected());
        final FloatValue actualWrapped = fromAny(mismatch.getActual());
        final FloatValue requestedWrapped = fromAny(mismatch.getRequested());

        assertEquals(expected, expectedWrapped.getValue(), DELTA);
        assertEquals(actual, actualWrapped.getValue(), DELTA);
        assertEquals(requested, requestedWrapped.getValue(), DELTA);
    }

    @Test
    public void return_mismatch_object_with_double_values() {
        final double expected = 0.1;
        final double actual = 0.2;
        final double requested = 0.3;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, requested, VERSION);
        final DoubleValue expectedWrapped = fromAny(mismatch.getExpected());
        final DoubleValue actualWrapped = fromAny(mismatch.getActual());
        final DoubleValue requestedWrapped = fromAny(mismatch.getRequested());

        assertEquals(expected, expectedWrapped.getValue(), DELTA);
        assertEquals(actual, actualWrapped.getValue(), DELTA);
        assertEquals(requested, requestedWrapped.getValue(), DELTA);
    }

    @Test
    public void return_mismatch_object_with_boolean_values() {
        final boolean expected = true;
        final boolean actual = false;
        final boolean requested = true;
        final ValueMismatch mismatch = Mismatch.of(expected, actual, requested, VERSION);
        final BoolValue expectedWrapped = fromAny(mismatch.getExpected());
        final BoolValue actualWrapped = fromAny(mismatch.getActual());
        final BoolValue requestedWrapped = fromAny(mismatch.getRequested());

        assertEquals(expected, expectedWrapped.getValue());
        assertEquals(actual, actualWrapped.getValue());
        assertEquals(requested, requestedWrapped.getValue());
    }

    @Test
    public void set_default_expected_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = Mismatch.of(null, newStringValue(ACTUAL), newStringValue(REQUESTED), VERSION);
        final String expected = mismatch.getExpected()
                                        .toString();

        assertEquals(DEFAULT_VALUE, expected);
    }

    @Test
    public void set_default_actual_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), null, newStringValue(REQUESTED), VERSION);
        final String actual = mismatch.getActual()
                                      .toString();

        assertEquals(DEFAULT_VALUE, actual);
    }

    @Test
    public void return_mismatch_object_with_message_values() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), newStringValue(ACTUAL), newStringValue(REQUESTED), VERSION);
        final StringValue expected = fromAny(mismatch.getExpected());
        final StringValue actual = fromAny(mismatch.getActual());
        final StringValue requested = fromAny(mismatch.getRequested());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(ACTUAL, actual.getValue());
        assertEquals(REQUESTED, requested.getValue());
    }
}
