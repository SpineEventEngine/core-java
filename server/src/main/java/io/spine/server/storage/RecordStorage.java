/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.ResponseFormat;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.FieldMasks;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.Columns;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A storage keeping messages with identity.
 *
 * @param <I>
 *         the type of entity IDs
 */
public abstract class RecordStorage<I>
        extends AbstractStorage<I, EntityRecord, RecordReadRequest<I>>
        implements StorageWithLifecycleFlags<I, EntityRecord, RecordReadRequest<I>>,
                   BulkStorageOperationsMixin<I, EntityRecord> {

    /**
     * The class of entities stored in this {@code RecordStorage}.
     */
    private final EntityClass<?> entityClass;

    /**
     * Creates an instance of {@code RecordStorage}.
     */
    protected RecordStorage(Class<? extends Entity<?, ?>> entityClass, boolean multitenant) {
        super(multitenant);
        this.entityClass = asEntityClass(entityClass);
    }

    /**
     * Reads a record which matches the specified {@linkplain RecordReadRequest request}.
     *
     * @param request
     *         the request to read the record
     * @return a record instance or {@code Optional.empty()} if there is no record with this ID
     */
    @Override
    public Optional<EntityRecord> read(RecordReadRequest<I> request) {
        checkNotClosed();
        checkNotNull(request);

        Optional<EntityRecord> record = readRecord(request.recordId());
        return record;
    }

    /**
     * Reads a record which matches the specified {@linkplain RecordReadRequest request}
     * and applies a {@link FieldMask} to it.
     *
     * @param request
     *         the request to read the record
     * @param fieldMask
     *         fields to read.
     * @return the item with the given ID and with the {@code FieldMask} applied
     *         or {@code Optional.empty()} if there is no record matching this request
     * @see #read(RecordReadRequest)
     */
    @SuppressWarnings("CheckReturnValue") // calling builder method
    public Optional<EntityRecord> read(RecordReadRequest<I> request, FieldMask fieldMask) {
        Optional<EntityRecord> rawResult = read(request);

        if (!rawResult.isPresent()) {
            return Optional.empty();
        }

        EntityRecord.Builder builder = EntityRecord.newBuilder(rawResult.get());
        Any state = builder.getState();
        Message stateAsMessage = AnyPacker.unpack(state);

        Message maskedState = FieldMasks.applyMask(fieldMask, stateAsMessage);

        Any packedState = AnyPacker.pack(maskedState);
        builder.setState(packedState);
        return Optional.of(builder.build());
    }

    /**
     * Writes a record and its {@linkplain io.spine.server.entity.storage.Column columns} into the
     * storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id
     *         the ID for the record
     * @param record
     *         the record to store
     * @throws IllegalStateException
     *         if the storage is closed
     * @see #write(Object, EntityRecord)
     */
    public void write(I id, EntityRecordWithColumns record) {
        checkNotNull(id);
        checkArgument(record.record()
                            .hasState(), "Record does not have state field.");
        checkNotClosed();

        writeRecord(id, record);
    }

    @Override
    public void write(I id, EntityRecord record) {
        EntityRecordWithColumns recordWithStorageFields =
                EntityRecordWithColumns.of(record);
        write(id, recordWithStorageFields);
    }

    /**
     * Writes a bulk of records into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param records
     *         the ID to record map with the entries to store
     * @throws IllegalStateException
     *         if the storage is closed
     */
    public void write(Map<I, EntityRecordWithColumns> records) {
        checkNotNull(records);
        checkNotClosed();

        writeRecords(records);
    }

    @Override
    public Optional<LifecycleFlags> readLifecycleFlags(I id) {
        RecordReadRequest<I> request = new RecordReadRequest<>(id);
        Optional<EntityRecord> optional = read(request);
        return optional.map(EntityRecord::getLifecycleFlags);
    }

    @Override
    public void writeLifecycleFlags(I id, LifecycleFlags flags) {
        RecordReadRequest<I> request = new RecordReadRequest<>(id);
        Optional<EntityRecord> optional = read(request);
        if (optional.isPresent()) {
            EntityRecord record = optional.get();
            EntityRecord updated = record.toBuilder()
                                         .setLifecycleFlags(flags)
                                         .build();
            write(id, updated);
        } else {
            String idStr = Identifier.toString(id);
            throw newIllegalStateException("Unable to load record for entity with ID: %s", idStr);
        }
    }

    /**
     * Deletes the record with the passed ID.
     *
     * @param id
     *         the record to delete
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public abstract boolean delete(I id);

    /**
     * Reads multiple active items from the storage and applies {@link FieldMask} to the results.
     *
     * <p>The size of the returned {@code Iterator} matches the size of the given {@code ids},
     * with nulls in place of missing or inactive entities.
     *
     * @param ids
     *         the IDs of the items to read
     * @param fieldMask
     *         the mask to apply
     * @return the items with the given IDs and with the given {@code FieldMask} applied
     */
    @Override
    public Iterator<@Nullable EntityRecord> readMultiple(Iterable<I> ids, FieldMask fieldMask) {
        checkNotClosed();
        checkNotNull(ids);

        return readMultipleRecords(ids, fieldMask);
    }

    /**
     * Reads all active items from the storage and apply {@link FieldMask} to each of the results.
     *
     * @param format
     *         the expected format of the response
     * @return all items from this repository with the given {@code FieldMask} applied
     */
    @Override
    public Iterator<EntityRecord> readAll(ResponseFormat format) {
        checkNotClosed();

        return readAllRecords(format);
    }

    /**
     * Reads all the records matching the given {@link EntityQuery} and applies the given
     * {@link FieldMask} to the resulting record states.
     *
     * <p>By default, the entities supporting lifecycle will be returned only if they are active.
     * To get inactive entities, the lifecycle attribute must be set to the
     * {@linkplain EntityQuery provided query}.
     *
     * @param query
     *         the query to execute
     * @param format
     *         the format of the query response
     * @return the matching records mapped upon their IDs
     */
    public Iterator<EntityRecord> readAll(EntityQuery<I> query, ResponseFormat format) {
        checkNotClosed();
        checkNotNull(query);
        checkNotNull(format);

        return readAllRecords(query, format);
    }

    /**
     * Reads all the records matching the given {@link EntityQuery} and applies the given
     * {@link FieldMask} to the resulting record states.
     *
     * <p>By default, if the query does not specify the {@linkplain LifecycleFlags} but the entity
     * supports them, all the resulting records are active. Otherwise the records obey
     * the constraints provided by the query.
     *
     * @param query
     *         the query to execute
     * @return the matching records mapped upon their IDs
     */
    public Iterator<EntityRecord> readAll(EntityQuery<I> query) {
        return readAll(query, ResponseFormat.getDefaultInstance());
    }

    /**
     * Obtains a list of columns of the managed {@link Entity}.
     *
     * @see io.spine.server.entity.storage.Column
     * @see io.spine.code.proto.ColumnOption
     */
    protected final ImmutableList<Column> columnList() {
        return columns().columnList();
    }

    /**
     * Returns the columns of the managed {@link Entity}.
     *
     * @see io.spine.server.entity.storage.Column
     * @see io.spine.code.proto.ColumnOption
     */
    @Internal
    public Columns columns() {
        return entityClass.columns();
    }

    /**
     * Returns a {@code Map} of {@linkplain io.spine.server.entity.storage.Column columns}
     * corresponded to the {@link LifecycleFlagField lifecycle storage fields} of the
     * {@link Entity} class managed by this storage.
     *
     * @return a {@code Map} of managed {@link Entity} lifecycle columns
     * @throws IllegalArgumentException
     *         if a lifecycle field is not present
     *         in the managed {@link Entity} class
     * @see LifecycleFlagField
     */
    @Internal
    public final ImmutableMap<ColumnName, Column> lifecycleColumns() {
        return columns().lifecycleColumns();
    }

    /*
     * Internal storage methods
     *****************************/

    /**
     * Reads a record from the storage by the passed ID.
     *
     * @param id
     *         the ID of the record to load
     * @return a record instance or {@code null} if there is no record with this ID
     */
    protected abstract Optional<EntityRecord> readRecord(I id);

    /**
     * Obtains an iterator for reading multiple records by IDs, and
     * applying the passed field mask to the results.
     *
     * <p>The size of the returned {@code Iterator} matches the size of the given {@code ids},
     * with nulls in place of missing or inactive entities.
     *
     * @see BulkStorageOperationsMixin#readMultiple
     */
    protected abstract Iterator<@Nullable EntityRecord>
    readMultipleRecords(Iterable<I> ids, FieldMask fieldMask);

    /**
     * Obtains an iterator for reading all records.
     *
     * <p>Only active entities are returned.
     *
     * @param format
     *         the expected format of the query response
     * @see BulkStorageOperationsMixin#readAll
     */
    protected abstract Iterator<EntityRecord> readAllRecords(ResponseFormat format);

    /**
     * Obtains an iterator for reading records matching the query,
     * and applying the passed field mask to the results.
     *
     * <p>Returns only active entities if the query does not specify the {@linkplain LifecycleFlags
     * lifecycle flags}. In order to read inactive entities, the corresponding filters must be set
     * to the provided {@link EntityQuery query}.
     *
     * @see #readAll(EntityQuery, ResponseFormat)
     */
    protected abstract Iterator<EntityRecord>
    readAllRecords(EntityQuery<I> query, ResponseFormat format);

    /**
     * Writes a record and the associated {@linkplain io.spine.server.entity.storage.Column column}
     * values into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id
     *         an ID of the record
     * @param record
     *         a record to store
     */
    protected abstract void writeRecord(I id, EntityRecordWithColumns record);

    /**
     * Writes a bulk of records into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param records
     *         an ID to record map with the entries to store
     */
    protected abstract void writeRecords(Map<I, EntityRecordWithColumns> records);
}
