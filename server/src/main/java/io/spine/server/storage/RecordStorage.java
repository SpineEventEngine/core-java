/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.FieldMasks;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.Columns;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.stand.AggregateStateId;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A storage keeping messages with identity.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 * @author Dmytro Grankin
 * @author Dmytro Dashenkov
 */
public abstract class RecordStorage<I>
        extends AbstractStorage<I, EntityRecord, RecordReadRequest<I>>
        implements StorageWithLifecycleFlags<I, EntityRecord, RecordReadRequest<I>>,
                   BulkStorageOperationsMixin<I, EntityRecord> {

    /**
     * The cache for entity columns.
     *
     * <p>Is {@code null} for instances that do not support entity columns.
     * @see RecordStorage(boolean)
     */
    private final @MonotonicNonNull EntityColumnCache entityColumnCache;

    /**
     * Creates an instance of {@code RecordStorage} which does not support
     * the {@link EntityColumnCache}.
     *
     * <p>This creation method should only be used for the {@code RecordStorage} descendants,
     * that are containers for another {@code RecordStorage} instance, which actually supports
     * {@link EntityColumnCache}, for example, a {@link ProjectionStorage}.
     *
     * <p>Instances created by this constructor should override
     * {@link RecordStorage#entityColumnCache()} method.
     */
    protected RecordStorage(boolean multitenant) {
        super(multitenant);
        this.entityColumnCache = null;
    }

    /**
     * Creates an instance of {@link RecordStorage} which supports the {@link EntityColumnCache}.
     */
    protected RecordStorage(boolean multitenant, Class<? extends Entity> entityClass) {
        super(multitenant);
        this.entityColumnCache = EntityColumnCache.initializeFor(entityClass);
    }

    /**
     * Reads a record, which matches the specified {@linkplain RecordReadRequest request}.
     *
     * @param  request the request to read the record
     * @return a record instance or {@code Optional.empty()} if there is no record with this ID
     */
    @Override
    public Optional<EntityRecord> read(RecordReadRequest<I> request) {
        checkNotClosed();
        checkNotNull(request);

        Optional<EntityRecord> record = readRecord(request.getRecordId());
        return record;
    }

    /**
     * Reads a record, which matches the specified {@linkplain RecordReadRequest request}
     * and applies a {@link FieldMask} to it.
     *
     * @param  request   the request to read the record
     * @param  fieldMask fields to read.
     * @return the item with the given ID and with the {@code FieldMask} applied
     *         or {@code Optional.empty()} if there is no record matching this request
     * @see    #read(RecordReadRequest)
     */
    @SuppressWarnings("CheckReturnValue") // calling builder method
    public Optional<EntityRecord> read(RecordReadRequest<I> request, FieldMask fieldMask) {
        Optional<EntityRecord> rawResult = read(request);

        if (!rawResult.isPresent()) {
            return Optional.empty();
        }

        EntityRecord.Builder builder = EntityRecord.newBuilder(rawResult.get());
        Any state = builder.getState();
        TypeUrl type = TypeUrl.parse(state.getTypeUrl());
        Message stateAsMessage = AnyPacker.unpack(state);

        Message maskedState = FieldMasks.applyMask(fieldMask, stateAsMessage, type);

        Any packedState = AnyPacker.pack(maskedState);
        builder.setState(packedState);
        return Optional.of(builder.build());
    }

    /**
     * Writes a record and its {@linkplain EntityColumn entity columns} into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param  id     the ID for the record
     * @param  record a record to store
     * @throws IllegalStateException if the storage is closed
     * @see   #write(Object, EntityRecord)
     */
    public void write(I id, EntityRecordWithColumns record) {
        checkNotNull(id);
        checkArgument(record.getRecord()
                            .hasState(), "Record does not have state field.");
        checkNotClosed();

        writeRecord(id, record);
    }

    @Override
    public void write(I id, EntityRecord record) {
        EntityRecordWithColumns recordWithStorageFields = EntityRecordWithColumns.of(record);
        write(id, recordWithStorageFields);
    }

    /**
     * Writes a bulk of records into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param  records an ID to record map with the entries to store
     * @throws IllegalStateException if the storage is closed
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
            // The AggregateStateId is a special case, which is not handled by the Identifier class.
            String idStr = id instanceof AggregateStateId
                              ? id.toString()
                              : Identifier.toString(id);
            throw newIllegalStateException("Unable to load record for entity with ID: %s", idStr);
        }
    }

    /**
     * Deletes the record with the passed ID.
     *
     * @param id the record to delete
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public abstract boolean delete(I id);

    @Override
    public Iterator<EntityRecord> readMultiple(Iterable<I> ids) {
        checkNotClosed();
        checkNotNull(ids);

        return readMultipleRecords(ids);
    }

    /**
     * Reads multiple items from the storage and apply {@link FieldMask} to each of the results.
     *
     * @param ids       the IDs of the items to read
     * @param fieldMask the mask to apply
     * @return the items with the given IDs and with the given {@code FieldMask} applied
     */
    public Iterator<EntityRecord> readMultiple(Iterable<I> ids, FieldMask fieldMask) {
        checkNotClosed();
        checkNotNull(ids);

        return readMultipleRecords(ids, fieldMask);
    }

    @Override
    public Iterator<EntityRecord> readAll() {
        checkNotClosed();

        return readAllRecords();
    }

    /**
     * Reads all items from the storage and apply {@link FieldMask} to each of the results.
     *
     * @param fieldMask the {@code FieldMask} to apply
     * @return all items from this repository with the given {@code FieldMask} applied
     */
    public Iterator<EntityRecord> readAll(FieldMask fieldMask) {
        checkNotClosed();

        return readAllRecords(fieldMask);
    }

    /**
     * Reads all the records matching the given {@link EntityQuery} and applies the given
     * {@link FieldMask} to the resulting record states.
     *
     * <p>By default, if the query does not specify the {@linkplain LifecycleFlags}, but the entity
     * supports them, all the resulting records are active. Otherwise the records obey
     * the constraints provided by the query.
     *
     * @param  query     the query to execute
     * @param  fieldMask the fields to retrieve
     * @return the matching records mapped upon their IDs
     */
    public Iterator<EntityRecord> readAll(EntityQuery<I> query, FieldMask fieldMask) {
        checkNotClosed();
        checkNotNull(query);
        checkNotNull(fieldMask);

        return readAllRecords(query, fieldMask);
    }

    /**
     * Reads all the records matching the given {@link EntityQuery} and applies the given
     * {@link FieldMask} to the resulting record states.
     *
     * <p>By default, if the query does not specify the {@linkplain LifecycleFlags}, but the entity
     * supports them, all the resulting records are active. Otherwise the records obey
     * the constraints provided by the query.
     *
     * @param  query     the query to execute
     * @return the matching records mapped upon their IDs
     */
    public Iterator<EntityRecord> readAll(EntityQuery<I> query) {
        return readAll(query, FieldMask.getDefaultInstance());
    }

    /**
     * Returns a {@code Collection} of {@linkplain Column columns} of the {@link Entity} managed
     * by this storage.
     *
     * @return a {@code Collection} of managed {@link Entity} columns
     * @see EntityColumn
     * @see Columns
     */
    @Internal
    public Collection<EntityColumn> entityColumns() {
        return entityColumnCache().getColumns();
    }

    /**
     * Returns a {@code Map} of {@linkplain EntityColumn columns} corresponded to the
     * {@link LifecycleFlagField lifecycle storage fields} of the {@link Entity} class managed
     * by this storage.
     *
     * @return a {@code Map} of managed {@link Entity} lifecycle columns
     * @throws IllegalArgumentException if a lifecycle field is not present
     *         in the managed {@link Entity} class
     * @see EntityColumn
     * @see Columns
     * @see LifecycleFlagField
     */
    @Internal
    public Map<String, EntityColumn> entityLifecycleColumns() {
        Map<String, EntityColumn> lifecycleColumns = new HashMap<>();
        for (LifecycleFlagField field : LifecycleFlagField.values()) {
            String name = field.name();
            EntityColumn column = entityColumnCache().findColumn(name);
            lifecycleColumns.put(name, column);
        }
        return lifecycleColumns;
    }

    /**
     * Obtains the entity column cache.
     *
     * @throws IllegalStateException if the storage {@linkplain RecordStorage(boolean)
     * does not support} the cache
     */
    @Internal
    public EntityColumnCache entityColumnCache() {
        if (entityColumnCache == null) {
            throw newIllegalStateException(
                    "Entity column cache is not initialized for the storage %s.",
                    this
            );
        }
        return entityColumnCache;
    }

    /*
     * Internal storage methods
     *****************************/

    /**
     * Reads a record from the storage by the passed ID.
     *
     * @param id the ID of the record to load
     * @return a record instance or {@code null} if there is no record with this ID
     */
    protected abstract Optional<EntityRecord> readRecord(I id);

    /** @see BulkStorageOperationsMixin#readMultiple(java.lang.Iterable) */
    protected abstract Iterator<EntityRecord> readMultipleRecords(Iterable<I> ids);

    /** @see BulkStorageOperationsMixin#readMultiple(java.lang.Iterable) */
    protected abstract
    Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<I> ids, FieldMask fieldMask);

    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Iterator<EntityRecord> readAllRecords();

    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Iterator<EntityRecord> readAllRecords(FieldMask fieldMask);

    /**
     * @see #readAll(EntityQuery, FieldMask)
     */
    protected abstract
    Iterator<EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask);

    /**
     * Writes a record and the associated {@link EntityColumn} values into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id     an ID of the record
     * @param record a record to store
     */
    protected abstract void writeRecord(I id, EntityRecordWithColumns record);

    /**
     * Writes a bulk of records into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param records an ID to record map with the entries to store
     */
    protected abstract void writeRecords(Map<I, EntityRecordWithColumns> records);
}
