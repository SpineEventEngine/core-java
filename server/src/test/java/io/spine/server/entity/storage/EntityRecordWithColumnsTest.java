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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.given.EntityWithoutCustomColumns;
import io.spine.server.entity.storage.given.TestStorageRules;
import io.spine.test.storage.TaskId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.ColumnTests.defaultColumns;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`EntityRecordWithColumns` should")
class EntityRecordWithColumnsTest {

    private static EntityRecordWithColumns newRecord() {
        return EntityRecordWithColumns.of(Sample.messageOfType(EntityRecord.class),
                                          Collections.emptyMap());
    }

    private static EntityRecordWithColumns newEmptyRecord() {
        return EntityRecordWithColumns.of(EntityRecord.getDefaultInstance(),
                                          Collections.emptyMap());
    }

    @Test
    @DisplayName("not accept `null`s in constructor")
    void rejectNullInCtor() {
        new NullPointerTester()
                .setDefault(EntityRecord.class, EntityRecord.getDefaultInstance())
                .testAllPublicConstructors(EntityRecordWithColumns.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        ColumnName columnName = ColumnName.of(archived.name());
        Object value = false;
        EntityRecordWithColumns emptyFieldsEnvelope = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance(),
                Collections.emptyMap()
        );
        EntityRecordWithColumns notEmptyFieldsEnvelope =
                EntityRecordWithColumns.of(
                        EntityRecord.getDefaultInstance(),
                        singletonMap(columnName, value)
                );
        new EqualsTester()
                .addEqualityGroup(emptyFieldsEnvelope, notEmptyFieldsEnvelope)
                .addEqualityGroup(newRecord())
                .addEqualityGroup(newRecord()) // Each one has different EntityRecord
                .testEquals();
    }

    @Nested
    @DisplayName("support being initialized with")
    class BeInitializedWith {

        @Test
        @DisplayName("record and storage fields")
        void recordAndColumns() {
            EntityRecordWithColumns record = EntityRecordWithColumns.of(
                    EntityRecord.getDefaultInstance(),
                    Collections.emptyMap()
            );
            assertNotNull(record);
        }

        @Test
        @DisplayName("record only")
        void recordOnly() {
            EntityRecordWithColumns record = newEmptyRecord();
            assertNotNull(record);
        }
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("record")
        void record() {
            EntityRecordWithColumns recordWithFields = newRecord();
            EntityRecord record = recordWithFields.record();
            assertNotNull(record);
        }

        @Test
        @DisplayName("column values")
        void columnValues() {
            ColumnName columnName = ColumnName.of(archived.name());
            Object value = false;
            Map<ColumnName, Object> columnsExpected = singletonMap(columnName, value);
            EntityRecordWithColumns record = EntityRecordWithColumns.of(
                    Sample.messageOfType(EntityRecord.class),
                    columnsExpected
            );
            ImmutableSet<ColumnName> columnNames = record.columnNames();
            assertThat(columnNames).hasSize(1);
            assertTrue(columnNames.contains(columnName));
            assertEquals(value, record.columnValue(columnName));
        }
    }

    @Test
    @DisplayName("return empty names collection if no storage fields are set")
    void returnEmptyColumns() {
        EntityRecordWithColumns record = newEmptyRecord();
        assertFalse(record.hasColumns());
        ImmutableSet<ColumnName> names = record.columnNames();
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("throw `ISE` on attempt to get value by non-existent name")
    void throwOnNonExistentColumn() {
        EntityRecordWithColumns record = newEmptyRecord();
        ColumnName nonExistentName = ColumnName.of("non-existent-column");
        assertThrows(IllegalStateException.class, () -> record.columnValue(nonExistentName));
    }

    @Test
    @DisplayName("have default system columns even if the entity does not define custom ones")
    void supportEmptyColumns() {
        TaskId taskId = TaskId
                .newBuilder()
                .setId(42)
                .build();
        EntityWithoutCustomColumns entity = new EntityWithoutCustomColumns(taskId);

        Columns columns = Columns.of(entity.getClass());
        Map<ColumnName, Object> storageFields = columns.valuesIn(entity);

        EntityRecordWithColumns record = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance(),
                storageFields
        );
        assertThat(record.columnNames()).containsExactlyElementsIn(defaultColumns);
    }

    @Test
    @DisplayName("return column value by column name")
    void returnColumnValue() {
        ColumnName columnName = ColumnName.of("some-boolean-column");
        boolean columnValue = false;
        ImmutableMap<ColumnName, Object> storageFields = ImmutableMap.of(columnName, columnValue);
        EntityRecordWithColumns record =
                EntityRecordWithColumns.of(EntityRecord.getDefaultInstance(), storageFields);
        Object value = record.columnValue(columnName);

        assertThat(value).isEqualTo(columnValue);
    }

    @Test
    @DisplayName("return a column value for storage")
    void returnValueWithStorageRules() {
        ColumnName columnName = ColumnName.of("some-int-column");
        int columnValue = 42;

        ImmutableMap<ColumnName, Object> storageFields = ImmutableMap.of(columnName, columnValue);
        EntityRecordWithColumns record =
                EntityRecordWithColumns.of(EntityRecord.getDefaultInstance(), storageFields);
        String value = record.columnValue(columnName, new TestStorageRules());

        assertThat(value).isEqualTo(String.valueOf(columnValue));
    }

    @Test
    @DisplayName("return `null` column value")
    void returnNullValue() {
        ColumnName columnName = ColumnName.of("the-null-column");
        Map<ColumnName, Object> storageFields = new HashMap<>();
        storageFields.put(columnName, null);
        EntityRecordWithColumns record =
                EntityRecordWithColumns.of(EntityRecord.getDefaultInstance(), storageFields);
        Object value = record.columnValue(columnName);

        assertThat(value).isNull();
    }
}
