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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.change.Mismatches.checkNotNullOrEqual;
import static org.spine3.protobuf.Values.pack;
import static org.spine3.util.Exceptions.wrapped;

/**
 * Utility class for working with string values in ValueMismatches.
 *
 * @author Alexander Yevsyukov
 */
public class StringMismatch {

    private StringMismatch() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates ValueMismatch for the case of discovering a non-empty value,
     * when an empty string was expected by a command.
     *
     * @param actual the value discovered instead of the empty string
     * @param newValue the new value requested in the command
     * @param version the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedEmpty(String actual, String newValue, int version) {
        checkNotNull(actual);
        checkNotNull(newValue);
        return of("", actual, newValue, version);
    }

    /**
     * Creates a mismatch for a command that wanted to clear a string value, but discovered
     * that the field is already empty.
     *
     * @param expected the value of the field that the command wanted to clear
     * @param version the version of the entity in which the mismatch is discovered
     * @return new ValueMismatch instance
     */
    public static ValueMismatch expectedNotEmpty(String expected, int version) {
        checkNotNull(expected);
        return of(expected, "", "", version);
    }

    /**
     * Creates ValueMismatch for the case of discovering a value different than by a command.
     *
     * @param expected the value expected by the command
     * @param actual the value discovered instead of the expected string
     * @param newValue the new value requested in the command
     * @param version the version of the entity in which the mismatch is discovered
     * @return new ValueMismatch instance
     */
    public static ValueMismatch unexpectedValue(String expected, String actual, String newValue, int version) {
        checkNotNullOrEqual(expected, actual);
        checkNotNull(newValue);

        return of(expected, actual, newValue, version);
    }

    /**
     * Creates a new instance of {@code ValueMismatch} with the passed values.
     */
    private static ValueMismatch of(String expected, String actual, String newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch
                .newBuilder()
                .setExpected(pack(expected))
                .setActual(pack(actual))
                .setNewValue(pack(newValue))
                .setVersion(version);
        return builder.build();
    }

    /**
     * Obtains expected string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String unpackExpected(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getExpected()
                                               .unpack(StringValue.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }

    /**
     * Obtains actual string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String unpackActual(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getActual()
                                               .unpack(StringValue.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }

    /**
     * Obtains new value string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String unpackNewValue(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getNewValue()
                                               .unpack(StringValue.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }
}
