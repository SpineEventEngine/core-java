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
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.change.MessageMismatch.expectedDefault;
import static io.spine.change.MessageMismatch.expectedNotDefault;
import static io.spine.change.MessageMismatch.unexpectedValue;
import static io.spine.change.MessageMismatch.unpackActual;
import static io.spine.change.MessageMismatch.unpackExpected;
import static io.spine.change.MessageMismatch.unpackNewValue;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MessageMismatch should")
class MessageMismatchTest {

    private static final StringValue EXPECTED = toMessage("expected_value");
    private static final StringValue ACTUAL = toMessage("actual-value");
    private static final StringValue NEW_VALUE = toMessage("new-value");
    private static final StringValue DEFAULT_VALUE = StringValue.getDefaultInstance();
    private static final int VERSION = 1;

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(MessageMismatch.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(MessageMismatch.class);
    }

    @SuppressWarnings({"InnerClassMayBeStatic" /* JUnit nested classes cannot be static */,
                       "DuplicateStringLiteralInspection" /* Nested class display name similar to
                                                          others */})
    @Nested
    @DisplayName("create ValueMismatch instance")
    class Create {

        @Test
        @DisplayName("for expected default value")
        void forExpectedDefault() {
            final ValueMismatch mismatch = expectedDefault(ACTUAL, NEW_VALUE, VERSION);

            assertEquals(DEFAULT_VALUE, unpackExpected(mismatch));
            assertEquals(ACTUAL, unpackActual(mismatch));
            assertEquals(NEW_VALUE, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for unexpected default when clearing")
        void forUnexpectedDefaultWhenClearing() {
            final ValueMismatch mismatch = expectedNotDefault(EXPECTED, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch));

            // Check that the actual value is default.
            assertEquals(DEFAULT_VALUE, unpackActual(mismatch));

            // Check that newValue has default value as the command intends to clear the field.
            assertEquals(DEFAULT_VALUE, unpackNewValue(mismatch));

            assertEquals(VERSION, mismatch.getVersion());
        }

        @Test
        @DisplayName("for unexpected default when changing")
        void forUnexpectedDefaultWhenChanging() {
            final ValueMismatch mismatch = expectedNotDefault(EXPECTED, NEW_VALUE, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch));

            // Check that the actual value is default.
            assertEquals(DEFAULT_VALUE, unpackActual(mismatch));

            assertEquals(NEW_VALUE, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }

        @SuppressWarnings("Duplicates") // Common test case for different Mismatches.
        @Test
        @DisplayName("for unexpected value")
        void forUnexpectedValue() {
            final ValueMismatch mismatch = unexpectedValue(EXPECTED, ACTUAL, NEW_VALUE, VERSION);

            assertEquals(EXPECTED, unpackExpected(mismatch));
            assertEquals(ACTUAL, unpackActual(mismatch));
            assertEquals(NEW_VALUE, unpackNewValue(mismatch));
            assertEquals(VERSION, mismatch.getVersion());
        }
    }
}
