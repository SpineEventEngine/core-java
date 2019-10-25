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

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.LifecycleFlagField;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.util.Exceptions.newIllegalArgumentException;

@Immutable
@Internal
public final class Columns {

    private final ImmutableMap<ColumnName, SpineColumn> systemColumns;
    private final ImmutableMap<ColumnName, SimpleColumn> simpleColumns;
    private final ImmutableMap<ColumnName, ImplementedColumn> implementedColumns;

    private final EntityClass<?> entityClass;

    private Columns(
            ImmutableMap<ColumnName, SpineColumn> systemColumns,
            ImmutableMap<ColumnName, SimpleColumn> simpleColumns,
            ImmutableMap<ColumnName, ImplementedColumn> implementedColumns,
            EntityClass<?> entityClass) {
        this.systemColumns = systemColumns;
        this.simpleColumns = simpleColumns;
        this.implementedColumns = implementedColumns;
        this.entityClass = entityClass;
    }

    public static Columns of(EntityClass<?> entityClass) {
        checkNotNull(entityClass);
        Introspector introspector = new Introspector(entityClass);
        return new Columns(introspector.systemColumns(),
                           introspector.simpleColumns(),
                           introspector.implementedColumns(),
                           entityClass);
    }

    public static Columns of(Class<? extends Entity<?, ?>> entityClass) {
        return of(asEntityClass(entityClass));
    }

    public Column get(ColumnName columnName) {
        checkNotNull(columnName);
        Column result = find(columnName).orElseThrow(() -> columnNotFound(columnName));
        return result;
    }

    public Optional<Column> find(ColumnName columnName) {
        checkNotNull(columnName);
        Column column = systemColumns.get(columnName);
        if (column == null) {
            column = simpleColumns.get(columnName);
        }
        if (column == null) {
            column = implementedColumns.get(columnName);
        }
        return Optional.ofNullable(column);
    }

    public Map<ColumnName, Object> valuesIn(Entity<?, ?> source) {
        Map<ColumnName, Object> result = new HashMap<>();
        systemColumns.forEach(
                (name, column) -> result.put(name, column.valueIn(source))
        );
        simpleColumns.forEach(
                (name, column) -> result.put(name, column.valueIn(source.state()))
        );
        implementedColumns.forEach(
                (name, column) -> result.put(name, column.valueIn(source.state()))
        );
        return result;
    }

    public ImmutableMap<ColumnName, Column> allColumns() {
        ImmutableMap.Builder<ColumnName, Column> builder = ImmutableMap.builder();
        builder.putAll(systemColumns);
        builder.putAll(simpleColumns);
        builder.putAll(implementedColumns);
        return builder.build();
    }

    /**
     * Returns a subset of columns corresponding to the lifecycle of the entity.
     */
    public ImmutableMap<ColumnName, SpineColumn> lifecycleColumns() {
        ImmutableMap.Builder<ColumnName, SpineColumn> result = ImmutableMap.builder();
        for (LifecycleFlagField field : LifecycleFlagField.values()) {
            ColumnName name = ColumnName.of(field.name());
            SpineColumn column = systemColumns.get(name);
            if (column != null) {
                result.put(name, column);
            }
        }
        return result.build();
    }

    public ImmutableMap<ColumnName, ImplementedColumn> implementedColumns() {
        return implementedColumns;
    }

    private IllegalStateException columnNotFound(ColumnName columnName) {
        throw newIllegalArgumentException(
                "A column with name '%s' not found in entity state class `%s`.",
                columnName, entityClass.stateClass()
                                       .getCanonicalName());
    }
}
