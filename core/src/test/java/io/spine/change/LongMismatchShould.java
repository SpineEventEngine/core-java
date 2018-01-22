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
import static io.spine.change.LongMismatch.expectedNonZero;
import static io.spine.change.LongMismatch.expectedZero;
import static io.spine.change.LongMismatch.unexpectedValue;
import static io.spine.change.LongMismatch.unpackActual;
import static io.spine.change.LongMismatch.unpackExpected;
import static io.spine.change.LongMismatch.unpackNewValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

public class LongMismatchShould {

    private static final long EXPECTED = 1839L;
    private static final long ACTUAL = 1900L;
    private static final long NEW_VALUE = 1452L;
    private static final int VERSION = 7;

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(LongMismatch.class);
    }

    @Test
    public void return_mismatch_object_with_int64_values() {
        final ValueMismatch mismatch = LongMismatch.of(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch));
        assertEquals(ACTUAL, unpackActual(mismatch));
        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_expected_zero_amount() {
        final long expected = 0L;
        final ValueMismatch mismatch = expectedZero(ACTUAL, NEW_VALUE, VERSION);

        assertEquals(expected, unpackExpected(mismatch));
        assertEquals(ACTUAL, unpackActual(mismatch));
        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_expected_non_zero_amount() {
        final long actual = 0L;
        final ValueMismatch mismatch = expectedNonZero(EXPECTED, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch));
        assertEquals(actual, unpackActual(mismatch));
        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_instance_for_unexpected_int_value() {
        final ValueMismatch mismatch = unexpectedValue(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

        assertEquals(EXPECTED, unpackExpected(mismatch));
        assertEquals(ACTUAL, unpackActual(mismatch));
        assertEquals(NEW_VALUE, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
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

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_same_expected_and_actual() {
        final long value = 1919L;
        unexpectedValue(value, value, NEW_VALUE, VERSION);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(LongMismatch.class);
    }
}
