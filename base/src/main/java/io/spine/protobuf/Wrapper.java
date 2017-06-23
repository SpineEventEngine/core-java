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

package io.spine.protobuf;

import com.google.common.base.Converter;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper for working with wrapper Protobuf types.
 *
 * <p>See {@code google.protobuf.wrappers.proto} for declarations of the wrapper types.
 *
 * @param <T> the type to wrap
 * @param <W> the wrapping type
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassWithTooManyMethods")
public abstract class Wrapper<T, W extends Message> extends Converter<T, W>
                implements Serializable {

    private static final long serialVersionUID = 0L;

    public Any pack(T value) {
        checkNotNull(value);
        final W wrapper = doForward(value);
        final Any result = AnyPacker.pack(wrapper);
        return result;
    }

    public T unpack(Any any) {
        checkNotNull(any);
        final W wrapper = AnyPacker.unpack(any);
        final T result = doBackward(wrapper);
        return result;
    }

    /**
     * Obtains the wrapper helper for strings.
     */
    public static Wrapper<String, StringValue> forString() {
        return StringWrapper.getInstance();
    }

    /**
     * Creates a new {@code StringValue} wrapping the passed string.
     */
    public static StringValue forString(String str) {
        checkNotNull(str);
        return forString().convert(str);
    }

    /**
     * Obtains the wrapper helper for doubles.
     */
    public static Wrapper<Double, DoubleValue> forDouble() {
        return DoubleWrapper.getInstance();
    }

    /**
     * Creates a new {@code DoubleValue} wrapping the passed number.
     */
    public static DoubleValue forDouble(double value) {
        return forDouble().convert(value);
    }

    /**
     * Obtains the wrapper helper for floats.
     */
    public static Wrapper<Float, FloatValue> forFloat() {
        return FloatWrapper.getInstance();
    }

    /**
     * Creates a new {@code FloatValue} wrapping the passed number.
     */
    public static FloatValue forFloat(float value) {
        return forFloat().convert(value);
    }

    /**
     * Obtains the wrapper helper for integers.
     */
    public static Wrapper<Integer, Int32Value> forInteger() {
        return IntegerWrapper.getInstance();
    }

    /**
     * Creates a new {@code Int32Value} wrapping the passed number.
     */
    public static Int32Value forInteger(int value) {
        return forInteger().convert(value);
    }

    /**
     * Obtains the wrapper helper for unsigned integers.
     */
    public static Wrapper<Integer, UInt32Value> forUnsignedInteger() {
        return UnsignedIntegerWrapper.getInstance();
    }

    /**
     * Creates a new {@code UInt32Value} wrapping the passed number.
     */
    public static UInt32Value forUnsignedInteger(int value) {
        return forUnsignedInteger().convert(value);
    }

    /**
     * Obtains the wrapper helper for longs.
     */
    public static Wrapper<Long, Int64Value> forLong() {
        return LongWrapper.getInstance();
    }

    /**
     * Creates a new {@code Int64Value} wrapping the passed number.
     */
    public static Int64Value forLong(long value) {
        return forLong().convert(value);
    }

    /**
     * Obtains the wrapper helper for unsigned longs.
     */
    public static Wrapper<Long, UInt64Value> forUnsignedLong() {
        return UnsignedLongWrapper.getInstance();
    }

    /**
     * Creates a new {@code UInt64Value} wrapping the passed number.
     */
    public static UInt64Value forUnsignedLong(long value) {
        return forUnsignedLong().convert(value);
    }

    /**
     * Obtains the wrapper helper for booleans.
     */
    public static Wrapper<Boolean, BoolValue> forBoolean() {
        return BooleanWrapper.getInstance();
    }

    /**
     * Creates a new {@code BoolValue} wrapping the passed value.
     */
    public static BoolValue forBoolean(boolean value) {
        return forBoolean().convert(value);
    }

    /**
     * Obtains the wrapper helper for {@link ByteString}.
     */
    public static Wrapper<ByteString, BytesValue> forBytes() {
        return BytesWrapper.getInstance();
    }

    /**
     * Creates a new wrapping instance of {@link BytesValue} for the passed bytes.
     */
    public static BytesValue forBytes(ByteString value) {
        return forBytes().convert(value);
    }

    /*
     * Wrapper helper classes
     **************************/

    /**
     * The wrapper helper for strings.
     */
    private static class StringWrapper extends Wrapper<String, StringValue> {

        private static final long serialVersionUID = 0L;
        private static final StringWrapper INSTANCE = new StringWrapper();

        static StringWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected StringValue doForward(String s) {
            return StringValue.newBuilder()
                              .setValue(s)
                              .build();
        }

        @Override
        protected String doBackward(StringValue stringValue) {
            return stringValue.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forString()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for doubles.
     */
    private static class DoubleWrapper extends Wrapper<Double, DoubleValue> {

        private static final long serialVersionUID = 0L;
        private static final DoubleWrapper INSTANCE = new DoubleWrapper();

        static DoubleWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected DoubleValue doForward(Double value) {
            return DoubleValue.newBuilder()
                              .setValue(value)
                              .build();
        }

        @Override
        protected Double doBackward(DoubleValue value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forDouble()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for floats.
     */
    private static class FloatWrapper extends Wrapper<Float, FloatValue> {

        private static final long serialVersionUID = 0L;
        private static final FloatWrapper INSTANCE = new FloatWrapper();

        static FloatWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected FloatValue doForward(Float value) {
            return FloatValue.newBuilder().setValue(value).build();
        }

        @Override
        protected Float doBackward(FloatValue value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forFloat()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for integers.
     */
    private static class IntegerWrapper extends Wrapper<Integer, Int32Value> {

        private static final long serialVersionUID = 0L;
        private static final IntegerWrapper INSTANCE = new IntegerWrapper();

        static IntegerWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected Int32Value doForward(Integer value) {
            return Int32Value.newBuilder()
                             .setValue(value)
                             .build();
        }

        @Override
        protected Integer doBackward(Int32Value value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forInteger()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for unsigned integers.
     */
    private static class UnsignedIntegerWrapper extends Wrapper<Integer, UInt32Value> {

        private static final long serialVersionUID = 0L;
        private static final UnsignedIntegerWrapper INSTANCE = new UnsignedIntegerWrapper();

        static UnsignedIntegerWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected UInt32Value doForward(Integer value) {
            return UInt32Value.newBuilder()
                              .setValue(value)
                              .build();
        }

        @Override
        protected Integer doBackward(UInt32Value value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forUnsignedInteger()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for longs.
     */
    private static class LongWrapper extends Wrapper<Long, Int64Value> {

        private static final long serialVersionUID = 0L;
        private static final LongWrapper INSTANCE = new LongWrapper();

        public static LongWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected Int64Value doForward(Long value) {
            return Int64Value.newBuilder()
                             .setValue(value)
                             .build();
        }

        @Override
        protected Long doBackward(Int64Value value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forLong()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for unsigned longs.
     */
    private static class UnsignedLongWrapper extends Wrapper<Long, UInt64Value> {

        private static final long serialVersionUID = 0L;
        private static final UnsignedLongWrapper INSTANCE = new UnsignedLongWrapper();

        static UnsignedLongWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected UInt64Value doForward(Long value) {
            return UInt64Value.newBuilder()
                              .setValue(value)
                              .build();
        }

        @Override
        protected Long doBackward(UInt64Value value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forUnsignedLong()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for booleans.
     */
    private static class BooleanWrapper extends Wrapper<Boolean, BoolValue> {

        private static final long serialVersionUID = 0L;
        private static final BooleanWrapper INSTANCE = new BooleanWrapper();

        static BooleanWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected BoolValue doForward(Boolean value) {
            return BoolValue.newBuilder()
                            .setValue(value)
                            .build();
        }

        @Override
        protected Boolean doBackward(BoolValue value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forBoolean()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The wrapper helper for {@link ByteString}.
     */
    private static class BytesWrapper extends Wrapper<ByteString, BytesValue> {

        private static final long serialVersionUID = 0L;
        private static final BytesWrapper INSTANCE = new BytesWrapper();

        static BytesWrapper getInstance() {
            return INSTANCE;
        }

        @Override
        protected BytesValue doForward(ByteString value) {
            return BytesValue.newBuilder()
                             .setValue(value)
                             .build();
        }

        @Override
        protected ByteString doBackward(BytesValue value) {
            return value.getValue();
        }

        @Override
        public String toString() {
            return "Wrapper.forByteString()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }
}
