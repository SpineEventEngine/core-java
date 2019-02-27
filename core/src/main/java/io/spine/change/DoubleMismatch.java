/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.protobuf.DoubleValue;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.change.ChangePreconditions.checkNotNullOrEqual;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toAny;

/**
 * Utility class for working with {@code double} values in {@link ValueMismatch}es.
 */
public final class DoubleMismatch {

    /** Prevent instantiation of this utility class. */
    private DoubleMismatch() {
    }

    /**
     * Creates {@code ValueMismatch} for the case of discovering not zero value,
     * when a zero amount was expected by a command.
     *
     * @param actual   the value discovered instead of zero
     * @param newValue the new value requested in the command
     * @param version  the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedZero(double actual, double newValue, int version) {
        return of(0.0, actual, newValue, version);
    }

    /**
     * Creates {@code ValueMismatch} for the case of discovering zero value,
     * when a non zero amount was expected by a command.
     *
     * @param expected the value of the field that the command wanted to clear
     * @param newValue the new value requested in the command
     * @param version  the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedNonZero(double expected, double newValue, int version) {
        return of(expected, 0.0, newValue, version);
    }

    /**
     * Creates {@code ValueMismatch} for the case of discovering a value
     * different than by a command.
     *
     * @param expected the value expected by the command
     * @param actual   the value discovered instead of the expected string
     * @param newValue the new value requested in the command
     * @param version  the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch unexpectedValue(double expected, double actual,
                                                double newValue, int version) {
        checkNotNullOrEqual(expected, actual);
        return of(expected, actual, newValue, version);
    }

    /**
     * Creates a new instance of {@code ValueMismatch} with the passed values
     * for a double attribute.
     */
    public static ValueMismatch of(double expected, double actual, double newValue, int version) {
        ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                     .setExpected(toAny(expected))
                                                     .setActual(toAny(actual))
                                                     .setNewValue(toAny(newValue))
                                                     .setVersion(version);
        return builder.build();
    }

    private static double unpacked(Any any) {
        DoubleValue unpacked = unpack(any, DoubleValue.class);
        return unpacked.getValue();
    }

    /**
     * Obtains expected double value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-double values
     */
    public static double unpackExpected(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any expected = mismatch.getExpected();
        return unpacked(expected);
    }

    /**
     * Obtains actual double value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-double values
     */
    public static double unpackActual(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any actual = mismatch.getActual();
        return unpacked(actual);
    }

    /**
     * Obtains new double value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-double values
     */
    public static double unpackNewValue(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any newValue = mismatch.getNewValue();
        return unpacked(newValue);
    }
}
