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
package org.spine3.protobuf;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import org.spine3.protobuf.error.MissingDescriptorException;
import org.spine3.protobuf.error.UnknownTypeException;
import org.spine3.type.ClassName;
import org.spine3.type.TypeName;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static com.google.protobuf.Descriptors.Descriptor;
import static com.google.protobuf.Descriptors.GenericDescriptor;

/**
 * Utility class for working with {@link Message} objects.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public class Messages {

    @SuppressWarnings("DuplicateStringLiteralInspection") // This constant is used in generated classes.
    private static final String METHOD_GET_DESCRIPTOR = "getDescriptor";

    private Messages() {}

    /**
     * Returns message {@link Class} for the given Protobuf message type.
     *
     * <p>This method is temporary until full support of {@link Any} is provided.
     *
     * @param messageType full type name defined in the proto files
     * @return message class
     * @throws RuntimeException wrapping {@link ClassNotFoundException} if there is no corresponding class
     *                          for the given Protobuf message type
     * @see AnyPacker#fromAny(Any) that uses the same convention
     */
    public static <T extends Message> Class<T> toMessageClass(TypeName messageType) {
        final ClassName className = TypeToClassMap.get(messageType);
        try {
            @SuppressWarnings("unchecked") // the client considers this message is of this class
            final Class<T> result = (Class<T>) Class.forName(className.value());
            return result;
        } catch (ClassNotFoundException e) {
            throw new UnknownTypeException(messageType.value(), e);
        }
    }

    /**
     * Prints the passed message into well formatted text.
     *
     * @param message the message object
     * @return text representation of the passed message
     */
    public static String toText(Message message) {
        checkNotNull(message);
        final String result = TextFormat.printToString(message);
        return result;
    }

    /**
     * Converts passed message into Json representation.
     *
     * @param message the message object
     * @return Json string
     */
    public static String toJson(Message message) {
        checkNotNull(message);
        String result = null;
        try {
            result = JsonPrinter.instance().print(message);
        } catch (InvalidProtocolBufferException e) {
            propagate(e);
        }
        checkState(result != null);
        return result;
    }

    /**
     * Builds and returns the registry of types known in the application.
     *
     * @return {@code JsonFormat.TypeRegistry} instance
     * @see TypeToClassMap#knownTypes()
     */
    public static JsonFormat.TypeRegistry forKnownTypes() {
        final JsonFormat.TypeRegistry.Builder builder = JsonFormat.TypeRegistry.newBuilder();
        for (TypeName typeName : TypeToClassMap.knownTypes()) {
            final Class<? extends Message> clazz = toMessageClass(typeName);
            final GenericDescriptor descriptor = getClassDescriptor(clazz);
            // Skip outer class descriptors.
            if (descriptor instanceof Descriptor) {
                final Descriptor typeDescriptor = (Descriptor) descriptor;
                builder.add(typeDescriptor);
            }
        }
        return builder.build();
    }

    private enum JsonPrinter {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final JsonFormat.Printer value = JsonFormat.printer().usingTypeRegistry(forKnownTypes());

        private static com.google.protobuf.util.JsonFormat.Printer instance() {
            return INSTANCE.value;
        }
    }

    /**
     * Returns descriptor for the passed message class.
     */
    public static GenericDescriptor getClassDescriptor(Class<? extends Message> clazz) {
        try {
            final Method method = clazz.getMethod(METHOD_GET_DESCRIPTOR);
            final GenericDescriptor result = (GenericDescriptor) method.invoke(null);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MissingDescriptorException(clazz, e.getCause());
        }
    }

    /**
     * Returns the class of the Protobuf message field.
     *
     * @param field the field descriptor
     * @return the class of the field
     * @throws IllegalArgumentException if the field type is unknown
     */
    public static Class<?> getFieldClass(FieldDescriptor field) {
        final FieldDescriptor.JavaType javaType = field.getJavaType();
        switch (javaType) {
            case INT:
                return Integer.class;
            case LONG:
                return Long.class;
            case FLOAT:
                return Float.class;
            case DOUBLE:
                return Double.class;
            case BOOLEAN:
                return Boolean.class;
            case STRING:
                return String.class;
            case BYTE_STRING:
                return ByteString.class;
            case ENUM:
                final String enumTypeName = field.getEnumType().getFullName();
                final Class<? extends Message> enumClass = toMessageClass(TypeName.of(enumTypeName));
                return enumClass;
            case MESSAGE:
                final TypeName typeName = TypeName.of(field.getMessageType());
                final Class<? extends Message> msgClass = toMessageClass(typeName);
                return msgClass;
        }
        throw new IllegalArgumentException("Unknown field type discovered: " + field.getFullName());
    }
}
