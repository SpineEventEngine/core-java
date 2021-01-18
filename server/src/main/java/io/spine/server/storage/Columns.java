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

package io.spine.server.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.protobuf.Message;
import io.spine.query.Column;
import io.spine.query.ColumnName;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * Columns stored for a particular record along with their names.
 *
 * @param <R>
 *         the type of the stored record
 * @param <C>
 *         the type of columns
 */
public final class Columns<R extends Message, C extends Column<R, ?>> implements Iterable<C> {

    private final ImmutableMap<ColumnName, C> columns;

    /**
     * Creates a new instance from the passed iterable.
     */
    public Columns(Iterable<C> source) {
        this.columns =
                Streams.stream(source)
                       .collect(toImmutableMap(Column::name, (c) -> c));
    }

    @Override
    public Iterator<C> iterator() {
        return columns.values()
                      .iterator();
    }

    /**
     * Returns the number of columns.
     */
    public int size() {
        return columns.size();
    }

    /**
     * Returns a new stream of the stored columns.
     */
    public Stream<C> stream() {
        return columns.values()
                      .stream();
    }

    /**
     * Finds a particular column definition by the column name.
     *
     * @param name
     *         the name of column to search by
     * @return the found column wrapped into an {@code Optional},
     *         or {@code Optional.empty()} if no column was found
     */
    public Optional<Column<?, ?>> find(ColumnName name) {
        return Optional.ofNullable(columns.get(name));
    }
}
