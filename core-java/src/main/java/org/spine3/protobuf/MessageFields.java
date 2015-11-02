/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * Utility class for working with message fields.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class MessageFields {
    /**
     * The prefix of generated getter methods for fields.
     */
    private static final String GETTER_METHOD_PREFIX = "get";
    /**
     * By convention underscore is used for separating words in field names of Protobuf messages.
     */
    private static final char PROPERTY_NAME_SEPARATOR = '_';

    private static final Map<AccessorMethodKey, Method> methodsMap = Maps.newHashMap();

    private MessageFields() {
    }

    /**
     * Get cached {@link Method} for class by field index.
     *
     * @param clazz      class which contains the method
     * @param fieldIndex method's field index
     * @return instance of (@link Method}. Returns null if nothing is found
     */
    protected static Method get(Class<? extends Message> clazz, int fieldIndex) {
        final AccessorMethodKey accessorMethodKey = new AccessorMethodKey(clazz, fieldIndex);

        final Method method = methodsMap.get(accessorMethodKey);
        return method;
    }

    /**
     * Puts {@link Method} into cache for further usage.
     *
     * @param clazz      class which contains the method
     * @param fieldIndex method's field index
     * @param method     instance of (@link Method}
     */
    protected static void put(Class<? extends Message> clazz, int fieldIndex, Method method) {
        final AccessorMethodKey accessorMethodKey = new AccessorMethodKey(clazz, fieldIndex);
        methodsMap.put(accessorMethodKey, method);
    }

    public static Descriptors.FieldDescriptor getFieldDescriptor(MessageOrBuilder msg, int fieldIndex) {
        final Descriptors.FieldDescriptor result = msg.getDescriptorForType().getFields().get(fieldIndex);
        return result;
    }

    /**
     * Converts Protobuf field name into Java accessor method name.
     */
    public static String toAccessorMethodName(CharSequence fieldName) {
        final StringBuilder out = new StringBuilder(checkNotNull(fieldName).length() + 3);
        out.append(GETTER_METHOD_PREFIX);
        final char uppercaseFirstChar = Character.toUpperCase(fieldName.charAt(0));
        out.append(uppercaseFirstChar);

        boolean nextUpperCase = false;
        for (int i = 1; i < fieldName.length(); i++) {
            final char c = fieldName.charAt(i);
            if (PROPERTY_NAME_SEPARATOR == c) {
                nextUpperCase = true;
                continue;
            }
            out.append(nextUpperCase ? Character.toUpperCase(c) : c);
            nextUpperCase = false;
        }

        return out.toString();
    }

    /**
     * Reads field from the passed message by its index.
     *
     * @param command    a message to inspect
     * @param fieldIndex a zero-based index of the field
     * @return value a value of the field
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static Object getFieldValue(Message command, int fieldIndex) {
        final Class<? extends Message> commandClass = command.getClass();
        Method method = get(commandClass, fieldIndex);

        if (method == null) {
            final Descriptors.FieldDescriptor fieldDescriptor = getFieldDescriptor(command, fieldIndex);
            final String fieldName = fieldDescriptor.getName();
            final String methodName = toAccessorMethodName(fieldName);

            try {
                method = commandClass.getMethod(methodName);
                put(commandClass, fieldIndex, method);
            } catch (NoSuchMethodException e) {
                throw propagate(e);
            }
        }
        try {
            final Object result = method.invoke(command);
            return result;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

    /**
     * Obtains Protobuf field name for the passed message.
     *
     * @param msg   a message to inspect
     * @param index a zero-based index of the field
     * @return name of the field
     */
    @SuppressWarnings("TypeMayBeWeakened") // Enforce type for API clarity.
    public static String getFieldName(Message msg, int index) {
        final Descriptors.FieldDescriptor fieldDescriptor = getFieldDescriptor(msg, index);
        final String fieldName = fieldDescriptor.getName();
        return fieldName;
    }

    private static class AccessorMethodKey {
        private final Class clazz;
        private final int fieldIndex;

        protected AccessorMethodKey(Class clazz, int fieldIndex) {
            this.clazz = clazz;
            this.fieldIndex = fieldIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, fieldIndex);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final AccessorMethodKey other = (AccessorMethodKey) obj;
            return Objects.equals(this.clazz, other.clazz)
                    && Objects.equals(this.fieldIndex, other.fieldIndex);
        }
    }
}
