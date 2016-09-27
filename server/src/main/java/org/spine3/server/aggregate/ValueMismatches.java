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

package org.spine3.server.aggregate;

import com.google.protobuf.Any;
import org.spine3.base.ValueMismatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Values.newStringValue;

/**
 * Utilities for working with {@link org.spine3.base.ValueMismatch}.
 *
 * @author Alexander Yevsyukov
 */
public class ValueMismatches {

    private ValueMismatches() {}

    private static void checkNotEqual(Object expected, Object actual) {
        checkNotNull(expected);
        checkNotNull(actual);
        if (expected.equals(actual)) {
            throw new IllegalArgumentException("Expected and actual are equal: " + expected);
        }
    }

    /**
     * Creates instance for mismatch of a string values.
     */
    public static ValueMismatch of(String expected, String actual) {
        checkNotEqual(expected, actual);

        final Any packedExpected = Any.pack(newStringValue(expected));
        final Any packedActual = Any.pack(newStringValue(actual));

        return of(packedExpected, packedActual);
    }

    /**
     * Creates instance for mismatch of objects packed into {@code Any}.
     */
    public static ValueMismatch of(Any packedExpected, Any packedActual) {
        final ValueMismatch result = ValueMismatch.newBuilder()
                                                  .setExpected(packedExpected)
                                                  .setActual(packedActual)
                                                  .build();
        return result;
    }
}
