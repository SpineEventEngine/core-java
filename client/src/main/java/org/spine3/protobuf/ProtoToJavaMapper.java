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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
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
import org.spine3.annotations.Internal;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility for converting the {@linkplain Message Protobuf Messages} (in form of {@link Any}) into
 * arbitrary {@linkplain Object Java Objects}.
 *
 * <p>Currently, the supported types are:
 * <ul>
 *     <li>{@link Message} - converted via {@link AnyPacker};
 *     <li>Java primitives - the passed {@link Any} is unpacked into one of types {@code Int32Value,
 *     Int64Value, UInt32Value, UInt64Value, FloatValue, DoubleValue, BoolValue, StringValue,
 *     BytesValue} and then into the corresponding Java type, either a primitive value, or
 *     {@code String} of {@link ByteString}.
 * </ul>
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class ProtoToJavaMapper {

    private ProtoToJavaMapper() {
        // Prevent utility class initialization
    }

    /**
     * Performs the {@link Any} to {@link Object} mapping.
     *
     * @param message the {@link Any} value to convert
     * @param target  the conversion target class
     * @param <T>     the conversion target type
     * @return te converted value
     */
    public static <T> T map(Any message, Class<T> target) {
        final AnyCaster<T> caster = AnyCaster.forType(target);
        final T result = caster.apply(message);
        return result;
    }

    /**
     * Selects and retrieves a {@link Function} for performing the described conversion.
     *
     * @param target the conversion target class
     * @param <T> the conversion target type
     * @return the conversion target type
     */
    public static <T> Function<Any, T> function(Class<T> target) {
        final AnyCaster<T> caster = AnyCaster.forType(target);
        return caster;
    }

    /**
     * The {@link Function} performing the described type convertion.
     */
    private abstract static class AnyCaster<T> implements Function<Any, T> {

        private static <T> AnyCaster<T> forType(Class<T> cls) {
            checkNotNull(cls);
            if (Message.class.isAssignableFrom(cls)) {
                return new MessageTypeCaster<>();
            } else {
                return new PrimitiveTypeCaster<>();
            }
        }

        @Override
        public T apply(@Nullable Any input) {
            checkNotNull(input);
            return cast(input);
        }

        protected abstract T cast(Any input);
    }

    private static class MessageTypeCaster<T> extends AnyCaster<T> {

        @Override
        protected T cast(Any input) {
            final Message unpacked = AnyPacker.unpack(input);
            @SuppressWarnings("unchecked") final T result = (T) unpacked;
            return result;
        }
    }

    private static class PrimitiveTypeCaster<T> extends AnyCaster<T> {

        private static final ImmutableMap<Class, Function<? extends Message, ?>>
                PROTO_WRAPPER_TO_PRIMITIVE =
                ImmutableMap.<Class, Function<? extends Message, ?>>builder()
                            .put(Int32Value.class, new Int32Unboxer())
                            .put(Int64Value.class, new Int64Unboxer())
                            .put(UInt32Value.class, new UInt32Unboxer())
                            .put(UInt64Value.class, new UInt64Unboxer())
                            .put(FloatValue.class, new FloatUnboxer())
                            .put(DoubleValue.class, new DoubleUnboxer())
                            .put(BoolValue.class, new BoolUnboxer())
                            .put(StringValue.class, new StringUnboxer())
                            .put(BytesValue.class, new BytesUnboxer())
                            .build();

        @Override
        protected T cast(Any input) {
            final Message unpacked = AnyPacker.unpack(input);
            final Class boxedType = unpacked.getClass();
            @SuppressWarnings("unchecked") final Function<Message, T> typeUnpacker =
                    (Function<Message, T>) PROTO_WRAPPER_TO_PRIMITIVE.get(boxedType);
            checkArgument(typeUnpacker != null,
                          "Could not find a primitive type for %s.",
                          boxedType.getCanonicalName());
            final T result = typeUnpacker.apply(unpacked);
            return result;
        }
    }

    abstract static class PrimitiveUnboxer<M extends Message, T> implements Function<M, T> {

        @Override
        public T apply(@Nullable M input) {
            checkNotNull(input);
            return unpack(input);
        }

        protected abstract T unpack(M message);
    }

    private static class Int32Unboxer extends PrimitiveUnboxer<Int32Value, Integer> {

        @Override
        protected Integer unpack(Int32Value message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class Int64Unboxer extends PrimitiveUnboxer<Int64Value, Long> {

        @Override
        protected Long unpack(Int64Value message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class UInt32Unboxer extends PrimitiveUnboxer<UInt32Value, Integer> {

        @Override
        protected Integer unpack(UInt32Value message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class UInt64Unboxer extends PrimitiveUnboxer<UInt64Value, Long> {

        @Override
        protected Long unpack(UInt64Value message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class FloatUnboxer extends PrimitiveUnboxer<FloatValue, Float> {

        @Override
        protected Float unpack(FloatValue message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class DoubleUnboxer extends PrimitiveUnboxer<DoubleValue, Double> {

        @Override
        protected Double unpack(DoubleValue message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class BoolUnboxer extends PrimitiveUnboxer<BoolValue, Boolean> {

        @Override
        protected Boolean unpack(BoolValue message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class StringUnboxer extends PrimitiveUnboxer<StringValue, String> {

        @Override
        protected String unpack(StringValue message) {
            checkNotNull(message);
            return message.getValue();
        }
    }

    private static class BytesUnboxer extends PrimitiveUnboxer<BytesValue, ByteString> {

        @Override
        protected ByteString unpack(BytesValue message) {
            checkNotNull(message);
            return message.getValue();
        }
    }
}
