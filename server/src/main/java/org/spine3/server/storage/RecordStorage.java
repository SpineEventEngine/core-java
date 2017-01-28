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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.SPI;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.FieldMasks;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A storage keeping messages with identity.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 * @see Entity
 */
@SPI
public abstract class RecordStorage<I> extends AbstractStorage<I, EntityStorageRecord>
        implements BulkStorageOperationsMixin<I, EntityStorageRecord> {

    protected RecordStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityStorageRecord read(I id) {
        checkNotClosed();
        checkNotNull(id);

        final EntityStorageRecord record = readRecord(checkNotNull(id));
        if (record == null) {
            return EntityStorageRecord.getDefaultInstance();
        }
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
    public EntityStorageRecord read(I id, FieldMask fieldMask) {
        final EntityStorageRecord rawResult = read(id);

        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder(rawResult);
        final Any state = builder.getState();
        final TypeUrl type = TypeUrl.of(state.getTypeUrl());
        final Message stateAsMessage = AnyPacker.unpack(state);

        final Message maskedState = FieldMasks.applyMask(fieldMask, stateAsMessage, type);

        final Any packedState = AnyPacker.pack(maskedState);
        builder.setState(packedState);
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(I id, EntityStorageRecord record) {
        checkNotNull(id);
        checkArgument(record.hasState(), "Record does not have state field.");
        checkNotClosed();

        writeRecord(id, record);
    }

    /**
     * Marks the record with the passed ID as {@code archived}.
     *
     * @param id the ID of the record to mark
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public abstract boolean markArchived(I id);

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<EntityStorageRecord> readMultiple(Iterable<I> ids) {
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
    public Iterable<EntityStorageRecord> readMultiple(Iterable<I> ids, FieldMask fieldMask) {
        checkNotClosed();
        checkNotNull(ids);

        return readMultipleRecords(ids, fieldMask);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<I, EntityStorageRecord> readAll() {
        checkNotClosed();

        return readAllRecords();
    }

    /**
     * Reads all items from the storage and apply {@link FieldMask} to each of the results.
     *
     * @param fieldMask the {@code FieldMask} to apply
     * @return all items from this repository with the given {@code FieldMask} applied
     */
    public Map<I, EntityStorageRecord> readAll(FieldMask fieldMask) {
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
    @Nullable
    protected abstract EntityStorageRecord readRecord(I id);

    /** @see BulkStorageOperationsMixin#readMultiple(java.lang.Iterable) */
    protected abstract Iterable<EntityStorageRecord> readMultipleRecords(Iterable<I> ids);

    /** @see BulkStorageOperationsMixin#readMultiple(java.lang.Iterable) */
    protected abstract Iterable<EntityStorageRecord> readMultipleRecords(Iterable<I> ids, FieldMask fieldMask);

    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Map<I, EntityStorageRecord> readAllRecords();

    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Map<I, EntityStorageRecord> readAllRecords(FieldMask fieldMask);

    /**
     * Writes a record into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id     an ID of the record
     * @param record a record to store
     */
    protected abstract void writeRecord(I id, EntityStorageRecord record);
}
