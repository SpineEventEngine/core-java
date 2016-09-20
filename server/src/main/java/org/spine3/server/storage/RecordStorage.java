/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

    @Override
    public EntityStorageRecord read(I id) {
        checkNotClosed();
        checkNotNull(id);

        final EntityStorageRecord record = readInternal(checkNotNull(id));
        if (record == null) {
            return EntityStorageRecord.getDefaultInstance();
        }
        return record;
    }

    // TODO:20-09-16:dmytro.dashenkov: Add javadoc.
    public EntityStorageRecord read(I id, FieldMask fieldMask) {
        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder(read(id));
        final Any state = builder.getState();
        builder.setState(AnyPacker.pack(FieldMasks.applyIfEffective(fieldMask, AnyPacker.unpack(state), TypeUrl.of(state.getTypeUrl()))));
        return builder.build();
    }


    @Override
    public void write(I id, EntityStorageRecord record) {
        checkNotNull(id);
        checkArgument(record.hasState(), "Record does not have state field.");
        checkNotClosed();

        writeInternal(id, record);
    }

    @Override
    public Iterable<EntityStorageRecord> readBulk(Iterable<I> ids) {
        checkNotClosed();
        checkNotNull(ids);

        return readBulkInternal(ids);
    }

    public Iterable<EntityStorageRecord> readBulk(Iterable<I> ids, FieldMask fieldMask) {
        checkNotClosed();
        checkNotNull(ids);

        return readBulkInternal(ids, fieldMask);
    }

    @Override
    public Map<I, EntityStorageRecord> readAll() {
        checkNotClosed();

        return readAllInternal();
    }

    public Map<I, EntityStorageRecord> readAll(FieldMask fieldMask) {
        checkNotClosed();

        return readAllInternal(fieldMask);
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
    protected abstract EntityStorageRecord readInternal(I id);

    /** @see BulkStorageOperationsMixin#readBulk(java.lang.Iterable) */
    protected abstract Iterable<EntityStorageRecord> readBulkInternal(Iterable<I> ids);

    /** @see BulkStorageOperationsMixin#readBulk(java.lang.Iterable) */
    protected abstract Iterable<EntityStorageRecord> readBulkInternal(Iterable<I> ids, FieldMask fieldMask);


    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Map<I, EntityStorageRecord> readAllInternal();

    /** @see BulkStorageOperationsMixin#readAll() */
    protected abstract Map<I, EntityStorageRecord> readAllInternal(FieldMask fieldMask);

    /**
     * Writes a record into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id     an ID of the record
     * @param record a record to store
     */
    protected abstract void writeInternal(I id, EntityStorageRecord record);

}
