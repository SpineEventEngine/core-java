/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A value of {@link EntityRecord} associated with its {@linkplain EntityColumn columns}.
 *
 * @author Dmytro Dashenkov
 */
public final class EntityRecordWithColumns implements Serializable {

    private static final long serialVersionUID = 0L;

    private final EntityRecord record;
    private final ImmutableMap<String, EntityColumn.MemoizedValue> storageFields;
    private final boolean hasStorageFields;

    /**
     * Creates a new instance of the {@code EntityRecordWithColumns}.
     *
     * @param record  {@link EntityRecord} to pack
     * @param columns {@linkplain Columns#from(Entity) columns} map to pack
     */
    private EntityRecordWithColumns(EntityRecord record,
                                    Map<String, EntityColumn.MemoizedValue> columns) {
        this.record = checkNotNull(record);
        this.storageFields = ImmutableMap.copyOf(columns);
        this.hasStorageFields = !columns.isEmpty();
    }

    /**
     * Creates an instance of the {@link EntityRecordWithColumns} with no
     * {@linkplain EntityColumn columns}.
     *
     * <p>An object created with this constructor will always return {@code false} on
     * {@link #hasColumns()}.
     *
     * @param record {@link EntityRecord} to pack
     * @see #hasColumns()
     */
    private EntityRecordWithColumns(EntityRecord record) {
        this.record = checkNotNull(record);
        this.storageFields = ImmutableMap.of();
        this.hasStorageFields = false;
    }

    /**
     * Creates a new instance of the {@code EntityRecordWithColumns} with
     * {@link EntityColumn} values from the given {@linkplain Entity}.
     */
    public static EntityRecordWithColumns create(EntityRecord record, Entity entity) {
        final Map<String, EntityColumn.MemoizedValue> columns = Columns.from(entity);
        return of(record, columns);
    }

    /**
     * Creates an instance of the {@link EntityRecordWithColumns}
     * with no {@linkplain EntityColumn columns}.
     *
     * <p>An object created with this factory method will always return {@code false} on
     * {@link #hasColumns()}.
     *
     * @see #hasColumns()
     */
    public static EntityRecordWithColumns of(EntityRecord record) {
        return new EntityRecordWithColumns(record);
    }

    /**
     * Creates a new instance of the {@code EntityRecordWithColumns}.
     */
    @VisibleForTesting
    static EntityRecordWithColumns of(EntityRecord record,
                                      Map<String, EntityColumn.MemoizedValue> storageFields) {
        return new EntityRecordWithColumns(record, storageFields);
    }

    public EntityRecord getRecord() {
        return record;
    }

    /**
     * Obtains entity column {@linkplain EntityColumn#getStoredName() names} for the record.
     *
     * @return the entity column names
     */
    public Set<String> getColumnNames() {
        return storageFields.keySet();
    }

    /**
     * Obtains the memoized value of the entity column
     * by the specified {@linkplain EntityColumn#getStoredName() name}.
     *
     * @param columnName the stored column name
     * @return the memoized value of the column
     * @throws IllegalStateException if there is no column with the specified name
     * @see ColumnRecords for the recommended way of working with the column values
     */
    @Internal
    public EntityColumn.MemoizedValue getColumnValue(String columnName) {
        checkNotNull(columnName);
        if (!storageFields.containsKey(columnName)) {
            throw newIllegalStateException("Column with the stored name `%s` was not found.",
                                           columnName);
        }
        return storageFields.get(columnName);
    }

    /**
     * Determines whether or not there are any {@linkplain EntityColumn columns}
     * associated with this record.
     *
     * <p>If returns {@code false}, the {@linkplain EntityColumn columns} are not considered
     * by the storage.
     *
     * @return {@code true} if the object was constructed via {@link #create(EntityRecord, Entity)}
     *         and the entity has columns; {@code false} otherwise
     */
    public boolean hasColumns() {
        return hasStorageFields;
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

        return getRecord().equals(other.getRecord());
    }

    @Override
    public int hashCode() {
        return getRecord().hashCode();
    }
}
