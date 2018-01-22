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

import static io.spine.change.BooleanMismatch.expectedFalse;
import static io.spine.change.BooleanMismatch.expectedTrue;
import static io.spine.change.BooleanMismatch.unpackActual;
import static io.spine.change.BooleanMismatch.unpackExpected;
import static io.spine.change.BooleanMismatch.unpackNewValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

public class BooleanMismatchShould {

    private static final int VERSION = 2;

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(BooleanMismatch.class);
    }

    @Test
    public void create_ValueMismatch_instance_for_expectedFalse_case() {
        final boolean expected = false;
        final boolean actual = true;
        final boolean newValue = true;
        final ValueMismatch mismatch = expectedFalse(VERSION);

        assertEquals(expected, unpackExpected(mismatch));
        assertEquals(actual, unpackActual(mismatch));
        assertEquals(newValue, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    public void create_ValueMismatch_instance_for_expectedTrue_case() {
        final boolean expected = true;
        final boolean actual = false;
        final boolean newValue = false;
        final ValueMismatch mismatch = expectedTrue(VERSION);

        assertEquals(expected, unpackExpected(mismatch));
        assertEquals(actual, unpackActual(mismatch));
        assertEquals(newValue, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackExpected_if_its_not_a_BooleanMismatch() {
        final ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
        BooleanMismatch.unpackExpected(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackActual_if_its_not_a_BooleanMismatch() {
        final ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
        BooleanMismatch.unpackActual(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackNewValue_if_its_not_a_BooleanMismatch() {
        final ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
        BooleanMismatch.unpackNewValue(mismatch);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(BooleanMismatch.class);
    }
}
