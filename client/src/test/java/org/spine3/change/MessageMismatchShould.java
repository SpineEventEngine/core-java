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
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

public class MessageMismatchShould {

    private static final String EXPECTED = "expected_value";
    private static final String ACTUAL = "actual-value";
    private static final String NEW_VALUE = "new-value";
    private static final String DEFAULT_VALUE = "";
    private static final int VERSION = 1;

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(MessageMismatch.class));
    }

    @Test
    public void set_default_expected_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = MessageMismatch.of(null, newStringValue(ACTUAL), newStringValue(NEW_VALUE), VERSION);
        final String expected = mismatch.getExpectedPreviousValue()
                                        .toString();

        assertEquals(DEFAULT_VALUE, expected);
    }

    @Test
    public void set_default_actual_value_if_it_was_passed_as_null_message_overload() {
        final ValueMismatch mismatch = MessageMismatch.of(newStringValue(EXPECTED), null, newStringValue(NEW_VALUE), VERSION);
        final String actual = mismatch.getActualPreviousValue()
                                      .toString();

        assertEquals(DEFAULT_VALUE, actual);
    }

    @Test
    public void return_mismatch_object_with_message_values() {
        final ValueMismatch mismatch = MessageMismatch.of(newStringValue(EXPECTED), newStringValue(ACTUAL), newStringValue(NEW_VALUE), VERSION);
        final StringValue expected = unpack(mismatch.getExpectedPreviousValue());
        final StringValue actual = unpack(mismatch.getActualPreviousValue());

        assertEquals(EXPECTED, expected.getValue());
        assertEquals(ACTUAL, actual.getValue());
    }

}
