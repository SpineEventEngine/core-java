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

package io.spine.server.entity.storage.enumeration;

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;

import java.io.Serializable;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.enumeration.EnumConverters.forType;
import static io.spine.server.entity.storage.enumeration.EnumType.ORDINAL;

/**
 * A representation of the enumerated value of the {@link
 * io.spine.server.entity.storage.EntityColumn}.
 *
 * <p>For the non-enum entity columns, the {@link EnumeratedValue} will be {@linkplain #isEmpty()
 * empty}.
 *
 * <p>For the entity columns storing {@link Enum} types, the {@link EnumeratedValue} will provide
 * a way to retrieve an object for persistence in the data storage for each given {@link
 * io.spine.server.entity.Entity}.
 *
 * @author Dmytro Kuzmin
 */
@Internal
public class EnumeratedValue implements Serializable {

    private static final long serialVersionUID = 0L;

    private final boolean empty;

    private EnumType type;

    private EnumeratedValue() {
        this.empty = true;
    }

    private EnumeratedValue(EnumType type) {
        this.type = type;
        this.empty = false;
    }

    public static EnumeratedValue from(Method getter) {
        checkNotNull(getter);
        if (!isEnumType(getter)) {
            return new EnumeratedValue();
        }
        final EnumType type = enumTypeFromAnnotation(getter);
        return new EnumeratedValue(type);
    }

    private static boolean isEnumType(Method getter) {
        final Class<?> returnType = getter.getReturnType();
        final boolean isJavaEnum = Enum.class.isAssignableFrom(returnType);
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

    public boolean isEmpty() {
        return empty;
    }

    public Class<?> getPersistenceType() {
        final Class<?> storedType = PersistenceTypes.getPersistenceType(type);
        return storedType;
    }

    public Serializable getFor(Enum value) {
        checkNotNull(value);
        final EnumConverter<? extends Serializable> converter = forType(type);
        final Serializable result = converter.convert(value);
        return result;
    }

    @VisibleForTesting
    EnumType getEnumType() {
        return type;
    }
}
