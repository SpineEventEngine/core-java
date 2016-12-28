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

import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;

import static org.spine3.protobuf.Values.pack;
import static org.spine3.util.Exceptions.wrapped;

/**
 * Utility class for working with {@code int} values in {@link ValueMismatch}es.
 *
 * @author Alexander Yevsyukov
 */
public class IntMismatch {


    private IntMismatch() {
        // Prevent instantiation.
    }

    /**
     * Creates ValueMismatch for the case of discovering not zero value,
     * when a zero amount was expected by a command.
     *
     * @param actual the value discovered instead of zero
     * @param newValue the new value requested in the command
     * @param version the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedZero(int actual, int newValue, int version) {
        return of(0, actual, newValue, version);
    }

    /**
     * Creates a ValueMismatch instance for a integer attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity
     * @return new ValueMismatch instance
     */
    public static ValueMismatch of(int expected, int actual, int newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpected(pack(expected))
                                                           .setActual(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Obtains expected int value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-int values
     */
    public static int unpackExpected(ValueMismatch mismatch) {
        try {
            final Int32Value result = mismatch.getExpected()
                                              .unpack(Int32Value.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }

    /**
     * Obtains actual int value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-int values
     */
    public static int unpackActual(ValueMismatch mismatch) {
        try {
            final Int32Value result = mismatch.getActual()
                                             .unpack(Int32Value.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }

    /**
     * Obtains new int value from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-int values
     */
    public static int unpackNewValue(ValueMismatch mismatch) {
        try {
            final Int32Value result = mismatch.getNewValue()
                                             .unpack(Int32Value.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }
}
