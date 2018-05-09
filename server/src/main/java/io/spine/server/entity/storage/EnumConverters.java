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
 * A container for the known {@linkplain EnumConverter enum converters} stored by the
 * {@link EnumType}.
 *
 * @author Dmytro Kuzmin
 */
final class EnumConverters {

    private static final Map<EnumType, EnumConverter> converters = converters();

    /**
     * Prevents instantiation of this class.
     */
    private EnumConverters() {
    }

    /**
     * Retrieves the {@linkplain EnumConverter converter} for the given {@link EnumType}.
     *
     * @param type the {@code EnumType} which defines the conversion method
     * @return the converter for the given type
     * @throws IllegalArgumentException if there is no converter for the specified {@code EnumType}
     */
    static EnumConverter forType(EnumType type) {
        final EnumConverter converter = converters.get(type);
        if (converter == null) {
            throw newIllegalArgumentException(
                    "There is no EnumConverter for the EnumType %s", type.name());
        }
        return converter;
    }

    private static Map<EnumType, EnumConverter> converters() {
        final Map<EnumType, EnumConverter> map = new EnumMap<>(EnumType.class);
        map.put(ORDINAL, new OrdinalEnumConverter());
        map.put(STRING, new StringEnumConverter());
        return unmodifiableMap(map);
    }
}
