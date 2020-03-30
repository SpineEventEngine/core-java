/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import io.spine.base.Identifier;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.StorageConverter;
import io.spine.server.entity.WithLifecycle;
import io.spine.server.storage.Column;
import io.spine.server.storage.MessageWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value of {@link EntityRecord} associated with the values of its {@linkplain Column columns}.
 */
public final class EntityRecordWithColumns<I>
        extends MessageWithColumns<I, EntityRecord> implements WithLifecycle {

    private final EntityRecord record;

    private EntityRecordWithColumns(EntityRecord record, Map<ColumnName, Object> columns) {
        this(extractId(record), record, columns);
    }

    private EntityRecordWithColumns(I id, EntityRecord record, Map<ColumnName, Object> columns) {
        super(id, record, columns);
        this.record = checkNotNull(record);
    }

    private static <I> I extractId(EntityRecord record) {
        return (I) Identifier.unpack(record.getEntityId());
    }
//
//    /**
//     * Creates a new record extracting the column values from the passed entity.
//     */
//    public static <I> EntityRecordWithColumns<I> create(EntityRecord record,
//                                                 Entity<I, ?> entity,
//                                                 RecordStorage<?> recordStorage) {
//        EntityColumns columns = recordStorage.columns();
//        Map<ColumnName, @Nullable Object> storageFields = columns.valuesIn(entity);
//        return of(record, storageFields);
//    }

    /**
     * Creates a new record extracting the column values from the passed entity.
     */
    public static <I, E extends Entity<I, ?>> EntityRecordWithColumns<I>
    create(E entity, StorageConverter<I, E, ?> converter, EntityColumns columns) {
        EntityRecord record = converter.convert(entity);
        checkNotNull(record);   //TODO:2020-03-19:alex.tymchenko: suspicious?
        return create(entity, columns, record);
    }

    //TODO:2020-03-17:alex.tymchenko: avoid ambiguity.
    public static <I, E extends Entity<I, ?>> EntityRecordWithColumns<I>
    create(E entity, EntityColumns columns, EntityRecord record) {
        Map<ColumnName, @Nullable Object> storageFields = columns.valuesIn(entity);
        return new EntityRecordWithColumns<>(entity.id(), record, storageFields);
    }

    /**
     * Wraps a passed entity record.
     *
     * <p>Such instance of {@code EntityRecordWithColumns} will contain no storage fields.
     */
    public static <I> EntityRecordWithColumns<I> of(EntityRecord record) {
        return new EntityRecordWithColumns<>(record, Collections.emptyMap());
    }

    /**
     * Creates a new instance from the passed record and storage fields.
     */
    @VisibleForTesting
    public static <I> EntityRecordWithColumns<I>
    of(EntityRecord record, Map<ColumnName, Object> storageFields) {
        return new EntityRecordWithColumns<>(record, storageFields);
    }

    @Override
    public LifecycleFlags getLifecycleFlags() {
        return record.getLifecycleFlags();
    }

    @Override
    public boolean isArchived() {
        return record.isArchived();
    }

    @Override
    public boolean isDeleted() {
        return record.isDeleted();
    }

    @Override
    public boolean isActive() {
        return record.isActive();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EntityRecordWithColumns other = (EntityRecordWithColumns) o;

        return record().equals(other.record());
    }

    @Override
    public int hashCode() {
        return record().hashCode();
    }
}
