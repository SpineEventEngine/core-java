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
import io.spine.annotation.SPI;
import io.spine.base.Identifier;
import io.spine.query.ColumnName;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.WithLifecycle;
import io.spine.server.storage.RecordWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyMap;

/**
 * A value of {@link EntityRecord} associated with the values
 * of its {@linkplain io.spine.query.Column columns}.
 *
 * @param <I>
 *         the type of entity identifiers
 */
@SPI
public final class EntityRecordWithColumns<I>
        extends RecordWithColumns<I, EntityRecord> implements WithLifecycle {

    private EntityRecordWithColumns(I id, EntityRecord record, Map<ColumnName, Object> columns) {
        super(id, record, columns);
    }

    // TODO:alex.tymchenko:2023-10-27: kill!
//    /**
//     * Creates the new instance of {@code EntityRecordWithColumns} by evaluating the values
//     * of the passed columns for the passed entity.
//     *
//     * @param record
//     *         the record prepared for storage
//     * @param <I>
//     *         the type of the entity identifiers
//     * @return a new instance of {@code EntityRecordWithColumns}
//     */
//    public static <I, S extends EntityState<I>, E extends Entity<I, S>>
//    EntityRecordWithColumns<I> create(E entity, EntityRecord record) {
//        checkNotNull(entity);
//        checkNotNull(record);
//        var recordSpec = EntityRecordSpec.of(entity);
//        var storageFields = recordSpec.valuesIn(entity);
//        return new EntityRecordWithColumns<>(entity.id(), record, storageFields);
//    }

    /**
     * Creates the new instance of {@code EntityRecordWithColumns} using the pre-created
     * entity record and the entity identifier.
     *
     * <p>This method considers only the values of the
     * {@linkplain EntityRecordColumn lifecycle columns}.
     *
     * @param id
     *         the identifier of the entity
     * @param record
     *         the record to store; it is also used as a source for the lifecycle column values
     * @param <I>
     *         the type of the identifiers
     * @return a new instance of {@code EntityRecordWithColumns}
     */
    public static <I> EntityRecordWithColumns<I> create(I id, EntityRecord record) {
        checkNotNull(id);
        checkNotNull(record);
        var lifecycleValues = EntityRecordColumn.valuesIn(record);
        return new EntityRecordWithColumns<>(id, record, lifecycleValues);
    }

//    /**
//     * Creates a new instance of {@code EntityRecordWithColumns} using the pre-created
//     * entity record and the entity class.
//     *
//     * <p>This method considers both lifecycle and state-based columns.
//     *
//     * <p>It is a responsibility of the caller to provide a record with the matching
//     * identifier and state types.
//     *
//     * @param record
//     *         the record prepared for storage
//     * @param entityClass
//     *         the class of entity which is stored as the given {@code EntityRecord}
//     * @param <I>
//     *         the type of the entity's identifier
//     * @param <S>
//     *         the type of the entity's state
//     * @param <E>
//     *         the type of the entity
//     */
//    @SuppressWarnings("unchecked") // see the docs.
//    public static <I, S extends EntityState<I>, E extends Entity<I, S>> EntityRecordWithColumns<I>
//    create(EntityRecord record, Class<E> entityClass) {
//        checkNotNull(record);
//        checkNotNull(entityClass);
//
//        var stateValues = stateColumnsFrom(record, entityClass);
//        var lifecycleValues = EntityRecordColumn.valuesIn(record);
//        var allColumnValues = merge(stateValues, lifecycleValues);
//        var result = (EntityRecordWithColumns<I>) of(record, allColumnValues);
//        return result;
//    }

    // TODO:alex.tymchenko:2023-10-27: kill?
//    @SuppressWarnings("unchecked") /* See the docs for `create(record, entityClass)`. */
//    private static <I, S extends EntityState<I>, E extends Entity<I, S>>
//    Map<ColumnName, @Nullable Object> stateColumnsFrom(EntityRecord record, Class<E> entityClass) {
//        // TODO:alex.tymchenko:2023-10-27: reuse this expression?
//        var entityClazz = EntityClass.asParameterizedEntityClass(entityClass);
//        var columnsScanner = new Scanner<>(entityClazz);
//        var entityState = (S) AnyPacker.unpack(record.getState());
//
//        var stateValues = columnsScanner.stateColumns().valuesIn(entityState);
//        return stateValues;
//    }

    private static Map<ColumnName, Object> merge(Map<ColumnName, @Nullable Object> stateValues,
                                                 Map<ColumnName, Object> lifecycleValues) {
        Map<ColumnName, Object> allColumnValues = new HashMap<>();
        allColumnValues.putAll(stateValues);
        allColumnValues.putAll(lifecycleValues);
        return allColumnValues;
    }

    /**
     * Wraps a passed entity record into a {@code EntityWithColumns} with no storage fields.
     *
     * <p>This is a shortcut for {@link #of(EntityRecord, Map) of(EntityRecord, Map)} with
     * an empty {@code Map} of storage fields.
     *
     * @see #of(EntityRecord, Map) for the notes on usage
     */
    @VisibleForTesting
    public static <I> EntityRecordWithColumns<I> of(EntityRecord record) {
        return of(record, emptyMap());
    }

    /**
     * Creates a new instance from the passed record and storage fields.
     *
     * @apiNote This test-only method unpacks the identifier of the passed record and casts
     *         it to the type {@code I}. It is a responsibility of the caller to provide the record
     *         with the matching identifier.
     */
    @VisibleForTesting
    public static <I> EntityRecordWithColumns<I>
    of(EntityRecord record, Map<ColumnName, Object> storageFields) {
        I id = extractId(record);
        return new EntityRecordWithColumns<>(id, record, storageFields);
    }

    /**
     * Extracts the identifier from the passed record and casts it to the type {@code I}.
     *
     * <p>It is a responsibility of the caller to provide a record with the matching identifier.
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})    // see the docs.
    private static <I> I extractId(EntityRecord record) {
        return (I) Identifier.unpack(record.getEntityId());
    }

    @Override
    public LifecycleFlags getLifecycleFlags() {
        return record().getLifecycleFlags();
    }

    @Override
    public boolean isArchived() {
        return record().isArchived();
    }

    @Override
    public boolean isDeleted() {
        return record().isDeleted();
    }

    @Override
    public boolean isActive() {
        return record().isActive();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var other = (EntityRecordWithColumns<?>) o;

        return Objects.equals(id(), other.id()) &&
                Objects.equals(record(), other.record()) &&
                Objects.equals(storageFields(), other.storageFields());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id(), record(), storageFields());
    }
}
