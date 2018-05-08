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

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import static io.spine.server.entity.storage.enumeration.EnumType.ORDINAL;
import static io.spine.server.entity.storage.enumeration.EnumType.STRING;

/**
 * A container for the known {@linkplain EnumConverter enum converters} stored by the {@link
 * EnumType}.
 *
 * @author Dmytro Kuzmin
 */
@Internal
public final class EnumConverters {

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
     */
    public static EnumConverter forType(EnumType type) {
        return converters.get(type);
    }

    private static Map<EnumType, EnumConverter> converters() {
        final Map<EnumType, EnumConverter> map = new EnumMap<>(EnumType.class);
        map.put(ORDINAL, new OrdinalEnumConverter());
        map.put(STRING, new StringEnumConverter());
        return map;
    }
}
