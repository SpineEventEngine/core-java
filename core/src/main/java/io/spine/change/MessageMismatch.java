/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.change.ChangePreconditions.checkNotEqual;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with {@code ValueMismatch}es for {@code Message} values.
 */
public final class MessageMismatch {

    /** Prevent instantiation of this utility class. */
    private MessageMismatch() {
    }

    /**
     * Creates {@code ValueMismatch} for the case of discovering a non-default value,
     * when the default value was expected by a command.
     *
     * @param actual   the value discovered instead of the default value
     * @param newValue the new value requested in the command
     * @param version  the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedDefault(Message actual, Message newValue, int version) {
        checkNotNull(actual);
        checkNotNull(newValue);
        Message expectedDefault = actual.getDefaultInstanceForType();
        return of(expectedDefault, actual, newValue, version);
    }

    /**
     * Creates a {@code ValueMismatch} for a command that wanted to <em>clear</em> a value,
     * but discovered that the field already has the default value.
     *
     * @param expected the value of the field that the command wanted to clear
     * @param version  the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedNotDefault(Message expected, int version) {
        checkNotNull(expected);
        Message defaultValue = expected.getDefaultInstanceForType();
        return of(expected, defaultValue, defaultValue, version);
    }

    /**
     * Creates a {@code ValueMismatch} for a command that wanted to <em>change</em> a field value,
     * but discovered that the field has the default value.
     *
     * @param expected the value expected by the command
     * @param newValue the value the command wanted to set
     * @param version  the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch expectedNotDefault(Message expected,
                                                   Message newValue,
                                                   int version) {
        checkNotNull(expected);
        checkNotNull(newValue);
        Message defaultValue = expected.getDefaultInstanceForType();
        return of(expected, defaultValue, newValue, version);
    }

    /**
     * Creates {@code ValueMismatch} for the case of discovering a value
     * different than by a command.
     *
     * @param expected the value expected by the command
     * @param actual   the value discovered instead of the expected value
     * @param newValue the new value requested in the command
     * @param version  the version of the entity in which the mismatch is discovered
     * @return new {@code ValueMismatch} instance
     */
    public static ValueMismatch unexpectedValue(Message expected, Message actual,
                                                Message newValue, int version) {
        checkNotNull(expected);
        checkNotNull(actual);
        checkNotNull(newValue);
        checkNotEqual(expected, actual);

        return of(expected, actual, newValue, version);
    }

    /**
     * Creates a new instance of {@code ValueMismatch} with the passed values.
     */
    private static ValueMismatch of(Message expected, Message actual,
                                    Message newValue, int version) {
        ValueMismatch result = ValueMismatch
                .newBuilder()
                .setExpected(pack(expected))
                .setActual(pack(actual))
                .setNewValue(pack(newValue))
                .setVersion(version)
                .vBuild();
        return result;
    }

    /**
     * Obtains expected value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of
     *                          non-{@code Message} values
     */
    public static Message unpackExpected(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any any = mismatch.getExpected();
        Message result = unpack(any);
        return result;
    }

    /**
     * Obtains actual value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of
     *                          non-{@code Message} values
     */
    public static Message unpackActual(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any any = mismatch.getActual();
        Message result = unpack(any);
        return result;
    }

    /**
     * Obtains new value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of
     *                          non-{@code Message} values
     */
    public static Message unpackNewValue(ValueMismatch mismatch) {
        checkNotNull(mismatch);
        Any any = mismatch.getNewValue();
        Message result = unpack(any);
        return result;
    }
}
