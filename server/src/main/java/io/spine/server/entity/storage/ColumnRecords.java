/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import io.spine.server.entity.storage.EntityColumn.MemoizedValue;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A utility for dealing with the {@linkplain EntityRecordWithColumns} and
 * the {@linkplain EntityColumn entity columns}.
 */
public final class ColumnRecords {

    /** Prevents instantiation of the utility class. */
    private ColumnRecords() {
    }

    /**
     * Feeds the given {@link EntityRecordWithColumns} to the given record representation.
     *
     * <p>This method deals with the {@linkplain EntityColumn entity columns} only.
     * The {@link io.spine.server.entity.EntityRecord EntityRecord} should be handled separately
     * while persisting a record.
     *
     * @param destination
     *        the representation of a record in the database; implementation specific
     * @param record
     *        the {@linkplain EntityRecordWithColumns} to persist
     * @param registry
     *        the {@link io.spine.server.storage.Storage Storage} {@link ColumnTypeRegistry}
     * @param mapColumnIdentifier
     *        a {@linkplain Function} mapping the column name
     *        {@linkplain EntityColumn#getStoredName()} for storing} to the database specific
     *        column identifier
     * @param <D>
     *        the type of the database record
     * @param <I>
     *        the type of the column identifier
     */
    public static <D, I> void feedColumnsTo(
            D destination,
            EntityRecordWithColumns record,
            ColumnTypeRegistry<? extends ColumnType<?, ?, D, I>> registry,
            Function<String, I> mapColumnIdentifier) {
        checkNotNull(destination);
        checkNotNull(record);
        checkNotNull(registry);
        checkNotNull(mapColumnIdentifier);
        checkArgument(record.hasColumns(), "Passed record has no Entity Columns.");

        for (String columnName : record.getColumnNames()) {
            feedColumn(record, columnName, destination, registry, mapColumnIdentifier);
        }
    }

    private static <D, I> void feedColumn(
            EntityRecordWithColumns record,
            String columnName,
            D destination,
            ColumnTypeRegistry<? extends ColumnType<?, ?, D, I>> registry,
            Function<String, I> mapColumnIdentifier) {
        I columnIdentifier = mapColumnIdentifier.apply(columnName);
        checkNotNull(columnIdentifier);
        MemoizedValue columnValue = record.getColumnValue(columnName);
        EntityColumn columnMetadata = columnValue.getSourceColumn();

        @SuppressWarnings("unchecked") // We don't know the exact types of the value
        ColumnType<Object, Object, D, I> columnType =
                (ColumnType<Object, Object, D, I>) registry.get(columnMetadata);
        checkArgument(columnType != null,
                      format("ColumnType for %s could not be found.",
                                    columnMetadata.getPersistedType()
                                                  .getCanonicalName()));
        setValue(columnValue, destination, columnIdentifier, columnType);
    }

    private static <J, S, D, I> void setValue(MemoizedValue columnValue,
                                              D destination,
                                              I columnIdentifier,
                                              ColumnType<J, S, D, I> columnType) {
        @SuppressWarnings("unchecked") // Checked at runtime
        J initialValue = (J) columnValue.getValue();
        if (initialValue == null) {
            columnType.setNull(destination, columnIdentifier);
        } else {
            S storedValue = columnType.convertColumnValue(initialValue);
            columnType.setColumnValue(destination, storedValue, columnIdentifier);
        }
    }
}
