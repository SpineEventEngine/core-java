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

package org.spine3.base;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.spine3.protobuf.AnyPacker;

import javax.annotation.Nullable;

import static com.google.common.base.Throwables.propagate;
import static org.spine3.protobuf.Values.pack;

/**
 * Factories for constructing {@link ValueMismatch} instances for different types of attributes.
 *
 * @author Alexander Yevsyukov
 * @author Andrey Lavrov
 */
public class Mismatch {

    private Mismatch() {
    }

    /**
     * Creates a {@link ValueMismatch} instance for a string attribute.
     *
     * @param expected the value expected by a command, or {@code null} if the command expects not populated field
     * @param actual   the value found in an entity, or {@code null} if the value is not set
     * @param version  the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(@Nullable String expected, @Nullable String actual, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder();
        if (expected != null) {
            builder.setExpected(pack(expected));
        }
        if (actual != null) {
            builder.setActual(pack(actual));
        }
        builder.setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a integer attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param version  the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(int expected, int actual, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpected(pack(expected))
                                                           .setActual(pack(actual))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a long integer attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param version  the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(long expected, long actual, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpected(pack(expected))
                                                           .setActual(pack(actual))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a float attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param version  the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(float expected, float actual, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpected(pack(expected))
                                                           .setActual(pack(actual))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a double attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param version  the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(double expected, double actual, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpected(pack(expected))
                                                           .setActual(pack(actual))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a boolean attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param version  the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(boolean expected, boolean actual, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpected(pack(expected))
                                                           .setActual(pack(actual))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a Message attribute.
     *
     * @param expected the value expected by a command, or {@code null} if the command expects not populated field
     * @param actual   the value actual in an entity, or {@code null} if the value is not set
     * @param version  the current version of the entity
     * @return info on the mismatch
     */
    public static ValueMismatch of(@Nullable Message expected, @Nullable Message actual, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder();
        if (expected != null) {
            builder.setExpected(AnyPacker.pack(expected));
        }
        if (actual != null) {
            builder.setActual(AnyPacker.pack(actual));
        }
        builder.setVersion(version);
        return builder.build();
    }


    /**
     * Obtains expected string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String getExpectedString(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getExpected()
                                               .unpack(StringValue.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw propagate(e);
        }
    }

    /**
     * Obtains actual string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String getActualString(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getActual()
                                               .unpack(StringValue.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw propagate(e);
        }
    }
}
