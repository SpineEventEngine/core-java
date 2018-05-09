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

import java.util.EnumMap;
import java.util.Map;

import static io.spine.server.entity.storage.EnumType.ORDINAL;
import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.Collections.unmodifiableMap;

/**
 * A storage of the {@linkplain Class Java Classes} representing the persistence types for each
 * {@link EnumType}.
 *
 * @author Dmytro Kuzmin
 * @see Enumerated
 */
final class EnumPersistenceTypes {

    private static final Map<EnumType, Class<?>> persistenceTypes = persistenceTypes();

    /**
     * Prevents instantiation of this class.
     */
    private EnumPersistenceTypes() {
    }

    /**
     * Retrieves the persistence type for the given {@link EnumType}.
     *
     * @param type the enum type of the {@link Enumerated} value
     * @return the persistence type used to store the value in the data storage
     * @throws IllegalArgumentException if there is no known persistence type for the specified
     *                                  {@code EnumType}
     */
    public static Class<?> of(EnumType type) {
        final Class<?> persistenceType = persistenceTypes.get(type);
        if (persistenceType == null) {
            throw newIllegalArgumentException(
                    "There is no known persistence type for the EnumType %s", type.name());
        }
        return persistenceType;
    }

    private static Map<EnumType, Class<?>> persistenceTypes() {
        final Map<EnumType, Class<?>> map = new EnumMap<>(EnumType.class);
        map.put(ORDINAL, Integer.class);
        map.put(STRING, String.class);
        return unmodifiableMap(map);
    }
}
