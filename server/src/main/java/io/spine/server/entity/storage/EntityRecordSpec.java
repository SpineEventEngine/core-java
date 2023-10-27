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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.RecordSpec;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableMap;

/**
 * Instructs the storage on how to handle {@link EntityRecord}s storing the information about
 * {@link Entity} instances.
 *
 * <p>Lists the columns defined for the {@code Entity}, including the columns defined
 * in the Protobuf message of the {@code Entity} state, and system columns.
 *
 * <p>In order to describe the specification of a plain Protobuf message stored,
 * see {@link io.spine.server.storage.MessageRecordSpec MessageRecordSpec}.
 *
 * @param <I>
 *         the type of the entity identifiers
 * @param <E>
 *         the type of entities
 * @param <S>
 *         the type of the entity states
 */
@Immutable
@Internal
public final class EntityRecordSpec<I, S extends EntityState<I>, E extends Entity<I, S>>
        extends RecordSpec<I, EntityRecord, E> {

    /**
     * The class of {@code Entity} which storage is configured.
     */
    private final EntityClass<E> entityClass;

    private final EntityColumns<E> columns;

    private EntityRecordSpec(EntityClass<E> entityClass, EntityColumns<E> columns) {
        super(idClass(entityClass), EntityRecord.class);
        this.entityClass = entityClass;
        this.columns = columns;
    }

    // TODO:alex.tymchenko:2023-10-27: kill?
//    /**
//     * Gathers columns declared by the entity class.
//     *
//     * @param entityClass
//     *         the class of the entity
//     * @param <I>
//     *         the type of the entity identifiers
//     * @param <E>
//     *         the type of the entity
//     * @param <S>
//     *         the type of the entity state
//     */
//    public static <I, S extends EntityState<I>, E extends Entity<I, S>>
//    EntityRecordSpec<I, S, E> of(EntityClass<E> entityClass) {
//        checkNotNull(entityClass);
//        var scanner = new Scanner<>(entityClass);
//        return new EntityRecordSpec<>(entityClass, scanner.columns());
//    }

    // TODO:alex.tymchenko:2023-10-27: kill both!
//    /**
//     * Gathers columns declared by the class of the passed entity.
//     *
//     * @param entity
//     *         the entity instance
//     * @param <I>
//     *         the type of the entity identifiers
//     * @param <E>
//     *         the type of the entity
//     * @param <S>
//     *         the type of the entity state
//     */
//    public static <I, S extends EntityState<I>, E extends Entity<I, S>>
//    EntityRecordSpec<I, S, E> of(E entity) {
//        checkNotNull(entity);
//        @SuppressWarnings("unchecked")  // Ensured by the entity type declaration.
//        var modelClass = (EntityClass<E>) entity.modelClass();
//        var scanner = new Scanner<>(modelClass);
//        return new EntityRecordSpec<>(modelClass, scanner.columns());
//    }
//
//    /**
//     * Gathers columns of the entity class.
//     */
//    public static <I, S extends EntityState<I>, E extends Entity<I, S>>
//    EntityRecordSpec<I, S, E> of(Class<E> cls) {
//        var aClass = asParameterizedEntityClass(cls);
//        return of(aClass);
//    }

    /**
     * Extracts column values from the entity.
     *
     * <p>The Protobuf-based columns are extracted from the entity state while the system columns
     * are obtained from the entity itself via the corresponding getters.
     *
     * @apiNote This method returns an unmodifiable version of a {@code Map}.
     *         An {@link com.google.common.collect.ImmutableMap ImmutableMap} is not used, as long
     *         as it prohibits the {@code null} values.
     */
    @Override
    public Map<ColumnName, @Nullable Object> valuesIn(E entity) {
        checkNotNull(entity);
        Map<ColumnName, @Nullable Object> result = new HashMap<>();
        columns.forEach(
                column -> result.put(column.name(), column.valueIn(entity))
        );
        return unmodifiableMap(result);
    }

    @Override
    public I idValueIn(E source) {
        return source.id();
    }

    /**
     * Extracts the entity identifier value from the passed {@code EntityRecord}.
     *
     * <p>Callers are responsible for passing the {@code EntityRecord} instances compatible
     * with the contract of this record specification. In case the type of the identifier
     * packed into the passed entity record is not {@code I},
     * a {@code ClassCastException} is thrown.
     *
     * @param record
     *         the source for extraction
     * @return the value of the entity identifier
     */
    @Override
    @SuppressWarnings("unchecked")      /* See the documentation. */
    public I idFromRecord(EntityRecord record) {
        var packed = record.getEntityId();
        return (I) Identifier.unpack(packed);
    }

    @Override
    public Optional<Column<?, ?>> findColumn(ColumnName name) {
        checkNotNull(name);
        for (Column<?, ?> column : columns) {
            if(column.name().equals(name)) {
                return Optional.of(column);
            }
        }
        return Optional.empty();
    }

    @Override
    public ImmutableSet<Column<?, ?>> columns() {
        var typed = columns.values();
        ImmutableSet.Builder<Column<?, ?>> builder = ImmutableSet.builder();
        var result = builder.addAll(typed).build();
        return result;
    }

    /**
     * Returns the value of the entity class, which record spec this is.
     */
    public EntityClass<E> entityClass() {
        return entityClass;
    }

    @Override
    public Class<? extends Message> sourceType() {
        return entityClass.stateClass();
    }

    /**
     * Returns the total number of columns in this specification.
     */
    @VisibleForTesting
    int columnCount() {
        return columns.size();
    }

    @SuppressWarnings("unchecked")  // Ensured by the `Entity` declaration.
    private static <I, E extends Entity<I, ?>> Class<I> idClass(EntityClass<E> entityClass) {
        return (Class<I>) entityClass.idClass();
    }
}
