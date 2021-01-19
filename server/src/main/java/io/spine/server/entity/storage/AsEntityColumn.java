/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.server.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * A type of {@link Column}s which values are calculated on top of an instance of {@code Entity}.
 *
 * <p>It serves as an adapter for the columns using another type as a source for extracting
 * values, which may be obtained from an instance of {@code Entity}. In particular, it converts
 * the Entity state-based columns (which originally use Protobuf {@code Message}s as a source
 * for values) into the columns taking {@code Entity} instance to calculate their values.
 * In this way, Entity state-based columns become uniform with other Entity instance-based columns,
 * such as lifecycle columns.
 *
 * <p>The returning column preserves the name and the type of the wrapped original column.
 *
 * <p>Callee must supply a new getter to extract the value for the resulting column from
 * the {@code Entity}. However, due to the limitations of Java generics, the type of
 * the returning value is not checked to be the same as the original type of the wrapped column.
 * See the implementation note on this matter.
 *
 * @param <E>
 *         the type of {@code Entity} serving as a source for the column value
 * @implNote This class uses a raw form of the returning value for
 *         {@link #type() Class type()} method, since it is not possible to use the type of the
 *         wrapped column to parameterize the type of the returning value of {@code Class<T>}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})    /* See the impl. note. */
final class AsEntityColumn<E extends Entity<?, ?>> implements Column<E, Object> {

    private final Column<?, ?> column;
    private final Function<E, Object> getter;

    /**
     * Creates a new instance of the adapter-column.
     *
     * @param column
     *         the column to wrap
     * @param getter
     *         a getter extracting the column value from the {@code Entity}
     */
    AsEntityColumn(Column<?, ?> column, Function<E, Object> getter) {
        this.column = column;
        this.getter = getter;
    }

    @Override
    public ColumnName name() {
        return column.name();
    }

    @Override
    public Class type() {
        return column.type();
    }

    @Override
    public @Nullable Object valueIn(E source) {
        return getter.apply(source);
    }
}
