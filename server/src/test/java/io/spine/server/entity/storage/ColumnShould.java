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

import com.google.common.testing.EqualsTester;
import com.google.protobuf.Any;
import io.spine.core.Version;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.EntityWithLifecycle;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.entity.given.Given;
import org.junit.Test;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Many string literals for method names
public class ColumnShould {

    private static final String CUSTOM_COLUMN_NAME = " customColumnName ";
    private static final String INVALID_COLUMN_NAME = " * ";

    @Test
    public void be_serializable() {
        final Column column = forMethod("getVersion", VersionableEntity.class);
        reserializeAndAssert(column);
    }

    @Test
    public void support_toString() {
        final Column column = forMethod("getVersion", VersionableEntity.class);
        assertEquals("VersionableEntity.version", column.toString());
    }

    @Test
    public void have_name_for_storing_from_annotation_property() {

    }

    @Test
    public void invoke_getter() {
        final String entityId = "entity-id";
        final int version = 2;
        final Column column = forMethod("getVersion", VersionableEntity.class);
        final TestEntity entity = Given.entityOfClass(TestEntity.class)
                                       .withId(entityId)
                                       .withVersion(version)
                                       .build();
        final Version actualVersion = (Version) column.getFor(entity);
        assertEquals(version, actualVersion.getNumber());
    }

    @Test
    public void have_equals_and_hashCode() {
        final Column col1 = forMethod("getVersion", VersionableEntity.class);
        final Column col2 = forMethod("getVersion", VersionableEntity.class);
        final Column col3 = forMethod("isDeleted", EntityWithLifecycle.class);
        new EqualsTester()
                .addEqualityGroup(col1, col2)
                .addEqualityGroup(col3)
                .testEquals();
    }

    @Test
    public void memoize_value_at_at_point_in_time() {
        final Column mutableColumn = forMethod("getMutableState", TestEntity.class);
        final TestEntity entity = new TestEntity("");
        final int initialState = 1;
        final int changedState = 42;
        entity.setMutableState(initialState);
        final Column.MemoizedValue memoizedState = mutableColumn.memoizeFor(entity);
        entity.setMutableState(changedState);
        final int extractedState = (int) mutableColumn.getFor(entity);

        final Integer value = (Integer) memoizedState.getValue();
        assertNotNull(value);
        assertEquals(initialState, value.intValue());
        assertEquals(changedState, extractedState);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_be_constructed_from_non_getter() {
        forMethod("toString", Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_be_constructed_from_non_annotated_getter() {
        forMethod("getClass", Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_be_constructed_from_static_method() {
        forMethod("getStatic", TestEntity.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_be_constructed_from_private_getter() {
        forMethod("getFortyTwoLong", TestEntity.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_be_constructed_from_getter_with_parameters() throws NoSuchMethodException {
        final Method method = TestEntity.class.getDeclaredMethod("getParameter",
                                                                 String.class);
        Column.from(method);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_construct_for_non_serializable_column() {
        forMethod("getFoo", BrokenTestEntity.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_get_value_from_wrong_object() {
        final Column column = forMethod("getMutableState", TestEntity.class);
        column.getFor(new EntityWithCustomColumnNameForStoring(""));
    }

    @Test(expected = NullPointerException.class)
    public void check_value_if_getter_is_not_null() {
        final Column column = forMethod("getNotNull", TestEntity.class);
        column.getFor(new TestEntity(""));
    }

    @Test
    public void allow_nulls_if_getter_is_nullable() {
        final Column column = forMethod("getNull", TestEntity.class);
        final Object value = column.getFor(new TestEntity(""));
        assertNull(value);
    }

    @Test
    public void tell_if_property_is_nullable() {
        final Column notNullColumn = forMethod("getNotNull", TestEntity.class);
        final Column nullableColumn = forMethod("getNull", TestEntity.class);

        assertFalse(notNullColumn.isNullable());
        assertTrue(nullableColumn.isNullable());
    }

    @Test
    public void contain_property_type() {
        final Column column = forMethod("getLong", TestEntity.class);
        assertEquals(Long.TYPE, column.getType());
    }

    @Test
    public void memoize_value_regarding_nulls() {
        final Column nullableColumn = forMethod("getNull", TestEntity.class);
        final Column.MemoizedValue memoizedNull = nullableColumn.memoizeFor(new TestEntity(""));
        assertTrue(memoizedNull.isNull());
        assertNull(memoizedNull.getValue());
    }

    @Test
    public void memoize_value_which_has_reference_on_Column_itself() {
        final Column column = forMethod("getMutableState", TestEntity.class);
        final TestEntity entity = new TestEntity("");
        final Column.MemoizedValue memoizedValue = column.memoizeFor(entity);
        assertSame(column, memoizedValue.getSourceColumn());
    }

    @Test
    public void have_valid_name_for_querying_and_storing() {
        final Column column = forMethod("getValue", EntityWithCustomColumnNameForStoring.class);
        assertEquals("value", column.getName());
        assertEquals(CUSTOM_COLUMN_NAME.trim(), column.getStoredName());
    }

    @Test
    public void have_same_names_for_and_querying_storing_if_last_is_not_specified() {
        final Column column = forMethod("getValue", EntityWithDefaultColumnNameForStoring.class);
        final String expectedName = "value";
        assertEquals(expectedName, column.getName());
        assertEquals(expectedName, column.getStoredName());
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_redefine_column_annotation() {
        forMethod("getValue", EntityRedefiningColumnAnnotation.class);
    }

    private static Column forMethod(String name, Class<?> enclosingClass) {
        try {
            final Method result = enclosingClass.getDeclaredMethod(name);
            return Column.from(result);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestEntity extends AbstractVersionableEntity<String, Any> {

        private int mutableState = 0;

        protected TestEntity(String id) {
            super(id);
        }

        @EntityColumn
        public int getMutableState() {
            return mutableState;
        }

        public void setMutableState(int mutableState) {
            this.mutableState = mutableState;
        }

        @EntityColumn
        public String getNotNull() {
            return null;
        }

        @Nullable
        @EntityColumn
        public String getNull() {
            return null;
        }

        @EntityColumn
        public long getLong() {
            return 0;
        }

        @EntityColumn
        private long getFortyTwoLong() {
            return 42L;
        }

        @EntityColumn
        public String getParameter(String param) {
            return param;
        }

        @EntityColumn
        public static String getStatic() {
            return "";
        }
    }

    public static class BrokenTestEntity extends AbstractEntity<String, Any> {
        protected BrokenTestEntity(String id) {
            super(id);
        }

        // non-serializable Entity Column
        @EntityColumn
        public Object getFoo() {
            fail("Invoked a getter for a non-serializable Entity Column BrokenTestEntity.foo");
            return new Object();
        }
    }

    public static class EntityWithCustomColumnNameForStoring
            extends AbstractVersionableEntity<String, Any> {
        protected EntityWithCustomColumnNameForStoring(String id) {
            super(id);
        }

        @EntityColumn(name = CUSTOM_COLUMN_NAME)
        public int getValue() {
            return 0;
        }
    }

    public static class EntityRedefiningColumnAnnotation extends EntityWithCustomColumnNameForStoring {
        protected EntityRedefiningColumnAnnotation(String id) {
            super(id);
        }

        @EntityColumn
        @Override
        public int getValue() {
            return super.getValue();
        }
    }

    public static class EntityWithDefaultColumnNameForStoring
            extends AbstractVersionableEntity<String, Any> {
        protected EntityWithDefaultColumnNameForStoring(String id) {
            super(id);
        }

        @EntityColumn
        public int getValue() {
            return 0;
        }
    }
}
