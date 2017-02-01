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

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;

import static java.lang.String.format;

/**
 * @author Dmytro Dashenkov
 */
public class Sample {

    private static <M extends Message> M filledMessage(Class<M> clazz) {
        final M.Builder builder = builderFor(clazz);
        final Descriptor builderDescriptor = builder.getDescriptorForType();
        final Collection<FieldDescriptor> fields = builderDescriptor.getFields();
        for (FieldDescriptor field : fields) {
            final Object value = noEmptyValueFor(field);
            if (value == null) {
                continue;
            }
            builder.setField(field, value);
        }
        @SuppressWarnings("unchecked") // Type safety is guaranteed
        final M result = (M) builder.build();
        return result;
    }

    private static Object noEmptyValueFor(FieldDescriptor field) {
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
                final byte[] bytesPrimitive = new byte[16];
                random.nextBytes(bytesPrimitive);
                return ByteString.copyFrom(bytesPrimitive);
            case ENUM:
                return null;
            case MESSAGE:
                return null;
            default:
                throw new IllegalArgumentException(format("Type %s is not supported", javaType));
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
                    format("Class %s must be assignable from com.google.protobuf.Message",
                           clazz.getCanonicalName()),
                    e);
        }

    }
}
