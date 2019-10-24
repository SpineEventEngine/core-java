/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableSet;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.WithLifecycle;
import io.spine.server.storage.RecordStorage;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.server.storage.VersionField.version;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A value of {@link EntityRecord} associated with the values of its {@linkplain Column columns}.
 */
public final class EntityRecordWithColumns implements WithLifecycle {

    private final EntityRecord record;
    private final Map<ColumnName, Object> storageFields;

    /**
     * Creates a new instance with storage fields.
     *
     * @param record
     *         the record to pack
     * @param storageFields
     *         the storage fields to pack
     */
    private EntityRecordWithColumns(EntityRecord record, Map<ColumnName, Object> storageFields) {
        this.record = checkNotNull(record);
        this.storageFields = new HashMap<>(storageFields);
    }

    /**
     */
    public static EntityRecordWithColumns create(EntityRecord record,
                                                 Entity<?, ?> entity,
                                                 RecordStorage<?> storage) {
        Columns columns = storage.columns();
        Map<ColumnName, Object> storageFields = columns.valuesIn(entity, Column::name);
        return of(record, storageFields);
    }

    /**
     * ...
     *
     * <p>Manually updates the entity lifecycle and version storage fields based on the passed
     * {@link EntityRecord}.
     */
    public static EntityRecordWithColumns create(EntityRecord record, RecordStorage<?> storage) {
        return create(record, storage.columns());
    }

    @VisibleForTesting
    static EntityRecordWithColumns create(EntityRecord record, Columns columns) {
        Map<ColumnName, Object> storageFields = new HashMap<>();
        storeLifecycle(storageFields, record, columns);
        storeVersion(storageFields, record, columns);
        return new EntityRecordWithColumns(record, storageFields);
    }

    private static void storeLifecycle(Map<ColumnName, Object> storageFields,
                                       EntityRecord record,
                                       Columns columns) {
        ColumnName archivedColumn = ColumnName.of(archived);
        boolean archivedPresent = columns.find(archivedColumn)
                                         .isPresent();
        ColumnName deletedColumn = ColumnName.of(deleted);
        boolean deletedPresent = columns.find(deletedColumn)
                                        .isPresent();
        if (archivedPresent && deletedPresent) {
            boolean archived = record.getLifecycleFlags()
                                     .getArchived();
            storageFields.put(archivedColumn, archived);

            boolean deleted = record.getLifecycleFlags()
                                    .getDeleted();
            storageFields.put(deletedColumn, deleted);
        }
    }

    private static void storeVersion(Map<ColumnName, Object> storageFields,
                                     EntityRecord record,
                                     Columns columns) {
        ColumnName versionColumn = ColumnName.of(version);
        boolean versionPresent = columns.find(versionColumn)
                                        .isPresent();
        if (versionPresent) {
            storageFields.put(versionColumn, record.getVersion());
        }
    }

    /**
     * Creates a new instance.
     */
    @VisibleForTesting
    public static EntityRecordWithColumns
    of(EntityRecord record, Map<ColumnName, Object> storageFields) {
        return new EntityRecordWithColumns(record, storageFields);
    }

    public EntityRecord record() {
        return record;
    }

    /**
     * Obtains the names of storage fields in the record.
     *
     * @return the storage field names
     */
    public ImmutableSet<ColumnName> columnNames() {
        return ImmutableSet.copyOf(storageFields.keySet());
    }

    /**
     * Obtains the value of the storage field by the specified column name.
     *
     * @param columnName
     *         the column name
     * @return the storage field value
     * @throws IllegalStateException
     *         if there is no column with the specified name
     */
    public Object columnValue(ColumnName columnName) {
        return columnValue(columnName, DefaultStorageRules.INSTANCE);
    }

    public <R> R columnValue(ColumnName columnName, ColumnStorageRules<R> storageRules) {
        checkNotNull(columnName);
        checkNotNull(storageRules);
        if (!storageFields.containsKey(columnName)) {
            throw newIllegalStateException("Column with the name `%s` was not found.",
                                           columnName);
        }
        Object columnValue = storageFields.get(columnName);
        if (columnValue == null) {
            R result = storageRules.ofNull()
                                   .apply(null);
            return result;
        }
        R result = storageRules.of(columnValue.getClass())
                               .applyTo(columnValue);
        return result;
    }

    /**
     * Determines if there are any {@linkplain Column columns} associated with this record.
     *
     * <p>If returns {@code false}, the columns are not considered by the storage.
     *
     * @return {@code true} if the object was constructed using
     *  {@link #create(EntityRecord, Entity, RecordStorage)} and the entity has columns;
     *  {@code false} otherwise
     */
    public boolean hasColumns() {
        return !storageFields.isEmpty();
    }

    public boolean hasColumn(ColumnName name) {
        boolean result = storageFields.containsKey(name);
        return result;
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
    public boolean equals(Object o) {
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
