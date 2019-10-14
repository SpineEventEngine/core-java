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
import com.google.common.collect.ImmutableMap;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.WithLifecycle;
import io.spine.server.storage.RecordStorage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.server.storage.VersionField.version;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A value of {@link EntityRecord} associated with the values of its {@linkplain Column columns}.
 */
public final class EntityRecordWithColumns implements WithLifecycle, Serializable {

    private static final long serialVersionUID = 0L;

    private final EntityRecord record;
    private final ImmutableMap<String, Object> storageFields;

    /**
     * Creates a new instance with storage fields.
     *
     * @param record
     *         the record to pack
     * @param storageFields
     *         the storage fields to pack
     */
    private EntityRecordWithColumns(EntityRecord record, Map<String, Object> storageFields) {
        this.record = checkNotNull(record);
        this.storageFields = ImmutableMap.copyOf(storageFields);
    }

    /**
     */
    public static EntityRecordWithColumns create(EntityRecord record,
                                                 Entity<?, ?> entity,
                                                 RecordStorage<?> storage) {
        Columns columns = storage.columns();
        Map<String, Object> storageFields = columns.valuesIn(entity);
        return of(record, storageFields);
    }

    /**
     * ...
     *
     * <p>Manually updates the entity lifecycle and version storage fields based on the passed
     * {@link EntityRecord}.
     */
    public static EntityRecordWithColumns of(EntityRecord record, RecordStorage<?> storage) {
        Map<String, Object> storageFields = new HashMap<>();
        storeLifecycle(storageFields, record, storage);
        storeVersion(storageFields, record, storage);
        return new EntityRecordWithColumns(record, storageFields);
    }

    private static void storeLifecycle(Map<String, Object> storageFields,
                                       EntityRecord record,
                                       RecordStorage<?> storage) {
        Columns columns = storage.lifecycleColumns();
        String archivedColumn = archived.name();
        boolean archivedPresent = columns.find(archivedColumn)
                                         .isPresent();
        String deletedColumn = deleted.name();
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

    private static void storeVersion(Map<String, Object> storageFields,
                                     EntityRecord record,
                                     RecordStorage<?> storage) {
        String versionColumn = version.name();
        boolean versionPresent = storage.columns()
                                        .find(versionColumn)
                                        .isPresent();
        if (versionPresent) {
            storageFields.put(versionColumn, record.getVersion());
        }
    }

    /**
     * Creates a new instance.
     */
    @VisibleForTesting
    static EntityRecordWithColumns of(EntityRecord record, Map<String, Object> storageFields) {
        return new EntityRecordWithColumns(record, storageFields);
    }

    public EntityRecord record() {
        return record;
    }

    /**
     * Obtains entity column {@linkplain Column#name() names} for the record.
     *
     * @return the entity column names
     */
    public Set<String> columnNames() {
        return storageFields.keySet();
    }

    /**
     * Obtains the memoized value of the entity column by the specified
     * {@linkplain Column#name() name}.
     *
     * @param columnName
     *         the column name
     * @return the memoized value of the column
     * @throws IllegalStateException
     *         if there is no column with the specified name
     */
    @Internal
    public Object columnValue(String columnName) {
        checkNotNull(columnName);
        if (!storageFields.containsKey(columnName)) {
            throw newIllegalStateException("Column with the stored name `%s` was not found.",
                                           columnName);
        }
        return storageFields.get(columnName);
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

    public boolean hasColumn(String name) {
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
