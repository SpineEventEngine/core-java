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
import org.spine3.protobuf.AnyPacker;

import javax.annotation.Nullable;

/**
 * Utility class for working with {@link ValueMismatch}es for {@code Message} values.
 *
 * @author Alexander Yevsyukov
 */
public class MessageMismatch {

    private MessageMismatch() {
        // Prevent instantiation of this utility class.
    }

    //TODO:2016-12-22:alexander.yevsyukov: Rename properties of the ValueMismatch proto removing 'previous_value' suffix.

    /**
     * Creates a {@link ValueMismatch} instance for a Message attribute.
     *
     * @param expected the value expected by a command, or {@code null} if the command expects not populated field
     * @param actual   the value actual in an entity, or {@code null} if the value is not set
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(@Nullable Message expected,
                                   @Nullable Message actual,
                                   @Nullable Message newValue,
                                   int version) {
        //TODO:2016-12-22:alexander.yevsyukov: check that expected and actual are not equal. Document this fact

        final ValueMismatch.Builder builder = ValueMismatch.newBuilder();

        //TODO:2016-12-22:alexander.yevsyukov: Handle packing default values when nulls passed or disallow nulls at all.
        if (expected != null) {
            builder.setExpectedPreviousValue(AnyPacker.pack(expected));
        }
        if (actual != null) {
            builder.setActualPreviousValue(AnyPacker.pack(actual));
        }
        if (newValue != null) {
            builder.setNewValue(AnyPacker.pack(newValue));
        }
        builder.setVersion(version);
        return builder.build();
    }

    //TODO:2016-12-22:alexander.yevsyukov: Add tests for the methods below.

    /**
     * Obtains expected value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-Message values
     */
    public static Message unpackExpected(ValueMismatch mismatch) {
        final Any any = mismatch.getExpectedPreviousValue();
        final Message result = AnyPacker.unpack(any);
        return result;
    }

    /**
     * Obtains actual value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-Message values
     */
    public static Message unpackActual(ValueMismatch mismatch) {
        final Any any = mismatch.getActualPreviousValue();
        final Message result = AnyPacker.unpack(any);
        return result;
    }

    /**
     * Obtains new value as a {@code Message} from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-Message values
     */
    public static Message unpackNewValue(ValueMismatch mismatch) {
        final Any any = mismatch.getActualPreviousValue();
        final Message result = AnyPacker.unpack(any);
        return result;
    }
}
