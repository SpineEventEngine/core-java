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

package io.spine.server.entity.storage.given;

import io.spine.server.entity.storage.ColumnType;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.testing.Tests;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.spine.server.entity.storage.given.SimpleColumn.stringColumn;

public final class ColumnRecordsTestEnv {

    public static final int COLUMN_COUNT = 3;

    /** Prevents instantiation of this utility class. */
    private ColumnRecordsTestEnv() {
    }

    public static Map<String, MemoizedValue> nullableStorageFields() {
        Map<String, MemoizedValue> columns = new HashMap<>(COLUMN_COUNT);
        for (int i = 0; i < COLUMN_COUNT; i++) {
            String columnValueToPersist = (i % 2 != 0) ? null : String.valueOf(i);
            EntityColumn column = stringColumn();
            MemoizedValue memoizedValue = new MemoizedValue(column, columnValueToPersist);
            columns.put(String.valueOf(i), memoizedValue);
        }
        return columns;
    }

    public static Collection<Object> getNonNullColumnValues() {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < COLUMN_COUNT; i += 2) { // each second value is non-null
            values.add(String.valueOf(i));
        }
        return values;
    }

    public static final class CollectAnyColumnType
            implements ColumnType<Object, Object, Collection<Object>, Object> {

        @Override
        public Object convertColumnValue(Object fieldValue) {
            return fieldValue;
        }

        @Override
        public void setColumnValue(Collection<Object> storageRecord, Object value,
                                   Object columnIdentifier) {
            storageRecord.add(value);
        }

        @Override
        public void setNull(Collection<Object> storageRecord, Object columnIdentifier) {
            storageRecord.add(Tests.nullRef());
        }
    }

    public static final class NoOpColumnIdentifierMapper implements Function<String, Object> {
        @Override
        public Object apply(@Nullable String s) {
            return s;
        }
    }
}
