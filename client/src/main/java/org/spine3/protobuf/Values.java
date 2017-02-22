/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.protobuf;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@link com.google.protobuf.Message Message} value wrapper objects.
 *
 * @author Alexander Litus
 */
public class Values {

    private Values() {
    }

    /**
     * Creates a new {@code StringValue} wrapping the passed string.
     *
     * @param value the value to wrap
     * @return a new {@code StringValue} instance
     */
    public static StringValue newStringValue(String value) {
        checkNotNull(value);
        final StringValue result = StringValue.newBuilder()
                                              .setValue(value)
                                              .build();
        return result;
    }

    /**
     * Creates a new {@code StringValue} wrapping the passed string.
     *
     * @param format a format string
     * @param args arguments referenced by the format string
     * @return a new {@code StringValue} instance
     * @see String#format(String, Object...)
     */
    public static StringValue newStringValue(String format, Object... args) {
        checkNotNull(format);
        checkNotNull(args);
        final String msg = String.format(format, args);
        return newStringValue(msg);
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(String value) {
        checkNotNull(value);
        final Any result = AnyPacker.pack(newStringValue(value));
        return result;
    }

    /**
     * Creates a new {@code DoubleValue} wrapping the passed number.
     *
     * @param value the value to wrap
     * @return a new DoubleValue instance
     */
    public static DoubleValue newDoubleValue(double value) {
        final DoubleValue result = DoubleValue.newBuilder()
                                              .setValue(value)
                                              .build();
        return result;
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(double value) {
        final Any result = AnyPacker.pack(newDoubleValue(value));
        return result;
    }

    /**
     * Creates a new {@code FloatValue} wrapping the passed number.
     *
     * @param value the value to wrap
     * @return a new FloatValue instance
     */
    public static FloatValue newFloatValue(float value) {
        final FloatValue result = FloatValue.newBuilder()
                                            .setValue(value)
                                            .build();
        return result;
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(float value) {
        final Any result = AnyPacker.pack(newFloatValue(value));
        return result;
    }

    /**
     * Creates a new {@code Int32Value} wrapping the passed number.
     *
     * @param value the value to wrap
     * @return a new Int32Value instance
     */
    public static Int32Value newIntValue(int value) {
        final Int32Value result = Int32Value.newBuilder()
                                            .setValue(value)
                                            .build();
        return result;
    }

    /**
     * Creates a new {@code UInt32Value} wrapping the passed number.
     *
     * @param value the value to wrap
     * @return a new UInt32Value instance
     */
    public static UInt32Value newUIntValue(int value) {
        final UInt32Value result = UInt32Value.newBuilder()
                                              .setValue(value)
                                              .build();
        return result;
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(int value) {
        final Any result = AnyPacker.pack(newIntValue(value));
        return result;
    }

    /**
     * Creates a new {@code Int64Value} wrapping the passed number.
     *
     * @param value the value to wrap
     * @return a new Int64Value instance
     */
    public static Int64Value newLongValue(long value) {
        final Int64Value result = Int64Value.newBuilder()
                                            .setValue(value)
                                            .build();
        return result;
    }

    /**
     * Creates a new {@code UInt64Value} wrapping the passed number.
     *
     * @param value the value to wrap
     * @return a new Int64Value instance
     */
    public static UInt64Value newUInt64Value(long value) {
        final UInt64Value result = UInt64Value.newBuilder()
                                              .setValue(value)
                                              .build();
        return result;
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(long value) {
        final Any result = AnyPacker.pack(newLongValue(value));
        return result;
    }

    /**
     * Creates a new {@code BoolValue} wrapping the passed value.
     *
     * @param value the value to wrap
     * @return a new BoolValue instance
     */
    public static BoolValue newBoolValue(boolean value) {
        final BoolValue result = BoolValue.newBuilder()
                                          .setValue(value)
                                          .build();
        return result;
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(boolean value) {
        final Any result = AnyPacker.pack(newBoolValue(value));
        return result;
    }
}
