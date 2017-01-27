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

import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.test.NullToleranceTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.change.MessageMismatch.expectedDefault;
import static org.spine3.change.MessageMismatch.expectedNotDefault;
import static org.spine3.change.MessageMismatch.unexpectedValue;
import static org.spine3.change.MessageMismatch.unpackActual;
import static org.spine3.change.MessageMismatch.unpackExpected;
import static org.spine3.change.MessageMismatch.unpackNewValue;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

public class MessageMismatchShould {

    private static final StringValue EXPECTED = newStringValue("expected_value");
    private static final StringValue ACTUAL = newStringValue("actual-value");
    private static final StringValue NEW_VALUE = newStringValue("new-value");
    private static final StringValue DEFAULT_VALUE = StringValue.getDefaultInstance();
    private static final int VERSION = 1;

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(MessageMismatch.class));
    }

    @Test
    public void create_instance_for_expected_default_value() {
        final ValueMismatch mismatch = expectedDefault(ACTUAL, NEW_VALUE, VERSION);

        assertEquals(DEFAULT_VALUE, unpackExpected(mismatch));
        assertEquals(ACTUAL, unpackActual(mismatch));
        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_unexpected_default_when_clearing() {
        final ValueMismatch mismatch = expectedNotDefault(EXPECTED, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch));

        // Check that the actual value is default.
        assertEquals(DEFAULT_VALUE, unpackActual(mismatch));

        // Check that newValue has default value as the command intends to clear the field.
        assertEquals(DEFAULT_VALUE, unpackNewValue(mismatch));

        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_unexpected_default_when_changing() {
        final ValueMismatch mismatch = expectedNotDefault(EXPECTED, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch));

        // Check that the actual value is default.
        assertEquals(DEFAULT_VALUE, unpackActual(mismatch));

        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_unexpected_value() {
        final ValueMismatch mismatch = unexpectedValue(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch));
        assertEquals(ACTUAL, unpackActual(mismatch));
        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void pass_the_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(MessageMismatch.class)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }
}
