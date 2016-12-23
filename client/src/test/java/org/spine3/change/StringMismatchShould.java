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

import com.google.protobuf.StringValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

public class StringMismatchShould {

    private static final String EXPECTED = "expected string";
    private static final String ACTUAL = "actual string";
    private static final String NEW_VALUE = "new value to set";
    private static final String DEFAULT_VALUE = "";

    private static final int VERSION = 1;

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(StringMismatch.class));
    }

    @Test
    public void set_default_expected_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = StringMismatch.of(null, ACTUAL, NEW_VALUE, VERSION);
        final StringValue expected = unpack(mismatch.getExpectedPreviousValue());

        assertEquals(DEFAULT_VALUE, expected.getValue());
    }

    @Test
    public void set_default_actual_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = StringMismatch.of(EXPECTED, null, NEW_VALUE, VERSION);
        final StringValue actual = unpack(mismatch.getActualPreviousValue());

        assertEquals(DEFAULT_VALUE, actual.getValue());
    }

    @Test
    public void set_default_new_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = StringMismatch.of(EXPECTED, ACTUAL, null, VERSION);
        final StringValue newValue = unpack(mismatch.getNewValue());

        assertEquals(DEFAULT_VALUE, newValue.getValue());
    }

    @Test
    public void return_mismatch_object_with_string_values() {
        final ValueMismatch mismatch = StringMismatch.of(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

        final StringValue expected = unpack(mismatch.getExpectedPreviousValue());
        final StringValue actual = unpack(mismatch.getActualPreviousValue());
        final StringValue newValue = unpack(mismatch.getNewValue());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(ACTUAL, actual.getValue());
        assertEquals(NEW_VALUE, newValue.getValue());
    }
}
