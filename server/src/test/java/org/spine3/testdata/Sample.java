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

package org.spine3.testdata;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.server.event.storage.EventStorageRecord;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Utility for creating simple stubs for generated messages.
 *
 * @author Dmytro Dashenkov
 */
public class Sample {

    public static class EventRecord {

        public static EventStorageRecord withAllFields() {
            return Sample.messageOfType(EventStorageRecord.class);
        }

        public static EventStorageRecord with(Timestamp timestamp) {
            final EventStorageRecord.Builder builder = Sample.builderForType(EventStorageRecord.class);
            builder.setTimestamp(timestamp);
            return builder.build();
        }

        public static EventStorageRecord with(String eventId) {
            final EventStorageRecord.Builder builder = Sample.builderForType(EventStorageRecord.class);
            builder.setEventId(eventId);
            return builder.build();
        }

        public static EventStorageRecord with(String eventId, Timestamp timestamp) {
            final EventStorageRecord.Builder builder = Sample.builderForType(EventStorageRecord.class);
            builder.setEventId(eventId)
                   .setTimestamp(timestamp);
            return builder.build();
        }
    }

    public static <M extends Message, B extends Message.Builder> B builderForType(Class<M> clazz) {
        checkClass(clazz);

        final B builder = builderFor(clazz);
        final Descriptor builderDescriptor = builder.getDescriptorForType();
        final Collection<FieldDescriptor> fields = builderDescriptor.getFields();

        for (FieldDescriptor field : fields) {
            final Object value = valueFor(field);
            if (value == null) {
                continue;
            }
            builder.setField(field, value);
        }
        return builder;
    }

    public static <M extends Message> M messageOfType(Class<M> clazz) {
        checkClass(clazz);

        final M.Builder builder = builderForType(clazz);
        @SuppressWarnings("unchecked") // Checked cast
        final M result = (M) builder.build();

        return result;
    }

    private static void checkClass(Class<? extends Message> clazz) {
        checkNotNull(clazz);
        // Support only generated protobuf messages
        checkArgument(clazz.isAssignableFrom(GeneratedMessageV3.class));
        checkArgument(!clazz.equals(Any.class), format(
                "%s type is not supported. Please, generate a generic message and use AnyPacker instead.",
                Any.class.getCanonicalName()
        ));
    }

    private static Object valueFor(FieldDescriptor field) {
        final Type type = field.getType();
        final JavaType javaType = type.getJavaType();
        final Random random = new SecureRandom();
        switch (javaType) {
            case INT:
                return random.nextInt();
            case LONG:
                return random.nextLong();
            case FLOAT:
                return random.nextFloat();
            case DOUBLE:
                return random.nextDouble();
            case BOOLEAN:
                return random.nextBoolean();
            case STRING:
                final byte[] bytes = new byte[8];
                random.nextBytes(bytes);
                return new String(bytes);
            case BYTE_STRING:
                final byte[] bytesPrimitive = new byte[8];
                random.nextBytes(bytesPrimitive);
                return ByteString.copyFrom(bytesPrimitive);
            case ENUM:
                return null;
            case MESSAGE:
                return null;
            default:
                throw new IllegalArgumentException(format("Field type %s is not supported", javaType));
        }
    }

    private static <B extends Message.Builder> B builderFor(Class<? extends Message> clazz) {
        try {
            final Method factoryMethod = clazz.getDeclaredMethod("newBuilder");
            @SuppressWarnings("unchecked")
            final B result = (B) factoryMethod.invoke(null);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(
                    format("Class %s must be a generated proto message",
                           clazz.getCanonicalName()),
                    e);
        }

    }
}
