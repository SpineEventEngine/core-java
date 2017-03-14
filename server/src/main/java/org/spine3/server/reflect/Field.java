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

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.FieldFilter;
import org.spine3.protobuf.AnyPacker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

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

    private final Method getter;

    private static Optional<Field> newField(Class<? extends Message> messageClass,
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
     * @param filter the field filter
     * @return an {@code Field} wrapped into {@code Optional} or
     *         {@code Optional.absent()} if there is no such field in the passed message class
     */
    public static Optional<Field> forFilter(Class<? extends Message> messageClass,
                                            FieldFilter filter) {
        final String fieldName = getFieldName(filter);
        return newField(messageClass, fieldName);
    }

    private Field(String name, Method getter) {
        this.name = name;
        this.getter = getter;
    }

    /**
     * Obtains the name of the field.
     */
    public String getName() {
        return name;
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
     * Obtains the value of the field in the passed object.
     *
     * <p>If the corresponding field is of type {@code Any} it will be unpacked.
     *
     * @throws IllegalStateException if getting the field value caused an exception.
     * The root cause will be available from the thrown instance.
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
            final Throwable rootCause = getRootCause(e);
            throw new IllegalStateException(rootCause);
        }

        return result;
    }
}
