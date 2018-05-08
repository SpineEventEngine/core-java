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
import io.spine.server.entity.storage.PersistentValueConverter;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An interface for the converter of {@link Enum} value to the arbitrary {@link Serializable}
 * value.
 *
 * @author Dmytro Kuzmin
 */
@Internal
public abstract class EnumConverter implements PersistentValueConverter {

    private static final long serialVersionUID = 0L;

    @Override
    public Serializable convert(Object value) {
        checkNotNull(value);
        if (!isEnumType(value)) {
            throw newIllegalArgumentException(
                    "Value passed to the EnumConverter should be of Enum type, actual type: %s",
                    value.getClass());
        }
        final Enum enumValue = (Enum) value;
        final Serializable convertedValue = doConvert(enumValue);
        return convertedValue;
    }

    /**
     * Convert the given {@link Enum} value into the arbitrary {@link Serializable} value.
     *
     * @param value the value to convert
     * @return the converted value
     */
    protected abstract Serializable doConvert(Enum value);

    private boolean isEnumType(Object value) {
        return Enum.class.isAssignableFrom(value.getClass());
    }
}
