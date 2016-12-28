/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.ByteString;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Checking of parameters for working with changes.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
public class Preconditions {

    private static final String NEW_VALUE_CANNOT_BE_EMPTY = "newValue cannot be empty";
    private static final String VALUES_CANNOT_BE_EQUAL = "newValue cannot be equal to previousValue";

    private Preconditions() {
    }

    public static void checkNotEqual(int previousValue, int newValue) {
        com.google.common.base.Preconditions.checkArgument(Integer.compare(newValue, previousValue) != 0, VALUES_CANNOT_BE_EQUAL);
    }

    public static void checkNotEqual(long previousValue, long newValue) {
        com.google.common.base.Preconditions.checkArgument(Long.compare(newValue, previousValue) != 0, VALUES_CANNOT_BE_EQUAL);
    }

    public static void checkNotEqual(float previousValue, float newValue) {
        checkArgument(Float.compare(newValue, previousValue) != 0, VALUES_CANNOT_BE_EQUAL);
    }

    public static void checkNotEqual(double previousValue, double newValue) {
        checkArgument(Double.compare(newValue, previousValue) != 0, VALUES_CANNOT_BE_EQUAL);
    }

    public static <T> void checkNotEqual(T previousValue, T newValue) {
        com.google.common.base.Preconditions.checkArgument(!newValue.equals(previousValue), VALUES_CANNOT_BE_EQUAL);
    }

    public static void checkNewValueNotEmpty(ByteString newValue) {
        com.google.common.base.Preconditions.checkArgument(!newValue.isEmpty(), NEW_VALUE_CANNOT_BE_EMPTY);
    }

    public static void checkNewValueNotEmpty(String newValue) {
        com.google.common.base.Preconditions.checkArgument(!newValue.isEmpty(), NEW_VALUE_CANNOT_BE_EMPTY);
    }
}
