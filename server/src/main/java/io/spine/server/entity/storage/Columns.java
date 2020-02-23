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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.LifecycleFlagField;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * The collection of declared columns of the {@link Entity}.
 */
@Immutable
@Internal
public final class Columns {

    /**
     * The {@linkplain SystemColumn system columns} of the entity.
     */
    private final ImmutableMap<ColumnName, SysColumn> systemColumns;

    /**
     * The entity-state-based columns of the entity.
     */
    private final ImmutableMap<ColumnName, SimpleColumn> simpleColumns;

    /**
     * The interface-based columns of the entity.
     */
    private final ImmutableMap<ColumnName, InterfaceBasedColumn> interfaceBasedColumns;

    private final EntityClass<?> entityClass;

    private Columns(
            ImmutableMap<ColumnName, SysColumn> systemColumns,
            ImmutableMap<ColumnName, SimpleColumn> simpleColumns,
            ImmutableMap<ColumnName, InterfaceBasedColumn> interfaceBasedColumns,
            EntityClass<?> entityClass) {
        this.systemColumns = systemColumns;
        this.simpleColumns = simpleColumns;
        this.interfaceBasedColumns = interfaceBasedColumns;
        this.entityClass = entityClass;
    }

    /**
     * Gathers columns of the entity class.
     */
    public static Columns of(EntityClass<?> entityClass) {
        checkNotNull(entityClass);
        Scanner scanner = new Scanner(entityClass);
        return new Columns(scanner.systemColumns(),
                           scanner.simpleColumns(),
                           scanner.interfaceBasedColumns(),
                           entityClass);
    }

    /**
     * Gathers columns of the entity class.
     */
    public static Columns of(Class<? extends Entity<?, ?>> entityClass) {
        return of(asEntityClass(entityClass));
    }

    /**
     * Obtains a column by name.
     *
     * @throws IllegalArgumentException
     *         if the column with the specified name is not found
     */
    public Column get(ColumnName columnName) {
        checkNotNull(columnName);
        Column result = find(columnName).orElseThrow(() -> columnNotFound(columnName));
        return result;
    }

    /**
     * Searches for a column with a given name.
     */
    public Optional<Column> find(ColumnName columnName) {
        checkNotNull(columnName);
        Column column = systemColumns.get(columnName);
        if (column == null) {
            column = simpleColumns.get(columnName);
        }
        if (column == null) {
            column = interfaceBasedColumns.get(columnName);
        }
        return Optional.ofNullable(column);
    }

    /**
     * Extracts column values from the entity.
     *
     * <p>The {@linkplain ColumnDeclaredInProto proto-based} columns are extracted from the entity
     * state while the system columns are obtained from the entity itself via the corresponding
     * getters.
     *
     * @implNote This method assumes that the {@linkplain InterfaceBasedColumn interface-based}
     *         column values are already propagated to the entity state as they are finalized by
     *         the moment of transaction {@linkplain Transaction#commit() commit}. It thus extracts
     *         values from the entity state directly, avoiding any recalculation to prevent
     *         possible inconsistencies in the model as well as performance drops.
     */
    public Map<ColumnName, @Nullable Object> valuesIn(Entity<?, ?> source) {
        checkNotNull(source);
        Map<ColumnName, @Nullable Object> result = new HashMap<>();
        systemColumns.forEach(
                (name, column) -> result.put(name, column.valueIn(source))
        );
        simpleColumns.forEach(
                (name, column) -> result.put(name, column.valueIn(source.state()))
        );
        interfaceBasedColumns.forEach(
                (name, column) -> result.put(name, column.valueIn(source.state()))
        );
        return result;
    }

    /**
     * Returns all columns of the entity.
     */
    public ImmutableList<Column> columnList() {
        ImmutableList.Builder<Column> builder = ImmutableList.builder();
        builder.addAll(systemColumns.values());
        builder.addAll(simpleColumns.values());
        builder.addAll(interfaceBasedColumns.values());
        return builder.build();
    }

    /**
     * Returns a subset of columns corresponding to the lifecycle of the entity.
     */
    public ImmutableMap<ColumnName, Column> lifecycleColumns() {
        ImmutableMap.Builder<ColumnName, Column> result = ImmutableMap.builder();
        for (LifecycleFlagField field : LifecycleFlagField.values()) {
            ColumnName name = ColumnName.of(field.name());
            SysColumn column = systemColumns.get(name);
            if (column != null) {
                result.put(name, column);
            }
        }
        return result.build();
    }

    /**
     * Obtains {@linkplain InterfaceBasedColumn interface-based} columns of the entity.
     */
    public ImmutableMap<ColumnName, InterfaceBasedColumn> interfaceBasedColumns() {
        return interfaceBasedColumns;
    }

    private IllegalArgumentException columnNotFound(ColumnName columnName) {
        throw newIllegalArgumentException(
                "A column with name '%s' not found in entity state class `%s`.",
                columnName, entityClass.stateClass()
                                       .getCanonicalName());
    }
}
