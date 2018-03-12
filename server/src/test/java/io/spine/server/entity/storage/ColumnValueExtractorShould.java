/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

    private static final String STRING_ID = "some-string-id-never-used";

    @Test
    public void pass_null_check() {
        new NullPointerTester().testStaticMethods(ColumnValueExtractor.class, Visibility.PACKAGE);
    }

    @Test
    public void extract_column_values_from_entity() {
        final EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        final Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertSize(3, columnValues);
        assertEquals(entity.getSomeMessage(),
                columnValues.get("someMessage")
                        .getValue());
    }

    @Test
    public void handle_null_column_values() {
        final EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        final Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        final EntityColumn.MemoizedValue memoizedFloatNull = columnValues.get("floatNull");
        assertNotNull(memoizedFloatNull);
        assertNull(memoizedFloatNull.getValue());
    }

    @Test
    public void allow_access_values_by_custom_column_name() {
        final EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        final Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertEquals(entity.getIntegerFieldValue(),
                columnValues.get(CUSTOM_COLUMN_NAME)
                        .getValue());
    }

    @Test
    public void extract_no_fields_if_none_defined() {
        final Entity entity = new EntityWithNoStorageFields(STRING_ID);
        final Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertNotNull(columnValues);
        assertEmpty(columnValues);
    }

    @Test
    public void handle_non_public_entity_class() {
        final Entity entity = new PrivateEntity(STRING_ID);
        final Map<String, EntityColumn.MemoizedValue> columnValues = extractColumnValues(entity);

        assertNotNull(columnValues);
        assertEmpty(columnValues);
    }


    private static <E extends Entity<?, ?>> Map<String, EntityColumn.MemoizedValue> extractColumnValues(E entity) {
        final Collection<EntityColumn> entityColumns = getAllColumns(entity.getClass());
        final ColumnValueExtractor columnValueExtractor = ColumnValueExtractor.create(entity, entityColumns);
        return columnValueExtractor.extractColumnValues();
    }

    @SuppressWarnings("unused") // Reflective access
    private static class PrivateEntity extends AbstractEntity<String, Any> {
        protected PrivateEntity(String id) {
            super(id);
        }
    }
}
