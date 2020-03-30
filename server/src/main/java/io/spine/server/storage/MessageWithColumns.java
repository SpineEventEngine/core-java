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

package io.spine.server.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.server.entity.storage.ColumnMapping;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.DefaultColumnMapping;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Alex Tymchenko
 */
public class MessageWithColumns<I, R extends Message> {

    private final R record;
    private final I id;

    /**
     * A map of column names to the corresponding column values.
     */
    private final Map<ColumnName, @Nullable Object> storageFields;

    protected MessageWithColumns(I identifier, R record, Map<ColumnName, Object> storageFields) {
        this.id = checkNotNull(identifier);
        this.record = checkNotNull(record);
        this.storageFields = new HashMap<>(storageFields);
    }

    /**
     * Creates a new record extracting the column values from the passed entity.
     */
    public static <I, R extends Message>
    MessageWithColumns<I, R> create(I identifier, R record, Columns<R> columns) {
        checkNotNull(identifier);
        checkNotNull(record);
        checkNotNull(columns);
        Map<ColumnName, @Nullable Object> storageFields = columns.valuesIn(record);
        return of(identifier, record, storageFields);
    }

    public static <I, R extends Message>
    MessageWithColumns<I, R> create(I identifier,
                                    R record,
                                    Map<ColumnName, @Nullable Object> fields) {
        checkNotNull(identifier);
        checkNotNull(record);
        checkNotNull(fields);
        return of(identifier, record, fields);
    }

    /**
     * Wraps a passed entity record.
     *
     * <p>Such instance of {@code EntityRecordWithColumns} will contain no storage fields.
     */
    public static <I, R extends Message> MessageWithColumns of(I id, R record) {
        return new MessageWithColumns<>(id, record, Collections.emptyMap());
    }

    /**
     * Creates a new instance from the passed record and storage fields.
     */
    @VisibleForTesting
    public static <I, R extends Message>
    MessageWithColumns<I, R> of(I identifier, R record, Map<ColumnName, Object> storageFields) {
        return new MessageWithColumns<>(identifier, record, storageFields);
    }

    public I id() {
        return id;
    }

    /**
     * Returns the enclosed entity record.
     */
    public R record() {
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
    public @Nullable Object columnValue(ColumnName columnName) {
        return columnValue(columnName, DefaultColumnMapping.INSTANCE);
    }

    /**
     * Obtains the value of the storage field by the specified column name.
     *
     * <p>The specified column mapping will be used to do the column value conversion.
     */
    public <V> V columnValue(ColumnName columnName, ColumnMapping<V> columnMapping) {
        checkNotNull(columnName);
        checkNotNull(columnMapping);
        if (!storageFields.containsKey(columnName)) {
            throw newIllegalStateException("Column with the name `%s` was not found.",
                                           columnName);
        }
        Object columnValue = storageFields.get(columnName);
        if (columnValue == null) {
            V result = columnMapping.ofNull()
                                    .apply(null);
            return result;
        }
        V result = columnMapping.of(columnValue.getClass())
                                .applyTo(columnValue);
        return result;
    }

    /**
     * Tells if there are any {@linkplain Column columns} associated with this record.
     */
    public boolean hasColumns() {
        return !storageFields.isEmpty();
    }

    /**
     * Determines if there is a column with the specified name among the storage fields.
     */
    public boolean hasColumn(ColumnName name) {
        boolean result = storageFields.containsKey(name);
        return result;
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
