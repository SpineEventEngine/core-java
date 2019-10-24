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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.LifecycleFlagField;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.function.Function.identity;

@Immutable
@Internal
public final class Columns {

    /**
     * A map of entity columns by their name.
     */
    private final ImmutableMap<ColumnName, Column> columns;
    private final EntityClass<?> entityClass;

    @VisibleForTesting
    Columns(Map<ColumnName, Column> columns, EntityClass<?> entityClass) {
        this.columns = ImmutableMap.copyOf(columns);
        this.entityClass = entityClass;
    }

    public static Columns of(EntityClass<?> entityClass) {
        checkNotNull(entityClass);
        Introspector introspector = new Introspector(entityClass);
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        columns.putAll(introspector.systemColumns());
        columns.putAll(introspector.protoColumns());
        return new Columns(columns.build(), entityClass);
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
        Column column = columns.get(columnName);
        Optional<Column> result = Optional.ofNullable(column);
        return result;
    }

    public ImmutableCollection<Column> columnList() {
        return columns.values();
    }

    public boolean basedOnInterface() {
        boolean result = columnList()
                .stream()
                .allMatch(Column::isInterfaceBased);
        return result;
    }

    public boolean empty() {
        boolean result = columns.isEmpty();
        return result;
    }

    public Map<Column, Object> valuesIn(Entity<?, ?> source) {
        return valuesIn(source, identity());
    }

    /**
     * A convenience method that returns column values by {@code C}.
     */
    public <C> Map<C, Object> valuesIn(Entity<?, ?> source, Function<Column, C> columnMapper) {
        checkNotNull(source);
        Map<C, Object> result = new HashMap<>();
        for (Column column : columns.values()) {
            result.put(columnMapper.apply(column), column.valueIn(source));
        }
        return result;
    }

    public Map<Column, Object> valuesFromInterface(Entity<?, ?> source) {
        return valuesFromInterface(source, identity());
    }

    public <C> Map<C, Object> valuesFromInterface(Entity<?, ?> source,
                                                  Function<Column, C> columnMapper) {
        Map<C, Object> result = new HashMap<>();
        for (Column column : columns.values()) {
            result.put(columnMapper.apply(column), column.valueFromInterface(source));
        }
        return result;
    }

    public Columns protoColumns() {
        ImmutableMap.Builder<ColumnName, Column> protoColumns = ImmutableMap.builder();
        columns.values()
               .stream()
               .filter(Column::isProtoColumn)
               .forEach(column -> protoColumns.put(column.name(), column));
        return new Columns(protoColumns.build(), entityClass);
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
        throw newIllegalArgumentException(
                "A column with name '%s' not found in entity state class `%s`.",
                columnName, entityClass.stateClass()
                                       .getCanonicalName());
    }
}
