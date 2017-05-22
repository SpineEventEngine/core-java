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
package org.spine3.validate;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.illegalStateWithCauseOf;

/**
 * Utility class for working with {@linkplain ValidatingBuilder validating builders}.
 *
 * @author Alex Tymchenko
 */
public class ValidatingBuilders {

    private ValidatingBuilders() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates an instance of {@code ValidatingBuilder} by its type.
     *
     * @param builderClass the type of the {@code ValidatingBuilder} to instantiate
     * @param <B>          the generic type of returned value
     * @return the new instance of the builder
     */
    @SuppressWarnings("OverlyBroadCatchBlock")   // OK, as the exception handling is the same.
    public static <B extends ValidatingBuilder<?, ?>> B newInstance(Class<B> builderClass) {
        checkNotNull(builderClass);

        try {
            final Method newBuilderMethod =
                    ValidatingBuilder.TypeInfo.getNewBuilderMethod(builderClass);
            final Object raw = newBuilderMethod.invoke(null);

            // By convention, `newBuilder()` always returns instances of `B`.
            @SuppressWarnings("unchecked")
            final B builder = (B) raw;
            return builder;
        } catch (Exception e) {
            throw illegalStateWithCauseOf(e);
        }
    }

    /**
     * Validating builder for {@linkplain StringValue} messages.
     */
    public static final class StringValueValidatingBuilder
            extends AbstractValidatingBuilder<StringValue, StringValue.Builder> {

        // Prevent instantiation from the outside.
        private StringValueValidatingBuilder() {
            super();
        }

        public static StringValueValidatingBuilder newBuilder() {
            return new StringValueValidatingBuilder();
        }

        public StringValueValidatingBuilder setValue(String value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain Timestamp} messages.
     */
    public static final class TimestampValidatingBuilder
            extends AbstractValidatingBuilder<Timestamp, Timestamp.Builder> {

        // Prevent instantiation from the outside.
        private TimestampValidatingBuilder() {
            super();
        }

        public static TimestampValidatingBuilder newBuilder() {
            return new TimestampValidatingBuilder();
        }

        public TimestampValidatingBuilder setSeconds(long value) {
            getMessageBuilder().setSeconds(value);
            return this;
        }

        public TimestampValidatingBuilder setNanos(int value) {
            getMessageBuilder().setNanos(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain Int32Value} messages.
     */
    public static final class Int32ValueValidatingBuilder
            extends AbstractValidatingBuilder<Int32Value, Int32Value.Builder> {

        // Prevent instantiation from the outside.
        private Int32ValueValidatingBuilder() {
            super();
        }

        public static Int32ValueValidatingBuilder newBuilder() {
            return new Int32ValueValidatingBuilder();
        }

        public Int32ValueValidatingBuilder setValue(int value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain Int64Value} messages.
     */
    public static final class Int64ValueValidatingBuilder
            extends AbstractValidatingBuilder<Int64Value, Int64Value.Builder> {

        // Prevent instantiation from the outside.
        private Int64ValueValidatingBuilder() {
            super();
        }

        public static Int64ValueValidatingBuilder newBuilder() {
            return new Int64ValueValidatingBuilder();
        }

        public Int64ValueValidatingBuilder setValue(long value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain UInt32Value} messages.
     */
    public static final class UInt32ValueValidatingBuilder
            extends AbstractValidatingBuilder<UInt32Value, UInt32Value.Builder> {

        // Prevent instantiation from the outside.
        private UInt32ValueValidatingBuilder() {
            super();
        }

        public static UInt32ValueValidatingBuilder newBuilder() {
            return new UInt32ValueValidatingBuilder();
        }

        public UInt32ValueValidatingBuilder setValue(int value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain UInt64Value} messages.
     */
    public static final class UInt64ValueValidatingBuilder
            extends AbstractValidatingBuilder<UInt64Value, UInt64Value.Builder> {

        // Prevent instantiation from the outside.
        private UInt64ValueValidatingBuilder() {
            super();
        }

        public static UInt64ValueValidatingBuilder newBuilder() {
            return new UInt64ValueValidatingBuilder();
        }

        public UInt64ValueValidatingBuilder setValue(long value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain FloatValue} messages.
     */
    public static final class FloatValueValidatingBuilder
            extends AbstractValidatingBuilder<FloatValue, FloatValue.Builder> {

        // Prevent instantiation from the outside.
        private FloatValueValidatingBuilder() {
            super();
        }

        public static FloatValueValidatingBuilder newBuilder() {
            return new FloatValueValidatingBuilder();
        }

        public FloatValueValidatingBuilder setValue(float value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain DoubleValue} messages.
     */
    public static final class DoubleValueValidatingBuilder
            extends AbstractValidatingBuilder<DoubleValue, DoubleValue.Builder> {

        // Prevent instantiation from the outside.
        private DoubleValueValidatingBuilder() {
            super();
        }

        public static DoubleValueValidatingBuilder newBuilder() {
            return new DoubleValueValidatingBuilder();
        }

        public DoubleValueValidatingBuilder setValue(double value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain BoolValue} messages.
     */
    public static final class BoolValueValidatingBuilder
            extends AbstractValidatingBuilder<BoolValue, BoolValue.Builder> {

        // Prevent instantiation from the outside.
        private BoolValueValidatingBuilder() {
            super();
        }

        public static BoolValueValidatingBuilder newBuilder() {
            return new BoolValueValidatingBuilder();
        }

        public BoolValueValidatingBuilder setValue(boolean value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    /**
     * Validating builder for {@linkplain Any} messages.
     */
    public static final class AnyValidatingBuilder
            extends AbstractValidatingBuilder<Any, Any.Builder> {

        // Prevent instantiation from the outside.
        private AnyValidatingBuilder() {
            super();
        }

        public static AnyValidatingBuilder newBuilder() {
            return new AnyValidatingBuilder();
        }
    }

    /**
     * Validating builder for {@linkplain Empty} messages.
     */
    public static final class EmptyValidatingBuilder
            extends AbstractValidatingBuilder<Empty, Empty.Builder> {

        // Prevent instantiation from the outside.
        private EmptyValidatingBuilder() {
            super();
        }

        public static EmptyValidatingBuilder newBuilder() {
            return new EmptyValidatingBuilder();
        }
    }
}
