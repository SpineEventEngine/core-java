/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.query.ColumnName;
import io.spine.query.CustomColumn;
import io.spine.query.RecordColumn;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;

import java.util.function.Supplier;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

/**
 * Columns storing the internal framework-specific attributes of an {@code Entity}.
 */
@Internal
public enum EntityRecordColumn implements Supplier<CustomColumn<Entity<?, ?>, ?>> {

    //TODO:2020-07-16:alex.tymchenko: document!
    archived(new ArchivedColumn()),

    deleted(new DeletedColumn()),

    version(new VersionColumn());

    private final CustomColumn<Entity<?, ?>, ?> column;

    EntityRecordColumn(CustomColumn<Entity<?, ?>, ?> column) {
        this.column = column;
    }

    @Override
    public CustomColumn<Entity<?, ?>, ?> get() {
        return column;
    }

    /**
     * Returns the name of the column.
     */
    public ColumnName columnName() {
        return get().name();
    }

    /**
     * Obtains the column treating it as a lifecycle column with {@code Boolean} values.
     *
     * <p>If the column has the type of values different from {@code Boolean},
     * the {@link ClassCastException} is thrown.
     */
    @SuppressWarnings("unchecked")
    public CustomColumn<Entity<?, ?>, Boolean> lifecycle() {
        CustomColumn<Entity<?, ?>, Boolean> boolColumn =
                (CustomColumn<Entity<?, ?>, Boolean>) this.column;
        return boolColumn;
    }

    <V> RecordColumn<EntityRecord, V> asRecordColumn(Class<V> valueType) {
        return AsEntityRecordColumn.apply(column, valueType);
    }

    /**
     * Returns the names of all columns.
     */
    @VisibleForTesting
    static ImmutableSet<ColumnName> columnNames() {
        return ImmutableSet.of(archived.columnName(), deleted.columnName(), version.columnName());
    }

    @VisibleForTesting
    static ImmutableList<CustomColumn<Entity<?, ?>, ?>> columns() {
        ImmutableList<CustomColumn<Entity<?, ?>, ?>> result =
                stream(values()).map(EntityRecordColumn::get)
                                .collect(toImmutableList());
        return result;
    }
}
