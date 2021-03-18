/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.client.ArchivedColumn;
import io.spine.client.DeletedColumn;
import io.spine.query.ColumnName;
import io.spine.query.RecordColumn;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.given.EntityWithoutCustomColumns;
import io.spine.server.entity.storage.given.TestEntity;
import io.spine.server.storage.given.TestColumnMapping;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
import io.spine.test.storage.StgTask;
import io.spine.test.storage.StgTaskId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`EntityRecordWithColumns` should")
class EntityRecordWithColumnsTest {

    private static final StgTaskId TASK_ID =
            StgTaskId.newBuilder()
                     .setId(42)
                     .vBuild();

    private static final StgProjectId PROJECT_ID =
            StgProjectId.newBuilder()
                        .setId("42")
                        .vBuild();

    private static final Any PACKED_TASK_ID = Identifier.pack(TASK_ID);

    private static EntityRecordWithColumns<?> sampleRecordWithEmptyColumns() {
        return EntityRecordWithColumns.of(sampleEntityRecord());
    }

    private static EntityRecordWithColumns<?> randomRecordWithEmptyColumns() {
        return EntityRecordWithColumns.of(randomEntityRecord());
    }

    private static EntityRecord sampleEntityRecord() {
        return EntityRecord.newBuilder()
                           .setEntityId(PACKED_TASK_ID)
                           .build();
    }

    private static EntityRecord randomEntityRecord() {
        return EntityRecord.newBuilder()
                           .setEntityId(Identifier.pack(Identifier.newUuid()))
                           .build();
    }

    @Test
    @DisplayName("not accept `null`s in constructor")
    void rejectNullInCtor() {
        new NullPointerTester()
                .setDefault(EntityRecord.class, sampleEntityRecord())
                .testAllPublicConstructors(EntityRecordWithColumns.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        ColumnName columnName = ArchivedColumn.instance()
                                              .name();
        Object value = false;
        EntityRecordWithColumns<?> emptyFieldsEnvelope =
                EntityRecordWithColumns.of(sampleEntityRecord());
        EntityRecordWithColumns<?> notEmptyFieldsEnvelope =
                EntityRecordWithColumns.of(sampleEntityRecord(), singletonMap(columnName, value));
        new EqualsTester()
                .addEqualityGroup(emptyFieldsEnvelope)
                .addEqualityGroup(notEmptyFieldsEnvelope)
                .addEqualityGroup(randomRecordWithEmptyColumns())
                .addEqualityGroup(
                        randomRecordWithEmptyColumns()  // Each one has a different `EntityRecord`.
                )
                .testEquals();
    }

    @Test
    @DisplayName("return empty names collection if no storage fields are set")
    void returnEmptyColumns() {
        EntityRecordWithColumns<?> record = sampleRecordWithEmptyColumns();
        assertFalse(record.hasColumns());
        ImmutableSet<ColumnName> names = record.columnNames();
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("throw `ISE` on attempt to get value by non-existent name")
    void throwOnNonExistentColumn() {
        EntityRecordWithColumns<?> record = sampleRecordWithEmptyColumns();
        ColumnName nonExistentName = ColumnName.of("non-existent-column");
        assertThrows(IllegalStateException.class, () -> record.columnValue(nonExistentName));
    }

    @Test
    @DisplayName("have `Entity` lifecycle columns even if the entity does not define custom ones")
    void supportEmptyColumns() {
        EntityWithoutCustomColumns entity = new EntityWithoutCustomColumns(TASK_ID);

        EntityRecordSpec<StgTaskId, StgTask, EntityWithoutCustomColumns> columns =
                EntityRecordSpec.of(entity);
        Map<ColumnName, Object> storageFields = columns.valuesIn(entity);

        EntityRecordWithColumns<?> record =
                EntityRecordWithColumns.of(sampleEntityRecord(), storageFields);
        assertThat(record.columnNames())
                .containsExactlyElementsIn(EntityRecordColumn.names());
    }

    @Test
    @DisplayName("return column value by column name")
    void returnColumnValue() {
        ColumnName columnName = ColumnName.of("some-boolean-column");
        boolean columnValue = false;
        ImmutableMap<ColumnName, Object> storageFields = ImmutableMap.of(columnName, columnValue);
        EntityRecordWithColumns<?> record =
                EntityRecordWithColumns.of(sampleEntityRecord(), storageFields);
        Object value = record.columnValue(columnName);

        assertThat(value).isEqualTo(columnValue);
    }

    @Test
    @DisplayName("return a column value with the column mapping applied")
    void returnValueWithColumnMapping() {
        ColumnName columnName = ColumnName.of("some-int-column");
        int columnValue = 42;

        ImmutableMap<ColumnName, Object> storageFields = ImmutableMap.of(columnName, columnValue);
        EntityRecordWithColumns<?> record =
                EntityRecordWithColumns.of(sampleEntityRecord(), storageFields);
        String value = record.columnValue(columnName, new TestColumnMapping());

        assertThat(value).isEqualTo(String.valueOf(columnValue));
    }

    @Test
    @DisplayName("return `null` column value")
    void returnNullValue() {
        ColumnName columnName = ColumnName.of("the-null-column");
        Map<ColumnName, Object> storageFields = new HashMap<>();
        storageFields.put(columnName, null);
        EntityRecordWithColumns<?> record =
                EntityRecordWithColumns.of(sampleEntityRecord(), storageFields);
        Object value = record.columnValue(columnName);

        assertThat(value).isNull();
    }

    @Nested
    @DisplayName("support being initialized with")
    class BeInitializedWith {

        @Test
        @DisplayName("an `Entity` identifier and a record")
        void idAndRecord() {
            EntityRecord rawRecord = sampleEntityRecord();
            Long id = 199L;
            EntityRecordWithColumns<Long> result = EntityRecordWithColumns.create(id, rawRecord);
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.record()).isEqualTo(rawRecord);
        }

        @Test
        @DisplayName("an `Entity` and a record")
        void entityAndRecord() {
            TestEntity entity = new TestEntity(PROJECT_ID);
            EntityRecord rawRecord = sampleEntityRecord();
            EntityRecordWithColumns<StgProjectId> result =
                    EntityRecordWithColumns.create(entity, rawRecord);
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(PROJECT_ID);
            assertThat(result.record()).isEqualTo(rawRecord);
            ImmutableSet<ColumnName> names = result.columnNames();
            ImmutableSet<ColumnName> expectedNames =
                    StgProject.Column.definitions()
                                     .stream()
                                     .map(RecordColumn::name)
                                     .collect(toImmutableSet());
            assertThat(names).containsAtLeastElementsIn(expectedNames);
            assertThat(names).containsAtLeastElementsIn(
                    ImmutableSet.of(ArchivedColumn.instance().name(),
                                    DeletedColumn.instance().name()));
        }
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("record")
        void record() {
            EntityRecordWithColumns<?> recordWithFields = sampleRecordWithEmptyColumns();
            EntityRecord record = recordWithFields.record();
            assertNotNull(record);
        }

        @Test
        @DisplayName("column values")
        void columnValues() {
            ColumnName columnName = DeletedColumn.instance()
                                                 .name();
            Object value = false;
            Map<ColumnName, Object> columnsExpected = singletonMap(columnName, value);
            EntityRecordWithColumns<?> record =
                    EntityRecordWithColumns.of(sampleEntityRecord(), columnsExpected);
            ImmutableSet<ColumnName> columnNames = record.columnNames();
            assertThat(columnNames).hasSize(1);
            assertTrue(columnNames.contains(columnName));
            assertThat(value).isEqualTo(record.columnValue(columnName));
        }
    }
}
