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
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.CustomColumn;
import io.spine.query.EntityColumn;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.RecordSpec;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.model.EntityClass.asParameterizedEntityClass;

/**
 * Instructs the storage on how to handle {@link EntityRecord}s storing the information about
 * {@link Entity} instances.
 *
 * <p>Lists the columns defined for the {@code Entity}, including the columns defined
 * in the Protobuf message of the {@code Entity} state, and system columns.
 *
 * <p>In order to describe the specification of a plain Protobuf message stored,
 * see {@link io.spine.server.storage.MessageRecordSpec MessageRecordSpec}.
 */
@Immutable
@Internal
public final class EntityRecordSpec<I, S extends EntityState<I>, E extends Entity<I, S>>
        extends RecordSpec<I, EntityRecord, E> {

    /**
     * The class of {@code Entity} which storage is configured.
     */
    private final EntityClass<E> entityClass;

    /**
     * The {@linkplain SystemColumn system columns} of the entity.
     */
    private final ImmutableSet<CustomColumn<E, ?>> systemColumns;

    /**
     * The entity-state-based columns of the entity.
     */
    private final ImmutableSet<EntityColumn<S, ?>> simpleColumns;

    private EntityRecordSpec(EntityClass<E> entityClass,
                             ImmutableSet<EntityColumn<S, ?>> simpleColumns,
                             ImmutableSet<CustomColumn<E, ?>> systemColumns) {
        super(idClass(entityClass), EntityRecord.class);
        this.entityClass = entityClass;
        this.simpleColumns = simpleColumns;
        this.systemColumns = systemColumns;
    }

    /**
     * Gathers columns of the entity class.
     */
    public static <I, S extends EntityState<I>, E extends Entity<I, S>>
    EntityRecordSpec<I, S, E> of(EntityClass<E> entityClass) {
        checkNotNull(entityClass);
        Scanner<I, S, E> scan = new Scanner<>(entityClass);
        return new EntityRecordSpec<>(entityClass, scan.simpleColumns(), scan.systemColumns());
    }

    public static <I, S extends EntityState<I>, E extends Entity<I, S>>
    EntityRecordSpec<I, S, E> of(E entity) {
        checkNotNull(entity);
        @SuppressWarnings("unchecked")  // Ensured by the entity type declaration.
            EntityClass<E> modelClass = (EntityClass<E>) entity.modelClass();
        Scanner<I, S, E> scan = new Scanner<>(modelClass);
        return new EntityRecordSpec<>(modelClass, scan.simpleColumns(), scan.systemColumns());
    }

    /**
     * Gathers columns of the entity class.
     */
    public static <I, S extends EntityState<I>, E extends Entity<I, S>>  EntityRecordSpec<I, S, E>
    of(Class<E> cls) {
        EntityClass<E> aClass = asParameterizedEntityClass(cls);
        return of(aClass);
    }

    /**
     * Extracts column values from the entity.
     *
     * <p>The Protobuf-based columns are extracted from the entity state while the system columns
     * are obtained from the entity itself via the corresponding getters.
     */
    @Override
    public Map<ColumnName, @Nullable Object> valuesIn(E entity) {
        checkNotNull(entity);
        Map<ColumnName, @Nullable Object> result = new HashMap<>();
        systemColumns.forEach(
                column -> result.put(column.name(), column.valueIn(entity))
        );
        simpleColumns.forEach(
                column -> result.put(column.name(), column.valueIn(entity.state()))
        );
        return result;
    }

    @Override
    protected I idValueIn(E source) {
        return source.id();
    }

    @Override
    public Optional<Column<?, ?>> findColumn(ColumnName name) {
        checkNotNull(name);
        Column<?, ?> resultInSimple = lookForColumn(name, simpleColumns);
        if (resultInSimple != null) {
            return Optional.of(resultInSimple);
        }
        Column<?, ?> resultInSystem = lookForColumn(name, systemColumns);
        if (resultInSystem != null) {
            return Optional.of(resultInSystem);
        }
        return Optional.empty();
    }

    private static @Nullable Column<?, ?>
    lookForColumn(ColumnName name, Iterable<? extends Column<?, ?>> columns) {
        for (Column<?, ?> column : columns) {
            if(column.name().equals(name)) {
                return column;
            }
        }
        return null;
    }

    /**
     * Returns the value of the entity class, which record spec this is.
     */
    public EntityClass<E> entityClass() {
        return entityClass;
    }

    /**
     * Returns the total number of columns in this specification.
     */
    @VisibleForTesting
    int columnCount() {
        return systemColumns.size() + simpleColumns.size();
    }

    /**
     * Returns the discovered system columns.
     */
    @VisibleForTesting
    ImmutableSet<CustomColumn<E, ?>> systemColumns() {
        return systemColumns;
    }

    @SuppressWarnings("unchecked")  // Ensured by the `Entity` declaration.
    private static <I, E extends Entity<I, ?>> Class<I> idClass(EntityClass<E> entityClass) {
        return (Class<I>) entityClass.idClass();
    }
}
