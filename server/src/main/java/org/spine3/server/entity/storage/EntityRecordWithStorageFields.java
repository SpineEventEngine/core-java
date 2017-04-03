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

package org.spine3.server.entity.storage;

import com.google.common.collect.ImmutableMap;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityRecord;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value of {@link EntityRecord} associated with its Storage Fields.
 *
 * @author Dmytro Dashenkov
 */
public final class EntityRecordWithStorageFields {

    private final EntityRecord record;

    @Nullable
    private final ImmutableMap<String, Column.MemoizedValue<?>> storageFields;

    /**
     * Creates a new instance of the {@code EntityRecordWithStorageFields}.
     *
     * @param record        {@link EntityRecord} to pack
     * @param storageFields {@linkplain StorageFields#from(Entity) Storage Fields map} to pack
     */
    private EntityRecordWithStorageFields(EntityRecord record,
                                          Map<String, Column.MemoizedValue<?>> storageFields) {
        this.record = checkNotNull(record);
        this.storageFields = ImmutableMap.copyOf(storageFields);
    }

    /**
     * Creates an instance of the {@link EntityRecordWithStorageFields} with no
     * {@linkplain StorageFields Storage Fields}.
     *
     * <p>An object created with this constructor will always return {@code false} on
     * {@link #hasStorageFields()}.
     *
     * @param record {@link EntityRecord} to pack
     * @see #hasStorageFields()
     */
    @SuppressWarnings("ConstantConditions") // null value for the Storage Fields map
    private EntityRecordWithStorageFields(EntityRecord record) {
        this.record = checkNotNull(record);
        this.storageFields = null;
    }

    /**
     * Creates a new instance of the {@code EntityRecordWithStorageFields}.
     */
    public static EntityRecordWithStorageFields newInstance(
            EntityRecord record,
            Map<String, Column.MemoizedValue<?>> storageFields) {
        return new EntityRecordWithStorageFields(record, storageFields);
    }

    /**
     * Creates an instance of the {@link EntityRecordWithStorageFields} with no Storage Fields.
     *
     * <p>An object created with this factory method will always return {@code false} on
     * {@link #hasStorageFields()}.
     *
     * @see #hasStorageFields()
     */
    public static EntityRecordWithStorageFields newInstance(EntityRecord record) {
        return new EntityRecordWithStorageFields(record);
    }

    public EntityRecord getRecord() {
        return record;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public Map<String, Column.MemoizedValue<?>> getStorageFields() {
        return storageFields == null
               ? StorageFields.empty()
               : storageFields;
    }

    /**
     * Determines whether or not there are any Storage Fields associated with this record.
     *
     * <p>If returns {@code false}, the Storage Fields are not considered
     * by the storage.
     *
     * @return {@code true} if current object was constructed with
     * {@linkplain #newInstance(EntityRecord, Map)} and {@code false} if it was
     * constructed with {@linkplain #newInstance(EntityRecord)}
     */
    public boolean hasStorageFields() {
        return storageFields != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EntityRecordWithStorageFields envelope = (EntityRecordWithStorageFields) o;

        return getRecord().equals(envelope.getRecord());
    }

    @Override
    public int hashCode() {
        return getRecord().hashCode();
    }
}
