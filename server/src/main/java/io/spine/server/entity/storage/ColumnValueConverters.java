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
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.EnumConverters.createFor;
import static io.spine.server.entity.storage.EnumType.ORDINAL;

/**
 * A utility which creates {@link ColumnValueConverter} instances for {@linkplain EntityColumn
 * entity columns}.
 */
final class ColumnValueConverters {

    /**
     * Prevents instantiation of this utility class.
     */
    private ColumnValueConverters() {
    }

    /**
     * Creates {@link ColumnValueConverter} for the {@link EntityColumn} represented by the given
     * getter.
     *
     * @param getter the getter from which both {@link EntityColumn} and its
     *               {@link ColumnValueConverter} are created
     * @return the created {@link ColumnValueConverter}
     */
    static ColumnValueConverter of(Method getter) {
        checkNotNull(getter);
        Class<?> columnType = getter.getReturnType();
        ColumnValueConverter converter;
        if (isEnumType(columnType)) {
            EnumType enumType = getEnumType(getter);
            @SuppressWarnings("unchecked") // Checked at runtime.
            Class<? extends Enum> type = (Class<? extends Enum>) columnType;
            converter = createFor(enumType, type);
        } else {
            @SuppressWarnings("unchecked")
                // It's impossible to create an Entity Column from the non-serializable type getter.
            Class<? extends Serializable> type = (Class<? extends Serializable>) columnType;
            converter = new IdentityConverter(type);
        }
        return converter;
    }

    /**
     * Checks if the specified type is the {@link Enum} type.
     *
     * @param type the type to check
     * @return {@code true} if the specified type is Java Enum, {@code false} otherwise
     */
    private static boolean isEnumType(Class<?> type) {
        boolean isJavaEnum = Enum.class.isAssignableFrom(type);
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
        EnumType type = getter.getAnnotation(Enumerated.class)
                              .value();
        return type;
    }
}
