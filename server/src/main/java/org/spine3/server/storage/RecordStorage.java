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

import org.spine3.SPI;
import org.spine3.server.entity.Entity;

import javax.annotation.Nullable;

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
public abstract class RecordStorage<I> extends AbstractStorage<I, EntityStorageRecord> {

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

    @Override
    public void write(I id, EntityStorageRecord record) {
        checkNotNull(id);
        checkArgument(record.hasState(), "Record does not have state field.");
        checkNotClosed();

        writeInternal(id, record);
    }

    /**
     * Reads the records from the storage with the given IDs.
     *
     * <p>The size of {@link Iterable} returned is always the same as the size of given IDs.
     *
     * <p>In case there is no record for a particular ID, {@code null} will be present in the result.
     *
     * @param ids record IDs of interest
     * @return the {@link Iterable} containing the records matching the given IDs
     * @throws IllegalStateException if the storage was closed before
     */
    public Iterable<EntityStorageRecord> readBulk(Iterable<I> ids) {
        checkNotClosed();
        checkNotNull(ids);

        return readBulkInternal(ids);
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

    /**
     * Writes a record into the storage.
     *
     * <p>Rewrites it if a record with this ID already exists in the storage.
     *
     * @param id     an ID of the record
     * @param record a record to store
     */
    protected abstract void writeInternal(I id, EntityStorageRecord record);

    /** @see RecordStorage#readBulk(java.lang.Iterable) */
    protected abstract Iterable<EntityStorageRecord> readBulkInternal(Iterable<I> ids);
}
