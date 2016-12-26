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

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.change.Mismatches.checkNotNullOrEqual;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with {@code ValueMismatch}es for {@code Message} values.
 *
 * @author Alexander Yevsyukov
 */
public class MessageMismatch {

    private MessageMismatch() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates {@code ValueMismatch} for the case of discovering a non-default value,
     * when the default value was expected by a command.
     *
     * @param actual the value discovered instead of the default value
     * @param newValue the new value requested in the command
     * @param version the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedDefault(Message actual, Message newValue, int version) {
        checkNotNull(actual);
        checkNotNull(newValue);
        final Message expectedDefault = actual.getDefaultInstanceForType();
        return of(expectedDefault, actual, newValue, version);
    }

    /**
     * Creates a mismatch for a command that wanted to clear a value, but discovered 
     * that the field already has the default value.
     * 
     * @param expected the value of the field that the command wanted to clear
     * @param version the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedNotDefault(Message expected, int version) {
        checkNotNull(expected);
        final Message defaultValue = expected.getDefaultInstanceForType();
        return of(expected, defaultValue, defaultValue, version);
    }

    /**
     * Creates {@code ValueMismatch} for the case of discovering a value different than by a command.
     *
     * @param expected the value expected by the command
     * @param actual the value discovered instead of the expected value
     * @param newValue the new value requested in the command
     * @param version the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch unexpectedValue(Message expected, Message actual, Message newValue, int version) {
        checkNotNullOrEqual(expected, actual);
        checkNotNull(newValue);

        return of(expected, actual, newValue, version);
    }

    private static ValueMismatch of(Message expected, Message actual, Message newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpected(pack(expected))
                                                           .setActual(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Obtains expected value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-{@code Message} values
     */
    public static Message unpackExpected(ValueMismatch mismatch) {
        final Any any = mismatch.getExpected();
        final Message result = unpack(any);
        return result;
    }

    /**
     * Obtains actual value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-{@code Message} values
     */
    public static Message unpackActual(ValueMismatch mismatch) {
        final Any any = mismatch.getActual();
        final Message result = unpack(any);
        return result;
    }

    /**
     * Obtains new value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-{@code Message} values
     */
    public static Message unpackNewValue(ValueMismatch mismatch) {
        final Any any = mismatch.getNewValue();
        final Message result = unpack(any);
        return result;
    }
}
