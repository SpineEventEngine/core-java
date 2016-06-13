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

import org.junit.Test;
import com.google.protobuf.StringValue;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;
/**
 * @author Andrey Lavrov
 */
public class MismatchShould {

    private static final String REQUESTED = "requested";
    private static final String EXPECTED = "expected";
    private static final String FOUND = "found";
    private static final int VERSION = 0;
    private static final String DEFAULT_VALUE = "";

    @Test
    public void has_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Mismatch.class));
    }

    @Test
    public void return_default_expected_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(null, FOUND, REQUESTED, VERSION);
        final String expected = mismatch.getExpected()
                                        .toString();

        assertEquals(DEFAULT_VALUE, expected);
    }

    @Test
    public void return_default_found_value_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, null, REQUESTED, VERSION);
        final String actual = mismatch.getActual()
                                      .toString();

        assertEquals(DEFAULT_VALUE, actual);
    }

    @Test
    public void return_mismatch_object_with_specified_values_string_overload() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, FOUND, REQUESTED, VERSION);
        final StringValue expected = fromAny(mismatch.getExpected());
        final StringValue found = fromAny(mismatch.getActual());
        final StringValue requested = fromAny(mismatch.getRequested());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(FOUND, found.getValue());
        assertEquals(REQUESTED, requested.getValue());
    }

    @Test
    public void return_default_expected_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = Mismatch.of(null, newStringValue(FOUND), newStringValue(REQUESTED), VERSION);
        final String expected = mismatch.getExpected()
                                        .toString();

        assertEquals(DEFAULT_VALUE, expected);
    }

    @Test
    public void return_default_found_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), null, newStringValue(REQUESTED), VERSION);
        final String actual = mismatch.getActual()
                                      .toString();

        assertEquals(DEFAULT_VALUE, actual);
    }

    @Test
    public void return_mismatch_object_with_specified_values_message_overload() {
        final ValueMismatch mismatch = Mismatch.of(newStringValue(EXPECTED), newStringValue(FOUND), newStringValue(REQUESTED), VERSION);
        final StringValue expected = fromAny(mismatch.getExpected());
        final StringValue found = fromAny(mismatch.getActual());
        final StringValue requested = fromAny(mismatch.getRequested());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(FOUND, found.getValue());
        assertEquals(REQUESTED, requested.getValue());
    }
}
