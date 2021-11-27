/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import static io.spine.change.IntMismatch.expectedNonZero;
import static io.spine.change.IntMismatch.expectedZero;
import static io.spine.change.IntMismatch.unexpectedValue;
import static io.spine.change.IntMismatch.unpackActual;
import static io.spine.change.IntMismatch.unpackExpected;
import static io.spine.change.IntMismatch.unpackNewValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`IntMismatch` should")
class IntMismatchTest extends UtilityClassTest<IntMismatch> {

    private static final int EXPECTED = 1986;
    private static final int ACTUAL = 1567;
    private static final int NEW_VALUE = 1452;
    private static final int VERSION = 5;

    IntMismatchTest() {
        super(IntMismatch.class);
    }

    @Nested
    @DisplayName("create `ValueMismatch` instance")
    class Create {

        @Test
        @DisplayName("from given int values")
        void withInts() {
            var mismatch = IntMismatch.of(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch));
            assertEquals(ACTUAL, unpackActual(mismatch));
            assertEquals(NEW_VALUE, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for expected zero amount")
        void forExpectedZero() {
            var expected = 0;
            var mismatch = expectedZero(ACTUAL, NEW_VALUE, VERSION);

            assertEquals(expected, unpackExpected(mismatch));
            assertEquals(ACTUAL, unpackActual(mismatch));
            assertEquals(NEW_VALUE, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for expected non-zero amount")
        void forExpectedNonZero() {
            var actual = 0;
            var mismatch = expectedNonZero(EXPECTED, NEW_VALUE, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch));
            assertEquals(actual, unpackActual(mismatch));
            assertEquals(NEW_VALUE, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for unexpected int value")
        void forUnexpectedInt() {
            var mismatch = unexpectedValue(EXPECTED, ACTUAL, NEW_VALUE,
                                           VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch));
            assertEquals(ACTUAL, unpackActual(mismatch));
            assertEquals(NEW_VALUE, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }
    }

    @Test
    @DisplayName("not accept same expected and actual values")
    void notAcceptSameExpectedAndActual() {
        var value = 5;
        assertThrows(IllegalArgumentException.class,
                     () -> unexpectedValue(value, value, NEW_VALUE, VERSION));
    }

    @Nested
    @DisplayName("if given non-int `ValueMismatch`, fail to unpack")
    class FailToUnpack {

        @Test
        @DisplayName("expected")
        void expectedWithWrongType() {
            var mismatch = expectedTrue(VERSION);
            assertThrows(RuntimeException.class, () -> unpackExpected(mismatch));
        }

        @Test
        @DisplayName("actual int")
        void actualWithWrongType() {
            var mismatch = expectedTrue(VERSION);
            assertThrows(RuntimeException.class, () -> unpackActual(mismatch));
        }

        @Test
        @DisplayName("new value")
        void newValueWithWrongType() {
            var mismatch = expectedTrue(VERSION);
            assertThrows(RuntimeException.class, () -> unpackNewValue(mismatch));
        }
    }
}
