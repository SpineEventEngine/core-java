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

package org.spine3.server.reflect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.spine3.base.FieldFilter;
import org.spine3.protobuf.AnyPacker;
import org.spine3.type.TypeUrl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.newIllegalArgumentException;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * Provides information and dynamic access to a field of a {@code Message}.
 *
 * @author Alexander Yevsyukov
 */
public final class Field {

    /**
     * The name of the field as declared in the proto type.
     */
    private final String name;

    /**
     * The method of obtaining the field value.
     */
    private final Method getter;

    private Field(String name, Method getter) {
        this.name = name;
        this.getter = getter;
    }

    /**
     * Creates a new instance for a field of a message class.
     *
     * @param messageClass the class with the field
     * @param name         the field name
     * @return new field instance
     */
    @VisibleForTesting
    static Optional<Field> newField(Class<? extends Message> messageClass,
                                            String name) {
        checkNotNull(messageClass);
        checkNotNull(name);

        final Method getter;
        try {
            getter = Classes.getGetterForField(messageClass, name);
        } catch (NoSuchMethodException ignored) {
            return Optional.absent();
        }

        final Field field = new Field(name, getter);
        return Optional.of(field);
    }

    /**
     * Creates instance for a field specified by the passed filter.
     *
     * @param messageClass the class of messages containing the field
     * @param filter       the field filter
     * @return an {@code Field} wrapped into {@code Optional} or
     * {@code Optional.absent()} if there is no such field in the passed message class
     */
    public static Optional<Field> forFilter(Class<? extends Message> messageClass,
                                            FieldFilter filter) {
        checkNotNull(messageClass);
        checkNotNull(filter);
        final String fieldName = getFieldName(filter);
        return newField(messageClass, fieldName);
    }

    private static String getFieldName(FieldFilter filter) {
        final String fieldPath = filter.getFieldPath();
        final String fieldName = fieldPath.substring(fieldPath.lastIndexOf('.') + 1);

        if (fieldName.isEmpty()) {
            throw newIllegalArgumentException(
                    "Unable to get a field name from the field filter: %s",
                    filter);
        }
        return fieldName;
    }

    /**
     * Returns the class of the Protobuf message field.
     *
     * @param field the field descriptor
     * @return the class of the field
     * @throws IllegalArgumentException if the field type is unknown
     */
    @SuppressWarnings("OverlyComplexMethod")    // as each branch is a fairly simple.
    public static Class<?> getFieldClass(FieldDescriptor field) {
        checkNotNull(field);
        final FieldDescriptor.JavaType javaType = field.getJavaType();
        final TypeUrl typeUrl;
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
                typeUrl = TypeUrl.from(field.getEnumType());
                final Class<? extends Message> enumClass = typeUrl.getJavaClass();
                return enumClass;
            case MESSAGE:
                typeUrl = TypeUrl.from(field.getMessageType());
                final Class<? extends Message> msgClass = typeUrl.getJavaClass();
                return msgClass;
            default:
                throw newIllegalArgumentException("Unknown field type discovered: %s",
                                                   field.getFullName());
        }
    }

    /**
     * Obtains the name of the field.
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains the value of the field in the passed object.
     *
     * <p>If the corresponding field is of type {@code Any} it will be unpacked.
     *
     * @throws IllegalStateException if getting the field value caused an exception.
     *                               The root cause will be available from the thrown instance.
     */
    public Message getValue(Message object) {
        final Message fieldValue;
        final Message result;
        try {
            fieldValue = (Message) getter.invoke(object);

            result = fieldValue instanceof Any
                     ? AnyPacker.unpack((Any) fieldValue)
                     : fieldValue;

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw wrappedCause(e);
        }

        return result;
    }
}
