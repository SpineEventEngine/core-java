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

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A utility for the creation of {@linkplain EnumConverter enum converters} for the given
 * {@link EnumType}.
 */
final class EnumConverters {

    /** Prevents instantiation of this class. */
    private EnumConverters() {
    }

    /**
     * Creates a {@link EnumConverter} for the given {@link EnumType} and {@link Class source type}.
     *
     * @param enumType   the {@code EnumType} which defines the conversion method
     * @param sourceType the source {@code Class} of the conversion
     * @return the converter for the given enum type and the source class
     */
    static EnumConverter createFor(EnumType enumType, Class<? extends Enum> sourceType) {
        checkNotNull(enumType);
        checkNotNull(sourceType);
        switch (enumType) {
            case ORDINAL:
                return new OrdinalEnumConverter(sourceType);
            case STRING:
                return new StringEnumConverter(sourceType);
            default:
                throw newIllegalArgumentException(
                        "There is no EnumConverter for the EnumType %s", enumType.name());
        }
    }
}
