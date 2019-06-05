/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An abstract base for converting the {@link Enum} entity column value into the value for
 * persistence in the data storage.
 *
 * @see EnumConverters
 * @see EnumType
 */
abstract class EnumConverter implements ColumnValueConverter {

    private final Class<? extends Enum> sourceType;

    EnumConverter(Class<? extends Enum> sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Converts the passed enum value to its {@linkplain #convertEnumValue(Enum)} serializable}
     * form.
     *
     * @throws IllegalArgumentException in case the passed value is not of the {@link Enum} type
     */
    @Override
    public Serializable convert(Object value) {
        checkNotNull(value);
        if (!isEnumType(value)) {
            throw newIllegalArgumentException(
                    "Value passed to the EnumConverter should be of Enum type, actual type: %s",
                    value.getClass());
        }
        Enum enumValue = (Enum) value;
        Serializable convertedValue = convertEnumValue(enumValue);
        return convertedValue;
    }

    @Override
    public Class<? extends Enum> sourceType() {
        return sourceType;
    }

    /**
     * Converts the given {@link Enum} value into the {@link Serializable} value which can be used
     * for persistence in the data storage.
     *
     * @param value the value to convert
     * @return the converted value
     */
    abstract Serializable convertEnumValue(Enum value);

    /**
     * Checks if the passed value is of the {@link Enum} type.
     *
     * @param value the value to check
     * @return {@code true} if the value is of the {@link Enum} type, {@code false} otherwise
     */
    private static boolean isEnumType(Object value) {
        Class<?> valueType = value.getClass();
        boolean isJavaEnum = Enum.class.isAssignableFrom(valueType);
        return isJavaEnum;
    }
}
