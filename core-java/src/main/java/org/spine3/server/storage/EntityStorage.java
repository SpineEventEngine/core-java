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

import org.spine3.base.EntityRecord;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An entity storage keeps messages with identity.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassMayBeInterface")
public abstract class EntityStorage<I> {

    /**
     * Loads an entity record from the storage by an ID.
     *
     * @param id the ID of the entity record to load
     * @return an entity record instance or {@code null} if there is no record with such an ID
     */
    @Nullable
    public EntityRecord load(I id) {
        final EntityStorageRecord record = read(id);
        if (record == null) {
            return null;
        }
        final EntityRecord result = toEntityRecord(record);
        return result;
    }

    /**
     * Loads an entity storage record from the storage by an ID.
     *
     * @param id the ID of the entity record to load
     * @return an entity record instance or {@code null} if there is no record with such an ID
     */
    @Nullable
    protected abstract EntityStorageRecord read(I id);

    /**
     * Saves a {@code record} into the storage. Rewrites it if a  {@code record} with such an ID already exists.
     *
     * @param record a record to save
     * @throws NullPointerException if the {@code record} is null
     */
    public void store(EntityRecord record) {
        checkNotNull(record);
        final EntityStorageRecord storageRecord = toEntityStorageRecord(record);
        write(storageRecord);
    }

    /**
     * Writes a record into the storage. Rewrites it if the record with such an entity ID already exists.
     *
     * @param record a record to save
     * @throws NullPointerException if the {@code record} is null
     */
    protected abstract void write(EntityStorageRecord record);

    private static EntityRecord toEntityRecord(EntityStorageRecord record) {
        final EntityRecord.Builder builder = EntityRecord.newBuilder()
                .setEntityState(record.getEntityState())
                .setEntityId(record.getEntityId())
                .setWhenModified(record.getWhenModified())
                .setVersion(record.getVersion());
        return builder.build();
    }

    private static EntityStorageRecord toEntityStorageRecord(EntityRecord record) {
        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder()
                .setEntityState(record.getEntityState())
                .setEntityId(record.getEntityId())
                .setWhenModified(record.getWhenModified())
                .setVersion(record.getVersion());
        return builder.build();
    }
}
