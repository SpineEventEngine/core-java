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

package org.spine3.server.entity.storage;

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
import org.spine3.protobuf.AnyPacker;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov
 */
// TODO:2017-04-21:dmytro.dashenkov: Maybe we should move this to client code ti make this util reusable.
final class ProtoToJavaMapper {

    private ProtoToJavaMapper() {
        // Prevent utility class initialization
    }

    public static <T> T map(Any message, Class<T> target) {
        final AnyCaster<T> caster = AnyCaster.forType(target);
        final T result = caster.apply(message);
        return result;
    }

    public static <T> Function<Any, T> function(Class<T> target) {
        final AnyCaster<T> caster = AnyCaster.forType(target);
        return caster;
    }

    abstract static class AnyCaster<T> implements Function<com.google.protobuf.Any, T> {

        private static <T> AnyCaster<T> forType(Class<T> cls) {
            checkNotNull(cls);
            if (Message.class.isAssignableFrom(cls)) {
                return new MessageTypeCaster<>();
            } else {
                return new PrimitiveTypeCaster<>();
            }
        }

        @Override
        public T apply(@Nullable com.google.protobuf.Any input) {
            checkNotNull(input);
            return cast(input);
        }

        protected abstract T cast(com.google.protobuf.Any input);
    }

    private static class MessageTypeCaster<T> extends AnyCaster<T> {

        @Override
        protected T cast(com.google.protobuf.Any input) {
            final Message unpacked = AnyPacker.unpack(input);
            @SuppressWarnings("unchecked")
            final T result = (T) unpacked;
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
        protected T cast(com.google.protobuf.Any input) {
            final Message unpacked = AnyPacker.unpack(input);
            final Class boxedType = unpacked.getClass();
            @SuppressWarnings("unchecked")
            final Function<Message, T> typeUnpacker =
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
