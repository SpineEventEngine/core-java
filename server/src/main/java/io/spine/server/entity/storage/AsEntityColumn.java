/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.base.EntityState;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.server.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * A type of {@link Column}s which values are calculated
 * on top of an instance of {@code Entity} state.
 *
 * <p>The returning column preserves the name and the type of the wrapped original column.
 *
 * <p>Callee must supply a new getter to extract the value for the resulting column from
 * the {@code Entity} state. However, due to the limitations of Java generics, the type of
 * the returning value is not checked to be the same as the original type of the wrapped column.
 * See the implementation note on this matter.
 *
 * @param <S>
 *         the type of {@code Entity} state serving as a source for the column value
 * @implNote This class uses a raw form of the returning value for
 *         {@link #type() Class type()} method, since it is not possible to use the type of the
 *         wrapped column to parameterize the type of the returning value of {@code Class<T>}.
 */
// TODO:alex.tymchenko:2023-10-27: kill!
@SuppressWarnings({"rawtypes", "unchecked"})    /* See the impl. note. */
final class AsEntityColumn<S extends EntityState<?>> implements Column<S, Object> {

    private final Column<?, ?> column;
    private final Function<S, Object> getter;

    /**
     * Creates a new instance of the adapter-column.
     *
     * @param column
     *         the column to wrap
     * @param getter
     *         a getter extracting the column value from the {@code Entity} state
     */
    AsEntityColumn(Column<?, ?> column, Function<S, Object> getter) {
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
    public @Nullable Object valueIn(S source) {
        return getter.apply(source);
    }
}
