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

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.protobuf.Any;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGetters;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithNoStorageFields;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collection;
import java.util.Map;

import static io.spine.server.entity.storage.Columns.getAllColumns;
import static io.spine.server.entity.storage.given.ColumnsTestEnv.CUSTOM_COLUMN_NAME;
import static io.spine.test.Verify.assertEmpty;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Dmytro Kuzmin
 */
public class ColumnValueExtractorShould {

    private static final String TEST_ENTITY_ID = "some-string-id-never-used";

    @Test
    @DisplayName("pass null check")
    void passNullCheck() {
        new NullPointerTester().testStaticMethods(ColumnValueExtractor.class, Visibility.PACKAGE);
    }

    @Test
    @DisplayName("extract column values from entity")
    void extractColumnValuesFromEntity() {
        EntityWithManyGetters entity = new EntityWithManyGetters(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertSize(3, columnValues);
        assertEquals(entity.getSomeMessage(),
                columnValues.get("someMessage")
                        .getValue());
    }

    @Test
    @DisplayName("handle null column values")
    void handleNullColumnValues() {
        EntityWithManyGetters entity = new EntityWithManyGetters(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        EntityColumn.MemoizedValue memoizedFloatNull = columnValues.get("floatNull");
        assertNotNull(memoizedFloatNull);
        assertNull(memoizedFloatNull.getValue());
    }

    @Test
    @DisplayName("allow to access values by custom column name")
    void allowToAccessValuesByCustomColumnName() {
        EntityWithManyGetters entity = new EntityWithManyGetters(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertEquals(entity.getIntegerFieldValue(),
                columnValues.get(CUSTOM_COLUMN_NAME)
                        .getValue());
    }

    @Test
    @DisplayName("extract no fields if none defined")
    void extractNoFieldsIfNoneDefined() {
        Entity entity = new EntityWithNoStorageFields(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertNotNull(columnValues);
        assertEmpty(columnValues);
    }

    @Test
    @DisplayName("handle non public entity class")
    void handleNonPublicEntityClass() {
        Entity entity = new PrivateEntity(TEST_ENTITY_ID);
        Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertNotNull(columnValues);
        assertEmpty(columnValues);
    }


    private static <E extends Entity<?, ?>>
    Map<String, EntityColumn.MemoizedValue> extractColumnValues(E entity) {
        Collection<EntityColumn> entityColumns = getAllColumns(entity.getClass());
        ColumnValueExtractor columnValueExtractor = ColumnValueExtractor.create(entity, entityColumns);
        return columnValueExtractor.extractColumnValues();
    }

    @SuppressWarnings("unused") // Reflective access
    private static class PrivateEntity extends AbstractEntity<String, Any> {
        protected PrivateEntity(String id) {
            super(id);
        }
    }
}
