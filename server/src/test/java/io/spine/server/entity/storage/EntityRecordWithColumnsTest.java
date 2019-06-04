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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.TestEntity;
import io.spine.server.entity.TestEntity.TestEntityBuilder;
import io.spine.server.entity.storage.given.EntityWithoutCustomColumns;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.ColumnTests.defaultColumns;
import static io.spine.server.entity.storage.Columns.extractColumnValues;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("EntityRecordWithColumns should")
class EntityRecordWithColumnsTest {

    private static EntityRecordWithColumns newRecord() {
        return EntityRecordWithColumns.of(Sample.messageOfType(EntityRecord.class),
                                          Collections.emptyMap());
    }

    private static EntityRecordWithColumns newEmptyRecord() {
        return EntityRecordWithColumns.of(EntityRecord.getDefaultInstance());
    }

    @Test
    @DisplayName("not accept nulls in constructor")
    void rejectNullInCtor() {
        new NullPointerTester()
                .setDefault(EntityRecord.class, EntityRecord.getDefaultInstance())
                .testAllPublicConstructors(EntityRecordWithColumns.class);
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        EntityRecord record = Sample.messageOfType(EntityRecord.class);
        TestEntity entity = new TestEntityBuilder().setResultClass(TestEntity.class)
                                                   .withVersion(1)
                                                   .build();
        String columnName = archived.name();
        EntityColumn column = findColumn(entity.getClass(), columnName);
        MemoizedValue value = column.memoizeFor(entity);

        Map<String, MemoizedValue> columns = singletonMap(columnName, value);
        EntityRecordWithColumns recordWithColumns = EntityRecordWithColumns.of(record, columns);
        reserializeAndAssert(recordWithColumns);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        MemoizedValue mockValue = mock(MemoizedValue.class);
        EntityRecordWithColumns noFieldsEnvelope = newEmptyRecord();
        EntityRecordWithColumns emptyFieldsEnvelope = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance(),
                Collections.emptyMap()
        );
        EntityRecordWithColumns notEmptyFieldsEnvelope =
                EntityRecordWithColumns.of(
                        EntityRecord.getDefaultInstance(),
                        singletonMap("key", mockValue)
                );
        new EqualsTester()
                .addEqualityGroup(noFieldsEnvelope, emptyFieldsEnvelope, notEmptyFieldsEnvelope)
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
            EntityRecord record = recordWithFields.getRecord();
            assertNotNull(record);
        }

        @Test
        @DisplayName("column values")
        void columnValues() {
            MemoizedValue mockValue = mock(MemoizedValue.class);
            String columnName = "some-key";
            Map<String, MemoizedValue> columnsExpected = singletonMap(columnName, mockValue);
            EntityRecordWithColumns record = EntityRecordWithColumns.of(
                    Sample.messageOfType(EntityRecord.class),
                    columnsExpected
            );
            Collection<String> columnNames = record.getColumnNames();
            assertThat(columnNames).hasSize(1);
            assertTrue(columnNames.contains(columnName));
            assertEquals(mockValue, record.getColumnValue(columnName));
        }
    }

    @Test
    @DisplayName("return empty names collection if no storage fields are set")
    void returnEmptyColumns() {
        EntityRecordWithColumns record = newEmptyRecord();
        assertFalse(record.hasColumns());
        Collection<String> names = record.getColumnNames();
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("throw ISE on attempt to get value by non-existent name")
    void throwOnNonExistentColumn() {
        EntityRecordWithColumns record = newEmptyRecord();

        assertThrows(IllegalStateException.class, () -> record.getColumnValue(""));
    }

    @Test
    @DisplayName("not have only lifecycle columns if the entity does not define custom")
    void supportEmptyColumns() {
        EntityWithoutCustomColumns entity = new EntityWithoutCustomColumns("ID");
        Class<? extends Entity<?, ?>> entityClass = entity.getClass();
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(entityClass);
        Map<String, MemoizedValue> columnValues = extractColumnValues(entity, entityColumns);

        assertThat(columnValues).hasSize(defaultColumns.size());

        EntityRecordWithColumns record = EntityRecordWithColumns.of(
                EntityRecord.getDefaultInstance(),
                columnValues
        );
        assertThat(record.getColumnNames()).containsExactlyElementsIn(defaultColumns);
    }
}
