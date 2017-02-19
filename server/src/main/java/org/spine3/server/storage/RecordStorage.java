/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.SPI;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.FieldMasks;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A storage keeping messages with identity.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class RecordStorage<I> extends AbstractStorage<I, EntityRecord>
        implements BulkStorageOperationsMixin<I, EntityRecord> {

    protected RecordStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<EntityRecord> read(I id) {
        checkNotClosed();
        checkNotNull(id);

        final Optional<EntityRecord> record = readRecord(id);
        return record;
    }

    /**
     * Reads a single item from the storage and applies a {@link FieldMask} to it.
     *
     * @param id        ID of the item to read.
     * @param fieldMask fields to read.
     * @return the item with the given ID and with the {@code FieldMask} applied.
     * @see #read(Object)
     */
    public Optional<EntityRecord> read(I id, FieldMask fieldMask) {
        final Optional<EntityRecord> rawResult = read(id);

        if (!rawResult.isPresent()) {
            return Optional.absent();
        }

        final EntityRecord.Builder builder = EntityRecord.newBuilder(rawResult.get());
        final Any state = builder.getState();
        final TypeUrl type = TypeUrl.of(state.getTypeUrl());
        final Message stateAsMessage = AnyPacker.unpack(state);

        final Message maskedState = FieldMasks.applyMask(fieldMask, stateAsMessage, type);

        final Any packedState = AnyPacker.pack(maskedState);
        builder.setState(packedState);
        return Optional.of(builder.build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(I id, EntityRecord record) {
        checkNotNull(id);
        checkArgument(record.hasState(), "Record does not have state field.");
        checkNotClosed();

        writeRecord(id, record);
    }

    /**
     * Writes a bulk of records into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param records an ID to record map with the entries to store
     * @throws IllegalStateException if the storage is closed
     */
    public void write(Map<I, EntityRecord> records) {
        checkNotNull(records);
        checkNotClosed();

        writeRecords(records);
    }

    /**
     * Marks the record with the passed ID as {@code archived}.
     *
     * @param id the ID of the record to mark
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public abstract boolean markArchived(I id);

    /**
     * Marks the record with the passed ID as {@code deleted}.
     *
     * <p>This method does not delete the record.
     * To delete the record please call {@link #delete(Object)}
     *
     * @param id the ID of the record to mark
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public abstract boolean markDeleted(I id);

    /**
     * Deletes the record with the passed ID.
     *
     * @param id the record to delete
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public abstract boolean delete(I id);

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<EntityRecord> readMultiple(Iterable<I> ids) {
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
    public Iterable<EntityRecord> readMultiple(Iterable<I> ids, FieldMask fieldMask) {
        checkNotClosed();
        checkNotNull(ids);

        return readMultipleRecords(ids, fieldMask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<I, EntityRecord> readAll() {
        checkNotClosed();

        return readAllRecords();
    }

    /**
     * Reads all items from the storage and apply {@link FieldMask} to each of the results.
     *
     * @param fieldMask the {@code FieldMask} to apply
     * @return all items from this repository with the given {@code FieldMask} applied
     */
    public Map<I, EntityRecord> readAll(FieldMask fieldMask) {
        checkNotClosed();

        return readAllRecords(fieldMask);
    }

    //
    // Internal storage methods
    //---------------------------

    /**
     * Reads a record from the storage by the passed ID.
     *
     * @param id the ID of the record to load
     * @return a record instance or {@code null} if there is no record with this ID
     */
    protected abstract Optional<EntityRecord> readRecord(I id);

    /** @see BulkStorageOperationsMixin#readMultiple(java.lang.Iterable) */
    protected abstract Iterable<EntityRecord> readMultipleRecords(Iterable<I> ids);

    /** @see BulkStorageOperationsMixin#readMultiple(java.lang.Iterable) */
    protected abstract Iterable<EntityRecord> readMultipleRecords(Iterable<I> ids, FieldMask fieldMask);

    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Map<I, EntityRecord> readAllRecords();

    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Map<I, EntityRecord> readAllRecords(FieldMask fieldMask);

    /**
     * Writes a record into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id     an ID of the record
     * @param record a record to store
     */
    protected abstract void writeRecord(I id, EntityRecord record);

    /**
     * Writes a bulk of records into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param records an ID to record map with the entries to store
     */
    protected abstract void writeRecords(Map<I, EntityRecord> records);
}
