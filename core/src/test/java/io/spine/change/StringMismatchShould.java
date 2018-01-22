/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.change;

import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import static io.spine.change.BooleanMismatch.expectedTrue;
import static io.spine.change.StringMismatch.expectedEmpty;
import static io.spine.change.StringMismatch.expectedNotEmpty;
import static io.spine.change.StringMismatch.unexpectedValue;
import static io.spine.change.StringMismatch.unpackActual;
import static io.spine.change.StringMismatch.unpackExpected;
import static io.spine.change.StringMismatch.unpackNewValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

public class StringMismatchShould {

    private static final String EXPECTED = "expected string";
    private static final String ACTUAL = "actual string";
    private static final String NEW_VALUE = "new value to set";

    private static final int VERSION = 1;

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(StringMismatch.class);
    }

    @Test
    public void create_instance_for_expected_empty_string() {
        final ValueMismatch mismatch = expectedEmpty(ACTUAL, NEW_VALUE, VERSION);

        assertEquals("", unpackExpected(mismatch));
        assertEquals(ACTUAL, unpackActual(mismatch));
        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_unexpected_empty_string() {
        final ValueMismatch mismatch = expectedNotEmpty(EXPECTED, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch));

        // Check that the actual value from the mismatch is an empty string.
        assertEquals("", unpackActual(mismatch));

        // We wanted to clear the field. Check that `newValue` is an empty string.
        assertEquals("", unpackNewValue(mismatch));

        // Check version.
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

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_same_expected_and_actual() {
        final String value = "same-same";
        unexpectedValue(value, value, NEW_VALUE, VERSION);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackExpected_if_its_not_a_IntMismatch() {
        final ValueMismatch mismatch = expectedTrue(VERSION);
        unpackExpected(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackActual_if_its_not_a_IntMismatch() {
        final ValueMismatch mismatch = expectedTrue(VERSION);
        unpackActual(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackNewValue_if_its_not_a_IntMismatch() {
        final ValueMismatch mismatch = expectedTrue(VERSION);
        unpackNewValue(mismatch);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(StringMismatch.class);
    }
}
