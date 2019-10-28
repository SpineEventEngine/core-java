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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A value of {@link EntityRecord} associated with the values of its {@linkplain Column columns}.
 */
public final class EntityRecordWithColumns implements WithLifecycle {

    private final EntityRecord record;

    /**
     * A map of column names to the corresponding column values.
     *
     * <p>May contain {@code null} column values.
     */
    private final Map<ColumnName, Object> storageFields;

    private final boolean hasStorageFields;

    private EntityRecordWithColumns(EntityRecord record, Map<ColumnName, Object> storageFields) {
        this.record = checkNotNull(record);
        this.storageFields = new HashMap<>(storageFields);
        this.hasStorageFields = !storageFields.isEmpty();
    }

    /**
     * Creates a new record extracting the column values from the passed entity.
     */
    public static EntityRecordWithColumns create(EntityRecord record,
                                                 Entity<?, ?> entity,
                                                 RecordStorage<?> recordStorage) {
        Columns columns = recordStorage.columns();
        Map<ColumnName, Object> storageFields = columns.valuesIn(entity);
        return of(record, storageFields);
    }

    /**
     * Wraps a passed entity record.
     *
     * <p>Such instance of {@code EntityRecordWithColumns} will contain no storage fields.
     */
    public static EntityRecordWithColumns of(EntityRecord record) {
        return new EntityRecordWithColumns(record, Collections.emptyMap());
    }

    /**
     * Creates a new instance from the passed record and storage fields.
     */
    @VisibleForTesting
    public static EntityRecordWithColumns
    of(EntityRecord record, Map<ColumnName, Object> storageFields) {
        return new EntityRecordWithColumns(record, storageFields);
    }

    /**
     * Returns the enclosed entity record.
     */
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
     * <p>The {@linkplain DefaultColumnMapping default column mapping} will be used. It is suitable
     * for storages that store values "as-is" or that are willing to do the manual column value
     * conversion.
     *
     * <p>In other cases consider implementing a custom {@link ColumnMapping} and using the
     * {@link #columnValue(ColumnName, ColumnMapping)} overload for convenient column value
     * conversion.
     *
     * @param columnName
     *         the column name
     * @return the storage field value
     * @throws IllegalStateException
     *         if there is no column with the specified name
     */
    public Object columnValue(ColumnName columnName) {
        return columnValue(columnName, DefaultColumnMapping.INSTANCE);
    }

    /**
     * Obtains the value of the storage field by the specified column name.
     *
     * <p>The specified column mapping will be used to do the column value conversion.
     */
    public <R> R columnValue(ColumnName columnName, ColumnMapping<R> columnMapping) {
        checkNotNull(columnName);
        checkNotNull(columnMapping);
        if (!storageFields.containsKey(columnName)) {
            throw newIllegalStateException("Column with the name `%s` was not found.",
                                           columnName);
        }
        Object columnValue = storageFields.get(columnName);
        if (columnValue == null) {
            R result = columnMapping.ofNull()
                                    .apply(null);
            return result;
        }
        R result = columnMapping.of(columnValue.getClass())
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
        return hasStorageFields;
    }

    /**
     * Determines if there is a column with the specified name among the storage fields.
     */
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
