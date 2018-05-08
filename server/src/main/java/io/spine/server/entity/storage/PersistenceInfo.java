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

import io.spine.server.entity.storage.enumeration.EnumConverters;
import io.spine.server.entity.storage.enumeration.EnumPersistenceTypes;
import io.spine.server.entity.storage.enumeration.EnumType;
import io.spine.server.entity.storage.enumeration.Enumerated;

import java.io.Serializable;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.enumeration.EnumType.ORDINAL;

public class PersistenceInfo implements Serializable {

    private static final long serialVersionUID = 0L;

    private final Class<?> persistenceType;
    private final PersistentValueConverter valueConverter;

    private PersistenceInfo(Class<?> persistenceType, PersistentValueConverter valueConverter) {
        this.persistenceType = persistenceType;
        this.valueConverter = valueConverter;
    }

    public static PersistenceInfo from(Method getter) {
        checkNotNull(getter);
        final Class<?> returnType = getter.getReturnType();
        if (!isEnumType(returnType)) {
            final PersistentValueConverter converter = new IdentityConverter();
            return new PersistenceInfo(returnType, converter);
        }
        final EnumType enumType = enumTypeFromAnnotation(getter);
        final Class<?> type = EnumPersistenceTypes.getPersistenceType(enumType);
        final PersistentValueConverter converter = EnumConverters.forType(enumType);
        return new PersistenceInfo(type, converter);
    }

    private static boolean isEnumType(Class<?> type) {
        final boolean isJavaEnum = Enum.class.isAssignableFrom(type);
        return isJavaEnum;
    }

    private static EnumType enumTypeFromAnnotation(Method getter) {
        if (!getter.isAnnotationPresent(Enumerated.class)) {
            return ORDINAL;
        }
        final EnumType type = getter.getAnnotation(Enumerated.class)
                                    .value();
        return type;
    }

    public Class<?> getPersistenceType() {
        return persistenceType;
    }

    public PersistentValueConverter getValueConverter() {
        return valueConverter;
    }
}
