/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.change.BooleanMismatch.expectedTrue;
import static io.spine.change.FloatMismatch.expectedNonZero;
import static io.spine.change.FloatMismatch.expectedZero;
import static io.spine.change.FloatMismatch.unexpectedValue;
import static io.spine.change.FloatMismatch.unpackActual;
import static io.spine.change.FloatMismatch.unpackExpected;
import static io.spine.change.FloatMismatch.unpackNewValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("FloatMismatch should")
class FloatMismatchTest extends UtilityClassTest<FloatMismatch> {

    private static final int VERSION = 100;
    private static final float EXPECTED = 18.39f;
    private static final float ACTUAL = 19.01f;
    private static final float NEW_VALUE = 14.52f;
    private static final float DELTA = 0.01f;

    FloatMismatchTest() {
        super(FloatMismatch.class);
    }

    @Nested
    @DisplayName("create ValueMismatch instance")
    class Create {

        @Test
        @DisplayName("from given float values")
        void withFloats() {
            ValueMismatch mismatch = FloatMismatch.of(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch), DELTA);
            assertEquals(ACTUAL, unpackActual(mismatch), DELTA);
            assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for expected zero amount")
        void forExpectedZero() {
            double expected = 0.0f;
            ValueMismatch mismatch = expectedZero(ACTUAL, NEW_VALUE, VERSION);

            assertEquals(expected, unpackExpected(mismatch), DELTA);
            assertEquals(ACTUAL, unpackActual(mismatch), DELTA);
            assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for expected non zero amount")
        void forExpectedNonZero() {
            float actual = 0.0f;
            ValueMismatch mismatch = expectedNonZero(EXPECTED, NEW_VALUE, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch), DELTA);
            assertEquals(actual, unpackActual(mismatch), DELTA);
            assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for unexpected float value")
        void forUnexpectedFloat() {
            ValueMismatch mismatch = unexpectedValue(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch), DELTA);
            assertEquals(ACTUAL, unpackActual(mismatch), DELTA);
            assertEquals(NEW_VALUE, unpackNewValue(mismatch), DELTA);
            assertEquals(VERSION, mismatch.getVersion());
        }
    }

    @Test
    @DisplayName("not accept same expected and actual values")
    void notAcceptSameExpectedAndActual() {
        float value = 19.19f;
        assertThrows(IllegalArgumentException.class,
                     () -> unexpectedValue(value, value, NEW_VALUE, VERSION));
    }

    @Nested
    @DisplayName("if given non-float ValueMismatch, fail to unpack")
    class FailToUnpack {

        @Test
        @DisplayName("expected")
        void expectedWithWrongType() {
            ValueMismatch mismatch = expectedTrue(VERSION);
            assertThrows(RuntimeException.class, () -> unpackExpected(mismatch));
        }

        @Test
        @DisplayName("actual float")
        void actualWithWrongType() {
            ValueMismatch mismatch = expectedTrue(VERSION);
            assertThrows(RuntimeException.class, () -> unpackActual(mismatch));
        }

        @Test
        @DisplayName("new value")
        void newValueWithWrongType() {
            ValueMismatch mismatch = expectedTrue(VERSION);
            assertThrows(RuntimeException.class, () -> unpackNewValue(mismatch));
        }
    }
}
