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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.entity.given.Given;
import io.spine.test.entity.Project;
import io.spine.testdata.Sample;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.server.entity.storage.Columns.extractColumnValues;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import static io.spine.server.entity.storage.EntityRecordWithColumns.of;
import static io.spine.server.storage.EntityField.version;
import static io.spine.test.Verify.assertSize;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Dmytro Dashenkov
 */
public class EntityRecordWithColumnsShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static EntityRecordWithColumns newRecord() {
        return of(Sample.messageOfType(EntityRecord.class),
                  Collections.emptyMap());
    }

    private static EntityRecordWithColumns newEmptyRecord() {
        return of(EntityRecord.getDefaultInstance());
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        EntityRecord record = Sample.messageOfType(EntityRecord.class);
        VersionableEntity<?, ?> entity = Given.entityOfClass(TestEntity.class)
                                              .withVersion(1)
                                              .build();
        String columnName = version.name();
        EntityColumn column = findColumn(VersionableEntity.class, columnName);
        MemoizedValue value = column.memoizeFor(entity);

        Map<String, MemoizedValue> columns = singletonMap(columnName, value);
        EntityRecordWithColumns recordWithColumns = of(record, columns);
        reserializeAndAssert(recordWithColumns);
    }

    @Test
    @DisplayName("initialize with record and storage fields")
    void initializeWithRecordAndStorageFields() {
        EntityRecordWithColumns record = of(EntityRecord.getDefaultInstance(),
                                            Collections.emptyMap());
        assertNotNull(record);
    }

    @Test
    @DisplayName("initialize with record only")
    void initializeWithRecordOnly() {
        EntityRecordWithColumns record = newEmptyRecord();
        assertNotNull(record);
    }

    @Test
    @DisplayName("not accept nulls in ctor")
    void notAcceptNullsInCtor() {
        new NullPointerTester()
                .setDefault(EntityRecord.class, EntityRecord.getDefaultInstance())
                .testAllPublicConstructors(EntityRecordWithColumns.class);
    }

    @Test
    @DisplayName("store record")
    void storeRecord() {
        EntityRecordWithColumns recordWithFields = newRecord();
        EntityRecord record = recordWithFields.getRecord();
        assertNotNull(record);
    }

    @Test
    @DisplayName("store column values")
    void storeColumnValues() {
        MemoizedValue mockValue = mock(MemoizedValue.class);
        String columnName = "some-key";
        Map<String, MemoizedValue> columnsExpected = singletonMap(columnName, mockValue);
        EntityRecordWithColumns record = of(Sample.messageOfType(EntityRecord.class),
                                            columnsExpected);
        Collection<String> columnNames = record.getColumnNames();
        assertSize(1, columnNames);
        assertTrue(columnNames.contains(columnName));
        assertEquals(mockValue, record.getColumnValue(columnName));
    }

    @Test
    @DisplayName("return empty names collection if no storage fields")
    void returnEmptyNamesCollectionIfNoStorageFields() {
        EntityRecordWithColumns record = newEmptyRecord();
        assertFalse(record.hasColumns());
        Collection<String> names = record.getColumnNames();
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("throw on attempt to get value by non existent name")
    void throwOnAttemptToGetValueByNonExistentName() {
        EntityRecordWithColumns record = newEmptyRecord();

        thrown.expect(IllegalStateException.class);
        record.getColumnValue("");
    }

    @Test
    @DisplayName("not have columns if values list is empty")
    void notHaveColumnsIfValuesListIsEmpty() {
        EntityWithoutColumns entity = new EntityWithoutColumns("ID");
        Class<? extends Entity> entityClass = entity.getClass();
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(entityClass);
        Map<String, MemoizedValue> columnValues = extractColumnValues(entity, entityColumns);
        assertTrue(columnValues.isEmpty());

        EntityRecordWithColumns record = of(EntityRecord.getDefaultInstance(), columnValues);
        assertFalse(record.hasColumns());
    }

    @Test
    @DisplayName("have equals")
    void haveEquals() {
        MemoizedValue mockValue = mock(MemoizedValue.class);
        EntityRecordWithColumns noFieldsEnvelope = newEmptyRecord();
        EntityRecordWithColumns emptyFieldsEnvelope = of(
                EntityRecord.getDefaultInstance(),
                Collections.emptyMap()
        );
        EntityRecordWithColumns notEmptyFieldsEnvelope =
                of(
                        EntityRecord.getDefaultInstance(),
                        singletonMap("key", mockValue)
                );
        new EqualsTester()
                .addEqualityGroup(noFieldsEnvelope, emptyFieldsEnvelope, notEmptyFieldsEnvelope)
                .addEqualityGroup(newRecord())
                .addEqualityGroup(newRecord()) // Each one has different EntityRecord
                .testEquals();
    }

    private static class TestEntity extends AbstractVersionableEntity<String, Project> {

        private TestEntity(String id) {
            super(id);
        }
    }

    private static class EntityWithoutColumns extends AbstractEntity<String, Any> {
        private EntityWithoutColumns(String id) {
            super(id);
        }
    }
}
