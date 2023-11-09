/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import io.spine.query.RecordColumn;
import io.spine.server.storage.given.GivenStorageProject.StgProjectColumns;
import io.spine.server.storage.given.TestColumnMapping;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.storage.given.GivenStorageProject.StgProjectColumns.due_date;
import static io.spine.server.storage.given.GivenStorageProject.StgProjectColumns.name;
import static io.spine.server.storage.given.GivenStorageProject.StgProjectColumns.random_non_stored_column;
import static io.spine.server.storage.given.GivenStorageProject.messageSpec;
import static io.spine.server.storage.given.GivenStorageProject.newState;
import static io.spine.server.storage.given.TestColumnMapping.CONVERTED_STRING;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`RecordWithColumns` should")
class RecordWithColumnsTest {

    @Test
    @DisplayName("not accept `null`s in static API")
    void rejectNullInCtor() {
        new NullPointerTester()
                .testAllPublicStaticMethods(RecordWithColumns.class);
    }

    @Test
    @DisplayName("support record equality")
    void supportEquality() {
        var record = newState();
        var noFields = withNoCols(record);
        var withFieldsBySpec = withCols(record);
        var withFieldsByIdAndSpec = RecordWithColumns.create(record.getId(), record, messageSpec());
        new EqualsTester()
                .addEqualityGroup(noFields)
                .addEqualityGroup(withFieldsBySpec, withFieldsByIdAndSpec)
                .testEquals();
    }

    @Test
    @DisplayName("return empty names collection if no storage fields are set")
    void returnEmptyColumns() {
        var record = withCols();
        assertThat(record.hasColumns()).isFalse();

        var names = record.columnNames();
        assertThat(names.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("throw `ISE` on attempt to get value by non-existent name")
    void throwOnNonExistentColumn() {
        var record = withCols();
        assertThrows(IllegalStateException.class,
                     () -> record.columnValue(random_non_stored_column.name()));
    }

    @Test
    @DisplayName("return column value by column name")
    void returnColumnValue() {
        var original = newState();
        var withColumns = withCols(original);
        var actual = withColumns.columnValue(due_date.name());

        assertThat(actual)
                .isEqualTo(original.getDueDate());
    }

    @Test
    @DisplayName("return a column value with the column mapping applied")
    void returnValueWithColumnMapping() {
        var original = newState();
        var withColumns = withCols(original);
        var actual = withColumns.columnValue(name.name(), new TestColumnMapping());

        assertThat(actual)
                .isEqualTo(CONVERTED_STRING);
    }

    @Nested
    @DisplayName("return")
    class Store {

        @Test
        @DisplayName("original record")
        void record() {
            var original = newState();
            var record = withCols(original);
            assertThat(record.record())
                    .isEqualTo(original);
        }

        @Test
        @DisplayName("columns and their values")
        void columnValues() {
            var original = newState();
            var record = withCols(original);

            var expectedColumns = StgProjectColumns.definitions();
            var expectedNames = expectedColumns.stream()
                    .map(RecordColumn::name)
                    .collect(toImmutableSet());
            assertThat(record.columnNames())
                    .isEqualTo(expectedNames);

            for (var column : expectedColumns) {
                var expected = column.valueIn(original);
                var actual = record.columnValue(column.name());
                assertThat(actual).
                        isEqualTo(expected);
            }
        }
    }

    @NonNull
    private static
    RecordWithColumns<StgProjectId, StgProject> withCols(StgProject original) {
        return RecordWithColumns.create(original, messageSpec());
    }

    private static
    RecordWithColumns<StgProjectId, StgProject> withNoCols(StgProject record) {
        return RecordWithColumns.of(record.getId(), record);
    }

    @NonNull
    private static RecordWithColumns<StgProjectId, StgProject> withCols() {
        return withNoCols(newState());
    }
}
