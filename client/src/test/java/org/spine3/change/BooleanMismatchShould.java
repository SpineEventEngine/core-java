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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.change.BooleanMismatch.expectedFalse;
import static org.spine3.change.BooleanMismatch.expectedTrue;
import static org.spine3.change.BooleanMismatch.unpackActual;
import static org.spine3.change.BooleanMismatch.unpackExpected;
import static org.spine3.change.BooleanMismatch.unpackNewValue;
import static org.spine3.change.IntMismatch.of;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

public class BooleanMismatchShould {

    private static final int VERSION = 2;

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(BooleanMismatch.class));
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
        final ValueMismatch mismatch = of(1, 2, 3, VERSION);
        BooleanMismatch.unpackExpected(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackActual_if_its_not_a_BooleanMismatch() {
        final ValueMismatch mismatch = of(1, 2, 3, VERSION);
        BooleanMismatch.unpackActual(mismatch);
    }

    @Test(expected = RuntimeException.class)
    public void not_unpackNewValue_if_its_not_a_BooleanMismatch() {
        final ValueMismatch mismatch = of(1, 2, 3, VERSION);
        BooleanMismatch.unpackNewValue(mismatch);
    }
}
