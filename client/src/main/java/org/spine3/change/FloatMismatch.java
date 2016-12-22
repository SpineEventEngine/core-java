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

import static org.spine3.protobuf.Values.pack;

/**
 * Utility class for working with {@code float} values in {@link ValueMismatch}es.
 *
 * @author Alexander Yevsyukov
 */
public class FloatMismatch {

    private FloatMismatch() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a {@link ValueMismatch} instance for a float attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity  @return info on the mismatch
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(float expected, float actual, float newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpectedPreviousValue(pack(expected))
                                                           .setActualPreviousValue(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    //TODO:2016-12-22:alexander.yevsyukov: Add methods for unpacking expected, actual, and newValue.
}
