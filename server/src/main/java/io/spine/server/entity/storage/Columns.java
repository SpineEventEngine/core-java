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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.LifecycleFlagField;

import java.util.Map;
import java.util.Optional;

import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.stream.Collectors.toMap;

@Internal
public final class Columns {

    /**
     * A map of entity columns by their name.
     */
    private final ImmutableMap<ColumnName, Column> columns;
    private final EntityClass<?> entityClass;

    private Columns(ImmutableMap<ColumnName, Column> columns, EntityClass<?> entityClass) {
        this.columns = columns;
        this.entityClass = entityClass;
    }

    public static Columns of(EntityClass<?> entityClass) {
        ColumnIntrospector introspector = new ColumnIntrospector(entityClass);
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        columns.putAll(introspector.systemColumns());
        columns.putAll(introspector.protoColumns());
        return new Columns(columns.build(), entityClass);
    }

    public Column get(ColumnName columnName) {
        Column result = find(columnName).orElseThrow(() -> columnNotFound(columnName));
        return result;
    }

    public Optional<Column> find(ColumnName columnName) {
        Column column = columns.get(columnName);
        Optional<Column> result = Optional.ofNullable(column);
        return result;
    }

    public ImmutableCollection<Column> columnList() {
        return columns.values();
    }

    public Map<ColumnName, Object> valuesIn(Entity<?, ?> source) {
        Map<ColumnName, Object> result =
                columns.values()
                       .stream()
                       .collect(toMap(Column::name,
                                      column -> column.valueIn(source)));
        return result;
    }

    /**
     * Returns a subset of columns corresponding to the lifecycle of the entity.
     */
    public Columns lifecycleColumns() {
        ImmutableMap.Builder<ColumnName, Column> lifecycleColumns = ImmutableMap.builder();
        for (LifecycleFlagField field : LifecycleFlagField.values()) {
            ColumnName name = ColumnName.of(field.name());
            Optional<Column> column = find(name);
            column.ifPresent(col -> lifecycleColumns.put(name, col));
        }
        return new Columns(lifecycleColumns.build(), entityClass);
    }

    private IllegalStateException columnNotFound(ColumnName columnName) {
        throw newIllegalStateException(
                "A column with name '%s' not found in entity state class `%s`.",
                columnName, entityClass.stateClass()
                                       .getCanonicalName());
    }
}
