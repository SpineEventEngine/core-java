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

@Internal
public class ColumnFilterValues {

    private ColumnFilterValues() {
    }

    public static <T> Any toAny(T value) {
        checkNotNull(value);
        if (isEnum(value.getClass())) {
            final Enum enumValue = (Enum) value;
            final Any enumValueToStore = enumToAny(enumValue);
            return enumValueToStore;
        }
        return TypeConverter.toAny(value);
    }

    public static <T> T toValue(Any message, Class<T> targetClass) {
        checkNotNull(message);
        checkNotNull(targetClass);
        if (isEnum(targetClass)) {
            @SuppressWarnings("unchecked") // Checked at runtime.
            final Class<? extends Enum> enumClass = (Class<? extends Enum>) targetClass;
            final Enum enumValue = enumFromAny(message, enumClass);
            final T result = targetClass.cast(enumValue);
            return result;
        }
        return TypeConverter.toObject(message, targetClass);
    }

    private static boolean isEnum(Class<?> type) {
        return Enum.class.isAssignableFrom(type);
    }

    private static <T extends Enum> Any enumToAny(T value) {
        final String anyValue = value.name();
        return TypeConverter.toAny(anyValue);
    }

    private static <T extends Enum<T>> T enumFromAny(Any message, Class<T> enumClass) {
        final String enumStoredValue = TypeConverter.toObject(message, String.class);
        return Enum.valueOf(enumClass, enumStoredValue);
    }
}
