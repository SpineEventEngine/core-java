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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link ColumnValueConverter} which returns the same value as a result of the conversion.
 *
 * @author Dmytro Kuzmin
 */
final class IdentityConverter implements ColumnValueConverter {

    private final Class<? extends Serializable> type;

    IdentityConverter(Class<? extends Serializable> type) {
        this.type = type;
    }

    @Override
    public Serializable convert(Object value) {
        checkNotNull(value);
        Serializable result = (Serializable) value;
        return result;
    }

    @Override
    public Class<?> getSourceType() {
        return type;
    }

    @Override
    public Class<? extends Serializable> getTargetType() {
        return type;
    }
}
