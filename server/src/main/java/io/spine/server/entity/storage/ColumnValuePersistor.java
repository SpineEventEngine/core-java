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

package io.spine.server.entity.storage;

import java.io.Serializable;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.EnumConverters.forType;
import static io.spine.server.entity.storage.EnumType.ORDINAL;

/**
 * A class performing conversions necessary for the {@link EntityColumn} value persistence in the
 * data storage.
 *
 * <p>This class transforms {@link EntityColumn} value into the type which is persisted in the data
 * storage. It also can provide user with the information about the persistence type of the {@link
 * EntityColumn}.
 *
 * @author Dmytro Kuzmin
 * @see Column
 */
final class ColumnValuePersistor {

    private final Class<?> persistedType;
    private final ColumnValueConverter converter;

    private ColumnValuePersistor(Class<?> persistedType, ColumnValueConverter converter) {
        this.persistedType = persistedType;
        this.converter = converter;
    }

    /**
     * Creates a {@link ColumnValuePersistor} for the {@link EntityColumn} represented by the
     * getter.
     *
     * <p>An annotated version of the getter should be used in case of the object hierarchy.
     *
     * @param getter the getter from which the {@link EntityColumn} is created
     * @return the {@code ColumnValuePersistor} for the {@link EntityColumn}
     */
    static ColumnValuePersistor from(Method getter) {
        checkNotNull(getter);
        final Class<?> columnType = getter.getReturnType();
        Class<?> persistenceType;
        ColumnValueConverter converter;
        if (isEnumType(columnType)) {
            final EnumType enumType = getEnumType(getter);
            persistenceType = EnumPersistenceTypes.of(enumType);
            converter = forType(enumType);
        } else {
            persistenceType = columnType;
            converter = new IdentityConverter();
        }
        return new ColumnValuePersistor(persistenceType, converter);
    }

    /**
     * Checks if the specified type is the {@link Enum} type.
     *
     * @param type the type to check
     * @return {@code true} if the specified type is Java Enum, {@code false} otherwise
     */
    private static boolean isEnumType(Class<?> type) {
        final boolean isJavaEnum = Enum.class.isAssignableFrom(type);
        return isJavaEnum;
    }

    /**
     * Obtains the {@link EnumType} from the {@linkplain Enumerated getter annotation}.
     *
     * <p>If the annotation is not present, the {@linkplain EnumType#ORDINAL ordinal} enum type is
     * returned.
     *
     * <p>This method only handles the {@link Enumerated} annotation of the method and doesn't
     * perform the actual return value check.
     *
     * @param getter the getter to obtain the enum type from
     * @return the {@code EnumType} specified for the getter
     * @see Enumerated
     * @see Column
     */
    private static EnumType getEnumType(Method getter) {
        if (!getter.isAnnotationPresent(Enumerated.class)) {
            return ORDINAL;
        }
        final EnumType type = getter.getAnnotation(Enumerated.class)
                                    .value();
        return type;
    }

    /**
     * Converts the {@link EntityColumn} value to the type persisted in the data storage.
     *
     * <p>The value is assumed to be of the {@linkplain EntityColumn#getType() entity column type},
     * so it is suitable for the {@linkplain ColumnValueConverter#convert(Object) conversion} to
     * the persisted type.
     *
     * @param columnValue the column value to convert
     * @return the converted {@link EntityColumn} value
     */
    Serializable toPersistedValue(Object columnValue) {
        checkNotNull(columnValue);
        final Serializable convertedValue = converter.convert(columnValue);
        return convertedValue;
    }

    /**
     * Returns the type under which the {@link EntityColumn} is persisted in the data storage.
     *
     * <p>Currently, non-{@link Enumerated} entity columns are persisted in the storage under the
     * same type as their getter return type, i.e. without changes.
     *
     * <p>For the {@link Enumerated} entity columns, see {@link EnumPersistenceTypes}.
     *
     * @return the persistence type of the {@link EntityColumn}
     */
    Class<?> getPersistedType() {
        return persistedType;
    }
}
