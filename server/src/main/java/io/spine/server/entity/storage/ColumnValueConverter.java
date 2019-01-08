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

import io.spine.server.entity.Entity;

import java.io.Serializable;

/**
 * An interface for converting the {@link EntityColumn} values to their persisted type.
 */
interface ColumnValueConverter {

    /**
     * Converts the {@link EntityColumn} value to its persisted type.
     *
     * <p>The input value is assumed to be the one {@linkplain EntityColumn#memoizeFor(Entity)
     * retrieved} via the {@link EntityColumn} getter.
     *
     * <p>The output value is the corresponding value to be used in the data storage.
     *
     * @param value the value to convert
     * @return the value used for the persistence in the data storage
     */
    Serializable convert(Object value);

    /**
     * Returns the source {@link Class} of the conversion, i.e. {@link EntityColumn} getter type.
     *
     * @return the source type of the conversion
     */
    Class<?> getSourceType();

    /**
     * Returns the target {@link Class} of the conversion, i.e. {@link EntityColumn} persisted type.
     *
     * @return the target type of the conversion
     */
    Class<? extends Serializable> getTargetType();
}
