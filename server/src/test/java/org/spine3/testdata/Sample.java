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
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.protobuf.AnyPacker;
import org.spine3.type.TypeUrl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Utility for creating simple stubs for generated messages, DTOs (like {@link Event} and {@link Command}),
 * storage objects and else.
 *
 * @author Dmytro Dashenkov
 */
public class Sample {

    private Sample() {
    }

    public static Event eventBy(Message producerId, Class<? extends Message> eventClass) {
        final EventContext eventContext = TestEventContextFactory.createEventContext(producerId);
        final Message eventMessage = messageOfType(eventClass);
        final Event event = Events.createEvent(eventMessage, eventContext);
        return event;
    }

    public static Event eventBy(Message producerId, Message eventMessage) {
        final EventContext eventContext = TestEventContextFactory.createEventContext(producerId);
        final Event event = Events.createEvent(eventMessage, eventContext);
        return event;
    }

    public static Command command(Message commandMessage) {
        final CommandContext commandContext = TestCommandContextFactory.createCommandContext();
        final Command command = Commands.createCommand(commandMessage, commandContext);
        return command;
    }

    /**
     * Generates a new stub {@link Message.Builder builder} with all the fields set to {@link Random random} values.
     *
     * <p> All the fields are guaranteed to be not {@code null} and not default. Number and {@code boolean} fields
     * may or may not have their default values ({@code 0} and {@code false}).
     *
     * @param clazz Java class of the stub message
     * @param <M>   type of the required message
     * @param <B>   type of the {@link Message.Builder builder} for the message
     * @return new instance of the {@link Message.Builder} for given type
     * @see #valueFor(FieldDescriptor)
     */
    public static <M extends Message, B extends Message.Builder> B builderForType(Class<M> clazz) {
        checkClass(clazz);

        final B builder = builderFor(clazz);
        final Descriptor builderDescriptor = builder.getDescriptorForType();
        final Collection<FieldDescriptor> fields = builderDescriptor.getFields();

        for (FieldDescriptor field : fields) {
            final Object value = valueFor(field);
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
     * <p> All the fields are guaranteed to be not {@code null} and not default. Number and {@code boolean} fields
     * may or may not have their default values ({@code 0} and {@code false}).
     *
     * <p>If the required type is {@link Any}, an instance of an empty {@link Any} wrapped into another {@link Any}
     * is returned. See {@link AnyPacker}.
     *
     * @param clazz Java class of the required stub message
     * @param <M>   type of the required message
     * @return new instance of the given {@link Message} type with random fields
     * @see #builderForType(Class)
     */
    public static <M extends Message> M messageOfType(Class<M> clazz) {
        checkClass(clazz);

        if (Any.class.equals(clazz)) {
            final Any any = Any.getDefaultInstance();
            @SuppressWarnings("unchecked") //
            final M result = (M) AnyPacker.pack(any);
            return result;
        }

        final M.Builder builder = builderForType(clazz);
        @SuppressWarnings("unchecked") // Checked cast
        final M result = (M) builder.build();

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
     * <p>All the protobuf types are supported including nested {@link Message}s and the {@code enum}s.
     *
     * @param field {@link FieldDescriptor} to take the type info from
     * @return a non-default generated value of type of the given field
     */
    @SuppressWarnings("OverlyComplexMethod")
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
                return enumValueFor(field, random);
            case MESSAGE:
                return messageValueFor(field);
            default:
                throw new IllegalArgumentException(format("Field type %s is not supported.", type));
        }
    }

    private static Object enumValueFor(FieldDescriptor field, Random random) {
        final Descriptors.EnumDescriptor descriptor = field.getEnumType();
        final List<Descriptors.EnumValueDescriptor> enumValues = descriptor.getValues();
        if (enumValues.isEmpty()) {
            return null;
        }

        // Value under index 0 is usually used to store `undefined` option
        // Use values with indexes from 1 to n
        final int index = random.nextInt(enumValues.size() - 1) + 1;
        final Descriptors.EnumValueDescriptor enumValue = descriptor.findValueByNumber(index);
        return enumValue;
    }

    private static Message messageValueFor(FieldDescriptor field) {
        final TypeUrl messageType = TypeUrl.from(field.getMessageType());
        final Class<? extends Message> javaClass = classFor(messageType);
        final Message fieldValue = messageOfType(javaClass);
        return fieldValue;
    }

    @SuppressWarnings("unchecked") // Reflective class definition retrieving
    private static <M extends Message> Class<M> classFor(TypeUrl url) {
        final Class<M> javaClass = url.getJavaClass();
        return javaClass;
    }

    private static <B extends Message.Builder> B builderFor(Class<? extends Message> clazz) {
        try {
            final Method factoryMethod = clazz.getDeclaredMethod("newBuilder");
            @SuppressWarnings("unchecked")
            final B result = (B) factoryMethod.invoke(null);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            final String errMsg = format("Class %s must be a generated proto message",
                                         clazz.getCanonicalName());
            throw new IllegalArgumentException(errMsg, e);
        }

    }
}
