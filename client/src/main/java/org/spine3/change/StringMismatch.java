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

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;

import javax.annotation.Nullable;

import static org.spine3.protobuf.Values.pack;
import static org.spine3.util.Exceptions.wrapped;

/**
 * Utility class for working with string values in {@link ValueMismatch}es.
 *
 * @author Alexander Yevsyukov
 */
public class StringMismatch {

    private StringMismatch() {
        // Prevent instantiation of this utility class.
    }

    //TODO:2016-12-22:alexander.yevsyukov: Document.
    //TODO:2016-12-22:alexander.yevsyukov: Test.
    public static ValueMismatch expectedEmpty(String actual, String newValue, int version) {
        return of("", actual, newValue, version);
    }

    //TODO:2016-12-22:alexander.yevsyukov: Document.
    //TODO:2016-12-22:alexander.yevsyukov: Test.
    public static ValueMismatch unexpectedValue(String expected, String actual, String newValue, int version) {
        return of(expected, actual, newValue, version);
    }

    //TODO:2016-12-22:alexander.yevsyukov: Test.
    /**
     * Creates a mismatch for a command that wanted to clear a string value, but discovered
     * that the field is already empty.
     *
     * @param expected the value of the field that the command wanted to clear
     * @param version the current version of the entity
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch alreadyEmpty(String expected, int version) {
        return of(expected, "", "", version);
    }

    /**
     * Creates a {@link ValueMismatch} instance for a string attribute.
     *
     * @param expected the value expected by a command, or {@code null} if the command expects not populated field
     * @param actual   the value found in an entity, or {@code null} if the value is not set
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(@Nullable String expected,
                                   @Nullable String actual,
                                   @Nullable String newValue,
                                   int version) {
        final ValueMismatch.Builder builder = ValueMismatch
                .newBuilder()
                .setExpectedPreviousValue(packStr(expected))
                .setActualPreviousValue(packStr(actual))
                .setNewValue(packStr(newValue))
                .setVersion(version);
        return builder.build();
    }

    private static Any packStr(@Nullable String expected) {
        return pack(Strings.nullToEmpty(expected));
    }

    /**
     * Obtains expected string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String unpackExpected(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getExpectedPreviousValue()
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
            final StringValue result = mismatch.getActualPreviousValue()
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
