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

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toAny;

/**
 * Utility class for working with {@code boolean} values in {@link ValueMismatch}es.
 *
 * @author Alexander Yevsyukov
 */
public final class BooleanMismatch {

    private BooleanMismatch() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates {@code ValueMismatch} for the case when command finds false value instead of true.
     *
     * @param version the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedTrue(int version) {
        return of(true, false, false, version);
    }

    /**
     * Creates {@code ValueMismatch} for the case when command finds true value instead of false.
     *
     * @param version the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedFalse(int version) {
        return of(false, true, true, version);
    }

    /**
     * Creates a {@code ValueMismatch} instance for a boolean attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity
     * @return new {@code ValueMismatch} instance
     */
    private static ValueMismatch of(boolean expected,
                                    boolean actual,
                                    boolean newValue,
                                    int version) {
        ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                     .setExpected(toAny(expected))
                                                     .setActual(toAny(actual))
                                                     .setNewValue(toAny(newValue))
                                                     .setVersion(version);
        return builder.build();
    }

    private static boolean unpacked(Any any) {
        BoolValue unpacked = unpack(any, BoolValue.class);
        return unpacked.getValue();
    }

    /**
     * Obtains expected boolean value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-boolean values
     */
    public static boolean unpackExpected(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any expected = mismatch.getExpected();
        return unpacked(expected);
    }

    /**
     * Obtains actual boolean value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-boolean values
     */
    public static boolean unpackActual(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any actual = mismatch.getActual();
        return unpacked(actual);
    }

    /**
     * Obtains new boolean value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-boolean values
     */
    public static boolean unpackNewValue(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any newValue = mismatch.getNewValue();
        return unpacked(newValue);
    }
}
