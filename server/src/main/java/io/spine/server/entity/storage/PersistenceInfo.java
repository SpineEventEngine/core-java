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

import io.spine.annotation.Internal;
import io.spine.server.entity.storage.enumeration.EnumPersistenceTypes;
import io.spine.server.entity.storage.enumeration.EnumType;
import io.spine.server.entity.storage.enumeration.Enumerated;

import java.io.Serializable;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.enumeration.EnumConverters.forType;
import static io.spine.server.entity.storage.enumeration.EnumType.ORDINAL;

/**
 * An info about how the {@link EntityColumn} is persisted in the data storage.
 *
 * @author Dmytro Kuzmin
 * @see Column
 */
@Internal
final class PersistenceInfo implements Serializable {

    private static final long serialVersionUID = 0L;

    private final Class<?> persistedType;
    private final ColumnValueConverter valueConverter;

    private PersistenceInfo(Class<?> persistedType, ColumnValueConverter valueConverter) {
        this.persistedType = persistedType;
        this.valueConverter = valueConverter;
    }

    /**
     * Creates {@link PersistenceInfo} for the {@link EntityColumn} represented by the getter.
     *
     * <p>An annotated version of the getter should be used in case of the object hierarchy.
     *
     * @param getter the getter from which the {@link EntityColumn} is created
     * @return the {@code PersistenceInfo} of the {@link EntityColumn}
     */
    public static PersistenceInfo from(Method getter) {
        checkNotNull(getter);
        final Class<?> returnType = getter.getReturnType();
        if (isEnumType(returnType)) {
            final EnumType enumType = getEnumType(getter);
            final Class<?> type = EnumPersistenceTypes.of(enumType);
            final ColumnValueConverter converter = forType(enumType);
            return new PersistenceInfo(type, converter);
        }
        final ColumnValueConverter converter = new IdentityConverter();
        return new PersistenceInfo(returnType, converter);
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
     * Returns the type under which the {@link EntityColumn} is persisted in the data storage.
     *
     * <p>Currently, non-{@link Enumerated} entity columns are persisted in the storage under the
     * same type as their getter return type, i.e. without changes.
     *
     * <p>For the {@link Enumerated} entity columns see {@link EnumPersistenceTypes}
     *
     * @return the persistence type of the {@link EntityColumn}
     */
    Class<?> getPersistedType() {
        return persistedType;
    }

    /**
     * Returns the converter between the {@link EntityColumn} value and its persisted type.
     *
     * <p>This converter can be used to transform the values obtained through the {@link
     * EntityColumn} getter to the values for persistence in the data storage.
     *
     * @return the converter of the {@link EntityColumn} values
     */
    ColumnValueConverter getValueConverter() {
        return valueConverter;
    }
}
