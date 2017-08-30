/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.Entity;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;
import io.spine.testdata.Sample;
import io.spine.time.Time;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertContains;
import static io.spine.test.Verify.assertEmpty;
import static io.spine.test.Verify.assertNotEmpty;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnsShould {

    private static final String CUSTOM_COLUMN_NAME = "columnName";
    private static final String STRING_ID = "some-string-id-never-used";

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(Columns.class);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester()
                .testStaticMethods(Columns.class,
                                   NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void extract_no_fields_if_none_defined() {
        final Entity entity = new EntityWithNoStorageFields(STRING_ID);
        final Map<String, Column.MemoizedValue> fields = Columns.from(entity);
        assertNotNull(fields);
        assertEmpty(fields);
    }

    @Test
    public void extract_fields_from_implemented_interfaces() {
        final Entity entity = new EntityWithColumnFromInterface(STRING_ID);
        final Map<String, Column.MemoizedValue> fields = Columns.from(entity);
        assertNotEmpty(fields);
    }

    @Test
    public void put_non_null_fields_to_fields_maps() {
        final EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        final Map<String, Column.MemoizedValue> fields = Columns.from(entity);
        assertNotNull(fields);

        assertSize(3, fields);

        final String floatNullKey = "floatNull";
        final Column.MemoizedValue floatMemoizedNull = fields.get(floatNullKey);
        assertNotNull(floatMemoizedNull);
        assertNull(floatMemoizedNull.getValue());

        final String intFieldKey = "integerFieldValue";
        assertEquals(entity.getIntegerFieldValue(),
                     fields.get(intFieldKey)
                           .getValue());

        final String messageKey = "someMessage";
        assertEquals(entity.getSomeMessage(),
                     fields.get(messageKey)
                           .getValue());
    }

    @Test
    public void ignore_non_public_getters_with_column_annotation_from_super_class() {
        final Entity entity = new EntityWithManyGettersDescendant(STRING_ID);
        final Map<String, Column.MemoizedValue> fields = Columns.from(entity);
        assertSize(3, fields);
    }

    @Test
    public void ignore_static_members() {
        final Map<String, Column.MemoizedValue> fields =
                Columns.from(new EntityWithManyGetters(STRING_ID));
        final Column.MemoizedValue staticValue = fields.get("staticMember");
        assertNull(staticValue);
    }

    @Test
    public void handle_non_public_entity_class() {
        final Map<?, ?> fields = Columns.from(new PrivateEntity(STRING_ID));
        assertNotNull(fields);
        assertEmpty(fields);
    }

    @Test
    public void handle_inherited_fields() {
        final Entity<?, ?> entity = new RealLifeEntity(Sample.messageOfType(ProjectId.class));
        final Map<String, ?> storageFields = Columns.from(entity);
        final Set<String> storageFieldNames = storageFields.keySet();

        assertSize(5, storageFieldNames);

        assertContains(archived.name(), storageFieldNames);
        assertContains(deleted.name(), storageFieldNames);
        assertContains("visible", storageFieldNames);
        assertContains(version.name(), storageFieldNames);
        assertContains("someTime", storageFieldNames);
    }

    @Test
    public void retrieve_column_metadata_from_given_class() {
        final Class<? extends Entity<?, ?>> entityClass = RealLifeEntity.class;
        final String existingColumnName = archived.name();
        final Column archivedColumn = Columns.findColumn(entityClass, existingColumnName);
        assertNotNull(archivedColumn);
        assertEquals(existingColumnName, archivedColumn.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_retrieve_non_existing_column() {
        final Class<? extends Entity<?, ?>> entityClass = EntityWithNoStorageFields.class;
        final String existingColumnName = "foo";
        Columns.findColumn(entityClass, existingColumnName);
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_same_column_name_within_one_entity() {
        Columns.getColumns(EntityWithRepeatedColumnNames.class);
    }

    public static class EntityWithNoStorageFields extends AbstractEntity<String, Any> {
        protected EntityWithNoStorageFields(String id) {
            super(id);
        }

        // A simple getter, which is not an entity column.
        public int getValue() {
            return 0;
        }
    }

    @SuppressWarnings("unused")  // Reflective access
    public static class EntityWithManyGetters extends AbstractEntity<String, Any> {

        private final Project someMessage = Sample.messageOfType(Project.class);

        protected EntityWithManyGetters(String id) {
            super(id);
        }

        @javax.persistence.Column
        public int getIntegerFieldValue() {
            return 0;
        }

        @Nullable
        @javax.persistence.Column
        public Float getFloatNull() {
            return null;
        }

        @javax.persistence.Column
        public Project getSomeMessage() {
            return someMessage;
        }

        @javax.persistence.Column
        int getSomeNonPublicMethod() {
            throw new AssertionError("getSomeNonPublicMethod invoked");
        }

        @javax.persistence.Column
        public void getSomeVoid() {
            throw new AssertionError("getSomeVoid invoked");
        }

        @javax.persistence.Column
        public static int getStaticMember() {
            return 1024;
        }
    }

    public static class EntityWithManyGettersDescendant extends EntityWithManyGetters {
        protected EntityWithManyGettersDescendant(String id) {
            super(id);
        }
    }

    @SuppressWarnings("unused") // Reflective access
    public static class EntityWithInvalidGetters extends AbstractEntity<String, Any> {

        protected EntityWithInvalidGetters(String id) {
            super(id);
        }

        @SuppressWarnings("ReturnOfNull") // required for the test
        public Boolean getNonNullBooleanField() {
            return null;
        }
    }

    @SuppressWarnings("unused") // Reflective access
    private static class PrivateEntity extends AbstractEntity<String, Any> {
        protected PrivateEntity(String id) {
            super(id);
        }
    }

    // Most read-life (non-test) Entities are children of AbstractVersionableEntity,
    // which brings 3 storage fields from the box.
    @SuppressWarnings("unused") // Reflective access
    public static class RealLifeEntity extends AbstractVersionableEntity<ProjectId, Project> {

        protected RealLifeEntity(ProjectId id) {
            super(id);
        }

        @javax.persistence.Column
        public Timestamp getSomeTime() {
            return Time.getCurrentTime();
        }

        @javax.persistence.Column
        public boolean isVisible() {
            return true;
        }
    }

    @SuppressWarnings("unused") // Reflective access
    public interface InterfaceWithEntityColumn {

        // The column annotation from the interface should be taken into account.
        @javax.persistence.Column
        int getIntegerFieldValue();
    }

    public static class EntityWithColumnFromInterface extends AbstractEntity<String, Any>
            implements InterfaceWithEntityColumn {
        protected EntityWithColumnFromInterface(String id) {
            super(id);
        }

        // The entity column annotation should be `inherited` from the interface.
        @Override
        public int getIntegerFieldValue() {
            return 0;
        }
    }

    public static class EntityWithRepeatedColumnNames
            extends AbstractVersionableEntity<String, Any> {
        protected EntityWithRepeatedColumnNames(String id) {
            super(id);
        }

        @javax.persistence.Column(name = CUSTOM_COLUMN_NAME)
        public int getValue() {
            return 0;
        }

        @javax.persistence.Column(name = CUSTOM_COLUMN_NAME)
        public long getLongValue() {
            return 0;
        }
    }
}
