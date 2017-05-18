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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityRecord;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value of {@link EntityRecord} associated with its {@link Column Columns}.
 *
 * @author Dmytro Dashenkov
 */
public final class EntityRecordWithColumns implements Serializable {

    private static final long serialVersionUID = 1576133534602043154L;

    private final EntityRecord record;
    private final ImmutableMap<String, Column.MemoizedValue> storageFields;
    private final boolean hasStorageFields;

    /**
     * Creates a new instance of the {@code EntityRecordWithColumns}.
     *
     * @param record  {@link EntityRecord} to pack
     * @param columns {@linkplain Columns#from(Entity) {@link Column Columns} map} to pack
     */
    private EntityRecordWithColumns(EntityRecord record,
                                    Map<String, Column.MemoizedValue> columns) {
        this.record = checkNotNull(record);
        this.storageFields = ImmutableMap.copyOf(columns);
        this.hasStorageFields = true;
    }

    /**
     * Creates an instance of the {@link EntityRecordWithColumns} with no
     * {@linkplain Columns {@link Column Columns}}.
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
     * {@linkplain Column Column values} from the given {@linkplain Entity}.
     */
    public static EntityRecordWithColumns create(EntityRecord record, Entity entity) {
        final Map<String, Column.MemoizedValue> columns = Columns.from(entity);
        return of(record, columns);
    }

    /**
     * Creates an instance of the {@link EntityRecordWithColumns} with no {@link Column Columns}.
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
    static EntityRecordWithColumns of(
            EntityRecord record,
            Map<String, Column.MemoizedValue> storageFields) {
        return new EntityRecordWithColumns(record, storageFields);
    }

    public EntityRecord getRecord() {
        return record;
    }

    public Map<String, Column> getColumns() {
        return Maps.transformEntries(storageFields,
                                     ColumnTransformer.INSTANCE);
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
        // Immutable structure
    Map<String, Column.MemoizedValue> getColumnValues() {
        return storageFields;
    }

    /**
     * Determines whether or not there are any {@link Column Columns} associated with this record.
     *
     * <p>If returns {@code false}, the {@link Column Columns} are not considered
     * by the storage.
     *
     * @return {@code true} if current object was constructed with
     * {@linkplain #create(EntityRecord, Entity)} and {@code false} if it was
     * constructed with {@linkplain #of(EntityRecord)}
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

    private enum ColumnTransformer
            implements Maps.EntryTransformer<String, Column.MemoizedValue, Column> {

        INSTANCE;

        @Override
        public Column transformEntry(@Nullable String s,
                                     @Nullable Column.MemoizedValue memoizedValue) {
            checkNotNull(s);
            checkNotNull(memoizedValue);
            return memoizedValue.getSourceColumn();
        }
    }
}
