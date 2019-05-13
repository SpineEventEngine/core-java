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

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.protobuf.Any;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.given.column.EntityWithManyGetters;
import io.spine.server.entity.storage.given.column.EntityWithNoStorageFields;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.ColumnTests.defaultColumns;
import static io.spine.server.entity.storage.Columns.getAllColumns;
import static io.spine.server.entity.storage.given.column.EntityWithManyGetters.CUSTOM_COLUMN_NAME;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("ColumnValueExtractor should")
class ColumnValueExtractorTest {

    private static final String TEST_ENTITY_ID = "some-string-id-never-used";

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testStaticMethods(ColumnValueExtractor.class, Visibility.PACKAGE);
    }

    @Nested
    @DisplayName("extract column values from")
    class ExtractColumnValues {

        @Test
        @DisplayName("public entity")
        void publicEntity() {
            EntityWithManyGetters entity = new EntityWithManyGetters(TEST_ENTITY_ID);
            Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

            assertEquals(entity.getSomeMessage(), columnValues.get("someMessage")
                                                              .getValue());
        }

        @Test
        @DisplayName("non-public entity")
        void nonPublicEntity() {
            PrivateEntity entity = new PrivateEntity(TEST_ENTITY_ID);
            Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

            assertEquals(entity.getIntValue(), columnValues.get("intValue")
                                                           .getValue());
        }
    }

    @Test
    @DisplayName("handle null column values")
    void handleNullValues() {
        EntityWithManyGetters entity = new EntityWithManyGetters(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        EntityColumn.MemoizedValue memoizedFloatNull = columnValues.get("floatNull");
        assertNotNull(memoizedFloatNull);
        assertNull(memoizedFloatNull.getValue());
    }

    @Test
    @DisplayName("extract values by custom column name")
    void extractByCustomName() {
        EntityWithManyGetters entity = new EntityWithManyGetters(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertEquals(entity.getIntegerFieldValue(),
                     columnValues.get(CUSTOM_COLUMN_NAME)
                                 .getValue());
    }

    @Test
    @DisplayName("extract standard fields if no custom fields defined")
    void handleNoneDefined() {
        Entity entity = new EntityWithNoStorageFields(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertNotNull(columnValues);
        assertThat(columnValues)
                .hasSize(defaultColumns.size());
        assertThat(columnValues.keySet())
                .containsExactlyElementsIn(defaultColumns);
    }

    private static <E extends Entity<?, ?>>
    Map<String, EntityColumn.MemoizedValue> extractColumnValues(E entity) {
        @SuppressWarnings("unchecked") // Only erasure type is available from `getClass()`.
        Class<? extends Entity<?, ?>> entityClass =
                (Class<? extends Entity<?, ?>>) entity.getClass();
        Collection<EntityColumn> entityColumns = getAllColumns(entityClass);
        ColumnValueExtractor columnValueExtractor =
                ColumnValueExtractor.create(entity, entityColumns);
        return columnValueExtractor.extractColumnValues();
    }

    @SuppressWarnings("WeakerAccess") // Entity column should be public.
    private static class PrivateEntity extends AbstractEntity<String, Any> {
        PrivateEntity(String id) {
            super(id);
        }

        @Column
        public int getIntValue() {
            return 42;
        }
    }
}
