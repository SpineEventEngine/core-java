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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.change.BooleanMismatch.expectedFalse;
import static io.spine.change.BooleanMismatch.expectedTrue;
import static io.spine.change.BooleanMismatch.unpackActual;
import static io.spine.change.BooleanMismatch.unpackExpected;
import static io.spine.change.BooleanMismatch.unpackNewValue;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"InnerClassMayBeStatic" /* JUnit nested classes cannot be static */,
                   "DuplicateStringLiteralInspection" /* A lot of similar test display names */})
@DisplayName("BooleanMismatch should")
class BooleanMismatchTest {

    private static final int VERSION = 2;

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(BooleanMismatch.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(BooleanMismatch.class);
    }

    @Nested
    @DisplayName("create ValueMismatch instance")
    class Create {

        @Test
        @DisplayName("for `expectedFalse` case")
        void forExpectedFalse() {
            boolean expected = false;
            boolean actual = true;
            boolean newValue = true;
            ValueMismatch mismatch = expectedFalse(VERSION);

            assertEquals(expected, unpackExpected(mismatch));
            assertEquals(actual, unpackActual(mismatch));
            assertEquals(newValue, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for `expectedTrue` case")
        void forExpectedTrue() {
            boolean expected = true;
            boolean actual = false;
            boolean newValue = false;
            ValueMismatch mismatch = expectedTrue(VERSION);

            assertEquals(expected, unpackExpected(mismatch));
            assertEquals(actual, unpackActual(mismatch));
            assertEquals(newValue, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }
    }

    @Nested
    @DisplayName("if given non-boolean ValueMismatch, fail to unpack")
    class FailToUnpack {

        @Test
        @DisplayName("expected")
        void expectedWithWrongType() {
            ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
            assertThrows(RuntimeException.class, () -> BooleanMismatch.unpackExpected(mismatch));
        }

        @Test
        @DisplayName("actual")
        void actualWithWrongType() {
            ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
            assertThrows(RuntimeException.class, () -> BooleanMismatch.unpackActual(mismatch));
        }

        @Test
        @DisplayName("new value")
        void newValueWithWrongType() {
            ValueMismatch mismatch = IntMismatch.of(1, 2, 3, VERSION);
            assertThrows(RuntimeException.class, () -> BooleanMismatch.unpackNewValue(mismatch));
        }
    }
}
