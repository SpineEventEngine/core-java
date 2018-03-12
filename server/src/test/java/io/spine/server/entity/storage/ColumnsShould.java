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
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithRepeatedColumnNames;
import io.spine.server.entity.storage.given.ColumnsTestEnv.RealLifeEntity;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static io.spine.server.entity.storage.Columns.checkColumnDefinitions;
import static io.spine.server.entity.storage.Columns.extractColumnValues;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.given.ColumnsTestEnv.CUSTOM_COLUMN_NAME;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Dmytro Dashenkov
 * @author Dmytro Kuzmin
 */
public class ColumnsShould {

    private static final String STRING_ID = "some-string-id-never-used";

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(Columns.class);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester().testStaticMethods(Columns.class, Visibility.PACKAGE);
    }

    @Test
    public void pass_column_definitions_check_for_correct_entity() {
        checkColumnDefinitions(EntityWithManyGetters.class);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_column_definitions_check_for_incorrect_entity() {
        checkColumnDefinitions(EntityWithRepeatedColumnNames.class);
    }

    @Test
    public void get_all_valid_columns_for_entity_class() {
        final Collection<EntityColumn> entityColumns = Columns.getAllColumns(EntityWithManyGetters.class);

        assertNotNull(entityColumns);
        assertSize(3, entityColumns);
    }

    @Test
    public void retrieve_specific_column_metadata_from_given_class() {
        final Class<? extends Entity<?, ?>> entityClass = RealLifeEntity.class;
        final String existingColumnName = archived.name();
        final EntityColumn archivedColumn = findColumn(entityClass, existingColumnName);
        assertNotNull(archivedColumn);
        assertEquals(existingColumnName, archivedColumn.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_retrieve_non_existing_column() {
        final Class<? extends Entity<?, ?>> entityClass = EntityWithNoStorageFields.class;
        final String nonExistingColumnName = "foo";
        findColumn(entityClass, nonExistingColumnName);
    }

    @Test
    public void extract_column_values_with_names_for_storing() {
        final EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        final Map<String, EntityColumn.MemoizedValue> fields = extractColumnValues(entity);
        assertNotNull(fields);

        assertSize(3, fields);

        final String floatNullKey = "floatNull";
        final EntityColumn.MemoizedValue floatMemoizedNull = fields.get(floatNullKey);
        assertNotNull(floatMemoizedNull);
        assertNull(floatMemoizedNull.getValue());

        assertEquals(entity.getIntegerFieldValue(),
                fields.get(CUSTOM_COLUMN_NAME)
                        .getValue());

        final String messageKey = "someMessage";
        assertEquals(entity.getSomeMessage(),
                fields.get(messageKey)
                        .getValue());
    }

    @Test
    public void extract_column_values_using_predefined_columns() {
        final EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        final Collection<EntityColumn> entityColumns = Columns.getAllColumns(entity.getClass());
        final Map<String, EntityColumn.MemoizedValue> fields = extractColumnValues(entity, entityColumns);
        assertNotNull(fields);

        assertSize(3, fields);

        final String floatNullKey = "floatNull";
        final EntityColumn.MemoizedValue floatMemoizedNull = fields.get(floatNullKey);
        assertNotNull(floatMemoizedNull);
        assertNull(floatMemoizedNull.getValue());

        assertEquals(entity.getIntegerFieldValue(),
                fields.get(CUSTOM_COLUMN_NAME)
                        .getValue());

        final String messageKey = "someMessage";
        assertEquals(entity.getSomeMessage(),
                fields.get(messageKey)
                        .getValue());
    }

    @SuppressWarnings("unused") // Reflective access
    private static class PrivateEntity extends AbstractEntity<String, Any> {
        protected PrivateEntity(String id) {
            super(id);
        }
    }
}
