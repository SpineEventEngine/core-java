/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.client;

import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.protobuf.TypeConverter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility for converting the {@link ColumnFilter} values represented as {@linkplain Object Java
 * Objects} into the {@linkplain com.google.protobuf.Message Protobuf Messages} (in form of {@link
 * Any}) and vice versa.
 *
 * <p>This class performs all necessary actions preceding the value storage as well as the type
 * conversion itself.
 *
 * <p>{@link ColumnFilter} values can either be {@linkplain com.google.protobuf.Message Protobuf
 * Messages}, Java primitives, or {@link Enum enumerated values}.
 *
 * @author Dmytro Kuzmin
 */
public final class FilterValueConverter {

    /**
     * Prevents instantiation of this utility class.
     */
    private FilterValueConverter() {
    }

    /**
     * Converts the given {@link Any} to the filter value represented as {@linkplain Object Java
     * Object}.
     *
     * @param message     the {@linkplain com.google.protobuf.Message Protobuf Message} in form of
     *                    {@code Any}
     * @param targetClass the {@link Class} representing filter value type
     * @param <T>         the filter value type
     * @return the filter value in the form of Java Object
     */
    public static <T> T toValue(Any message, Class<T> targetClass) {
        checkNotNull(message);
        checkNotNull(targetClass);
        if (isEnum(targetClass)) {
            @SuppressWarnings("unchecked") // Checked at runtime.
            final Class<? extends Enum> enumClass = (Class<? extends Enum>) targetClass;
            final Enum enumValue = enumFromAny(message, enumClass);
            final T filterValue = targetClass.cast(enumValue);
            return filterValue;
        }
        return TypeConverter.toObject(message, targetClass);
    }

    /**
     * Converts the given filter value to Protobuf {@link Any}.
     *
     * @param value the filter value
     * @param <T>   the value type
     * @return the filter value in the form of {@code Any}
     */
    public static <T> Any toAny(T value) {
        checkNotNull(value);
        if (isEnum(value.getClass())) {
            final Enum enumValue = (Enum) value;
            final Any anyValue = enumToAny(enumValue);
            return anyValue;
        }
        return TypeConverter.toAny(value);
    }

    /**
     * Determines whether the given {@linkplain Class type} is Java {@link Enum}.
     *
     * @param type the {@code Class} object representing the type
     * @return true if the type is Java Enum, false otherwise
     */
    private static boolean isEnum(Class<?> type) {
        return Enum.class.isAssignableFrom(type);
    }

    /**
     * Converts the given {@link Enum} value to the Protobuf {@link Any}.
     *
     * @param value the value to convert
     * @param <T>   the type of the {@code Enum}
     * @return the value in the form of {@code Any}
     */
    private static <T extends Enum> Any enumToAny(T value) {
        final String anyValue = value.name();
        return TypeConverter.toAny(anyValue);
    }

    /**
     * Converts the given {@link Any} to the Java {@link Enum} object.
     *
     * @param message   the {@code Any} object to convert
     * @param enumClass the {@code Class} object representing the {@code Enum} type
     * @param <T>       the {@code Enum} type
     * @return the {@code Enum} value
     */
    private static <T extends Enum<T>> T enumFromAny(Any message, Class<T> enumClass) {
        final String enumStoredValue = TypeConverter.toObject(message, String.class);
        return Enum.valueOf(enumClass, enumStoredValue);
    }
}
