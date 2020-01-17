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

import com.google.protobuf.ByteString;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Checking of parameters for working with changes.
 */
@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
final class ChangePreconditions {

    private static final String NEW_VALUE_CANNOT_BE_EMPTY =
            "newValue cannot be empty";

    private static final String VALUES_CANNOT_BE_EQUAL =
            "newValue cannot be equal to previousValue";

    private static final String ERR_CANNOT_BE_EQUAL =
            "`expected` and `actual` cannot be equal in ValueMismatch";

    /** Prevent instantiation of this utility class. */
    private ChangePreconditions() {
    }

    /**
     * Ensures that parameters are not equal.
     *
     * @throws IllegalArgumentException in case if values are equal
     */
    static void checkNotEqual(int previousValue, int newValue) {
        checkArgument(newValue != previousValue, VALUES_CANNOT_BE_EQUAL);
    }

    /**
     * Ensures that parameters are not equal.
     *
     * @throws IllegalArgumentException in case if values are equal
     */
    static void checkNotEqual(long previousValue, long newValue) {
        checkArgument(newValue != previousValue, VALUES_CANNOT_BE_EQUAL);
    }

    /**
     * Ensures that parameters are not equal.
     *
     * @throws IllegalArgumentException in case if values are equal
     */
    static void checkNotEqual(float previousValue, float newValue) {
        checkArgument(Float.compare(newValue, previousValue) != 0,
                      VALUES_CANNOT_BE_EQUAL);
    }

    /**
     * Ensures that parameters are not equal.
     *
     * @throws IllegalArgumentException in case if values are equal
     */
    static void checkNotEqual(double previousValue, double newValue) {
        checkArgument(Double.compare(newValue, previousValue) != 0,
                      VALUES_CANNOT_BE_EQUAL);
    }

    /**
     * Ensures that parameters are not equal.
     *
     * @throws IllegalArgumentException in case if values are equal
     */
    static <T> void checkNotEqual(T previousValue, T newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkArgument(!newValue.equals(previousValue), VALUES_CANNOT_BE_EQUAL);
    }

    /**
     * Ensures that parameter size is more than 0.
     *
     * @throws IllegalArgumentException in case if parameter is empty
     */
    static void checkNewValueNotEmpty(ByteString newValue) {
        checkNotNull(newValue);
        checkArgument(!newValue.isEmpty(), NEW_VALUE_CANNOT_BE_EMPTY);
    }

    /**
     * Ensures that parameter size is more than 0.
     *
     * @throws IllegalArgumentException in case if parameter is empty
     */
    static void checkNewValueNotEmpty(String newValue) {
        checkNotNull(newValue);
        checkArgument(!newValue.isEmpty(), NEW_VALUE_CANNOT_BE_EMPTY);
    }

    static void checkNotNullOrEqual(Object expected, Object actual) {
        checkNotNull(expected);
        checkNotNull(actual);
        checkArgument(!expected.equals(actual), ERR_CANNOT_BE_EQUAL);
    }
}
