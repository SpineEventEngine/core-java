/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testdata;

import com.google.common.base.Charsets;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeUrl;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.Messages.builderFor;
import static java.lang.String.format;

/**
 * Utility for creating simple stubs for generated messages, DTOs (like {@link Event} and
 * {@link Command}), storage objects and else.
 */
public class Sample {

    /** Prevents instantiation of this utility class. */
    private Sample() {
    }

    /**
     * Generates a new stub {@link Message.Builder} with all the fields set to
     * {@link Random random} values.
     *
     * <p> All the fields are guaranteed to be not {@code null} and not default.
     * Number and {@code boolean} fields may or may not have their default values ({@code 0} and
     * {@code false}).
     *
     * @apiNote This method casts the builder to the generic parameter {@code <B>} for brevity of
     *          test code. It is the caller responsibility to ensure that the message
     *          type {@code <M>} corresponds to the builder type {@code <B>}.
     *
     * @param clazz Java class of the stub message
     * @param <M>   type of the required message
     * @param <B>   type of the {@link Message.Builder} for the message
     * @return new instance of the {@link Message.Builder} for given type
     * @see #valueFor(FieldDescriptor)
     */
    @SuppressWarnings("TypeParameterUnusedInFormals") // See apiNote.
    public static <M extends Message, B extends Message.Builder> B builderForType(Class<M> clazz) {
        checkClass(clazz);

        B builder = builderFor(clazz);
        Descriptor builderDescriptor = builder.getDescriptorForType();
        Collection<FieldDescriptor> fields = builderDescriptor.getFields();

        for (FieldDescriptor field : fields) {
            Object value = valueFor(field);
            if (field.isRepeated()) {
                builder.addRepeatedField(field, value);
            } else {
                builder.setField(field, value);
            }
        }
        return builder;
    }

    /**
     * Generates a new stub {@link Message} with all the fields set to {@link Random random} values.
     *
     * <p> All the fields are guaranteed to be not {@code null} and not default.
     * Number and {@code boolean} fields
     * may or may not have their default values ({@code 0} and {@code false}).
     *
     * <p>If the required type is {@link Any}, an instance of an empty {@link Any} wrapped into
     * another {@link Any} is returned. See {@link AnyPacker}.
     *
     * @param clazz Java class of the required stub message
     * @param <M>   type of the required message
     * @return new instance of the given {@link Message} type with random fields
     * @see #builderForType(Class)
     */
    public static <M extends Message> M messageOfType(Class<M> clazz) {
        checkClass(clazz);

        if (Any.class.equals(clazz)) {
            Any any = Any.getDefaultInstance();
            @SuppressWarnings("unchecked") //
            M result = (M) AnyPacker.pack(any);
            return result;
        }

        Message.Builder builder = builderForType(clazz);
        @SuppressWarnings("unchecked") // Checked cast
        M result = (M) builder.build();

        return result;
    }

    private static void checkClass(Class<? extends Message> clazz) {
        checkNotNull(clazz);
        // Support only generated protobuf messages
        checkArgument(GeneratedMessageV3.class.isAssignableFrom(clazz),
                      "Only generated protobuf messages are allowed.");
    }

    /**
     * Generates a non-default value for the given message field.
     *
     * <p>All the protobuf types are supported including nested {@link Message}s and
     * the {@code enum}s.
     *
     * @param field {@link FieldDescriptor} to take the type info from
     * @return a non-default generated value of type of the given field
     */
    @SuppressWarnings("OverlyComplexMethod")
    private static Object valueFor(FieldDescriptor field) {
        Type type = field.getType();
        JavaType javaType = type.getJavaType();
        Random random = new SecureRandom();
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
                byte[] bytes = new byte[8];
                random.nextBytes(bytes);
                return new String(bytes, Charsets.UTF_8);
            case BYTE_STRING:
                byte[] bytesPrimitive = new byte[8];
                random.nextBytes(bytesPrimitive);
                return ByteString.copyFrom(bytesPrimitive);
            case ENUM:
                return enumValueFor(field, random);
            case MESSAGE:
                return messageValueFor(field);
            default:
                throw new IllegalArgumentException(format("Field type %s is not supported.", type));
        }
    }

    private static Object enumValueFor(FieldDescriptor field, Random random) {
        Descriptors.EnumDescriptor descriptor = field.getEnumType();
        List<Descriptors.EnumValueDescriptor> enumValues = descriptor.getValues();
        if (enumValues.isEmpty()) {
            return null;
        }

        // Value under index 0 is usually used to store `undefined` option
        // Use values with indexes from 1 to n
        int index = random.nextInt(enumValues.size() - 1) + 1;
        Descriptors.EnumValueDescriptor enumValue = descriptor.findValueByNumber(index);
        return enumValue;
    }

    private static Message messageValueFor(FieldDescriptor field) {
        TypeUrl messageType = TypeUrl.from(field.getMessageType());
        Class<? extends Message> javaClass = messageType.getMessageClass();
        Message fieldValue = messageOfType(javaClass);
        return fieldValue;
    }
}
