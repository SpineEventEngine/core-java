/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.change.BooleanMismatch.expectedFalse;
import static io.spine.change.BooleanMismatch.expectedTrue;
import static io.spine.change.BooleanMismatch.unpackActual;
import static io.spine.change.BooleanMismatch.unpackExpected;
import static io.spine.change.BooleanMismatch.unpackNewValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("BooleanMismatch should")
class BooleanMismatchTest {

    private static final int VERSION = 2;

    @Test
    @DisplayName("have private parameterless constructor")
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(BooleanMismatch.class);
    }

    @Test
    @DisplayName("not accept nulls for non-Nullable public method arguments")
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(BooleanMismatch.class);
    }

    @Test
    @DisplayName("create ValueMismatch instance for `expectedFalse` case")
    void createForExpectedFalse() {
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
    @DisplayName("create ValueMismatch instance for `expectedTrue` case")
    void createForExpectedTrue() {
        final boolean expected = true;
        final boolean actual = false;
        final boolean newValue = false;
        final ValueMismatch mismatch = expectedTrue(VERSION);

        assertEquals(expected, unpackExpected(mismatch));
        assertEquals(actual, unpackActual(mismatch));
        assertEquals(newValue, unpackNewValue(mismatch));
        assertEquals(VERSION, mismatch.getVersion());
    }

    @Test
    @DisplayName("not unpackExpected if passed ValueMismatch is not a BooleanMismatch")
    void notUnpackExpectedForWrongType() {
        final ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
        assertThrows(RuntimeException.class, () -> BooleanMismatch.unpackExpected(mismatch));
    }

    @Test
    @DisplayName("not unpackActual if passed ValueMismatch is not a BooleanMismatch")
    void notUnpackActualForWrongType() {
        final ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
        assertThrows(RuntimeException.class, () -> BooleanMismatch.unpackActual(mismatch));
    }

    @Test
    @DisplayName("not unpackNewValue if passed ValueMismatch is not a BooleanMismatch")
    void notUnpackNewValueForWrongType() {
        final ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
        assertThrows(RuntimeException.class, () -> BooleanMismatch.unpackNewValue(mismatch));
    }
}
