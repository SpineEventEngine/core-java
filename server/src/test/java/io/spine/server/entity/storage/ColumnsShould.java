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
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGetters;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithNoStorageFields;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithRepeatedColumnNames;
import io.spine.server.entity.storage.given.ColumnsTestEnv.RealLifeEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Map;

import static io.spine.server.entity.storage.Columns.extractColumnValues;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.Columns.getAllColumns;
import static io.spine.server.entity.storage.given.ColumnsTestEnv.CUSTOM_COLUMN_NAME;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnsShould {

    private static final String STRING_ID = "some-string-id-never-used";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Helper method that checks all {@link EntityWithManyGetters} field values.
     *
     * <p>Created to avoid code duplication, as {@link EntityWithManyGetters} is
     * the main {@link Entity} class used to test
     * {@linkplain Columns#extractColumnValues(Entity, Collection) column extraction}
     * functionality.
     */
    private
    static void checkEntityWithManyGettersFields(EntityWithManyGetters entity,
                                                 Map<String, MemoizedValue> fields) {
        assertNotNull(fields);

        assertSize(3, fields);

        String floatNullKey = "floatNull";
        MemoizedValue floatMemoizedNull = fields.get(floatNullKey);
        assertNotNull(floatMemoizedNull);
        assertNull(floatMemoizedNull.getValue());

        assertEquals(entity.getIntegerFieldValue(),
                     fields.get(CUSTOM_COLUMN_NAME)
                           .getValue());

        String messageKey = "someMessage";
        assertEquals(entity.getSomeMessage(),
                     fields.get(messageKey)
                           .getValue());
    }

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(Columns.class);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester().testStaticMethods(Columns.class, Visibility.PACKAGE);
    }

    @Test
    public void get_all_valid_columns_for_entity_class() {
        Collection<EntityColumn> entityColumns = getAllColumns(EntityWithManyGetters.class);

        assertNotNull(entityColumns);
        assertSize(3, entityColumns);
    }

    @Test
    public void fail_to_obtain_columns_for_invalid_entity_class() {
        thrown.expect(IllegalStateException.class);
        getAllColumns(EntityWithRepeatedColumnNames.class);
    }

    @Test
    public void retrieve_specific_column_metadata_from_given_class() {
        Class<? extends Entity<?, ?>> entityClass = RealLifeEntity.class;
        String existingColumnName = archived.name();
        EntityColumn archivedColumn = findColumn(entityClass, existingColumnName);
        assertNotNull(archivedColumn);
        assertEquals(existingColumnName, archivedColumn.getName());
    }

    @Test
    public void fail_to_retrieve_non_existing_column() {
        Class<? extends Entity<?, ?>> entityClass = EntityWithNoStorageFields.class;
        String nonExistingColumnName = "foo";

        thrown.expect(IllegalArgumentException.class);
        findColumn(entityClass, nonExistingColumnName);
    }

    @Test
    public void extract_column_values_with_names_for_storing() {
        EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        Collection<EntityColumn> entityColumns = getAllColumns(entity.getClass());
        Map<String, MemoizedValue> fields = extractColumnValues(entity, entityColumns);

        checkEntityWithManyGettersFields(entity, fields);
    }

    @Test
    public void extract_column_values_using_predefined_columns() {
        EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        Collection<EntityColumn> entityColumns = getAllColumns(entity.getClass());
        Map<String, MemoizedValue> fields = extractColumnValues(entity, entityColumns);

        checkEntityWithManyGettersFields(entity, fields);
    }

    @SuppressWarnings("unused") // Reflective access
    private static class PrivateEntity extends AbstractEntity<String, Any> {
        protected PrivateEntity(String id) {
            super(id);
        }
    }
}
