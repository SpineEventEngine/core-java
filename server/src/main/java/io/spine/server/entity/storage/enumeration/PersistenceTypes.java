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

import io.spine.annotation.Internal;

import java.util.EnumMap;
import java.util.Map;

import static io.spine.server.entity.storage.enumeration.EnumType.ORDINAL;
import static io.spine.server.entity.storage.enumeration.EnumType.STRING;

/**
 * A storage of the persistence types for each of the {@linkplain EnumType enum types}.
 *
 * <p>A persistence type is a type used when storing the {@link Enumerated} field in the data
 * storage.
 *
 * @author Dmytro Kuzmin
 * @see Enumerated
 */
@Internal
public class PersistenceTypes {

    private static final Map<EnumType, Class<?>> persistenceTypes = persistenceTypes();

    /**
     * Prevent instantiation of this class.
     */
    private PersistenceTypes() {
    }

    /**
     * Retrieve persistence type for the given {@link EnumType}.
     *
     * @param type the type of the {@link Enumerated} value
     * @return the persistence type used to store value in the data storage
     */
    public static Class<?> getPersistenceType(EnumType type) {
        return persistenceTypes.get(type);
    }

    private static Map<EnumType, Class<?>> persistenceTypes() {
        final Map<EnumType, Class<?>> map = new EnumMap<>(EnumType.class);
        map.put(ORDINAL, Integer.class);
        map.put(STRING, String.class);
        return map;
    }
}
