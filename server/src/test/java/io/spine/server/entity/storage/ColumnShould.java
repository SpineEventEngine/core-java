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
import com.google.protobuf.Any;
import io.spine.core.Version;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityWithLifecycle;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.server.entity.storage.given.ColumnTestEnv.BrokenTestEntity;
import io.spine.server.entity.storage.given.ColumnTestEnv.EntityRedefiningColumnAnnotation;
import io.spine.server.entity.storage.given.ColumnTestEnv.EntityWithDefaultColumnNameForStoring;
import io.spine.server.entity.storage.given.ColumnTestEnv.TestEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.server.entity.storage.given.ColumnTestEnv.TaskStatus.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Many string literals for method names
public class ColumnShould {

    private static final String CUSTOM_COLUMN_NAME = " customColumnName ";

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test
    public void be_serializable() {
        EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        reserializeAndAssert(column);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Just check that operation passes without an exception.
    @Test
    public void restore_getter_when_its_not_null_without_errors() {
        final EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        column.restoreGetter();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Just check that operation passes without an exception.
    @Test
    public void restore_value_converter_when_its_not_null_without_errors() {
        final EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        column.restoreValueConverter();
    }

    @Test
    public void support_toString() {
        EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        assertEquals("VersionableEntity.version", column.toString());
    }

    @Test
    public void invoke_getter() {
        String entityId = "entity-id";
        int version = 2;
        EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        TestEntity entity = Given.entityOfClass(TestEntity.class)
                                       .withId(entityId)
                                       .withVersion(version)
                                       .build();
        Version actualVersion = (Version) column.getFor(entity);
        assertEquals(version, actualVersion.getNumber());
    }

    @Test
    public void have_equals_and_hashCode() {
        EntityColumn col1 = forMethod("getVersion", VersionableEntity.class);
        EntityColumn col2 = forMethod("getVersion", VersionableEntity.class);
        EntityColumn col3 = forMethod("isDeleted", EntityWithLifecycle.class);
        new EqualsTester()
                .addEqualityGroup(col1, col2)
                .addEqualityGroup(col3)
                .testEquals();
    }

    @Test
    public void memoize_value_at_at_point_in_time() {
        EntityColumn mutableColumn = forMethod("getMutableState", TestEntity.class);
        TestEntity entity = new TestEntity("");
        int initialState = 1;
        int changedState = 42;
        entity.setMutableState(initialState);
        MemoizedValue memoizedState = mutableColumn.memoizeFor(entity);
        entity.setMutableState(changedState);
        int extractedState = (int) mutableColumn.getFor(entity);

        Integer value = (Integer) memoizedState.getValue();
        assertNotNull(value);
        assertEquals(initialState, value.intValue());
        assertEquals(changedState, extractedState);
    }

    @Test
    public void not_be_constructed_from_non_getter() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("toString", Object.class);
    }

    @Test
    public void not_be_constructed_from_non_annotated_getter() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getClass", Object.class);
    }

    @Test
    public void not_be_constructed_from_static_method() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getStatic", TestEntity.class);
    }

    @Test
    public void not_be_constructed_from_private_getter() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getFortyTwoLong", TestEntity.class);
    }

    @Test
    public void not_be_constructed_from_getter_with_parameters() throws NoSuchMethodException {
        Method method = TestEntity.class.getDeclaredMethod("getParameter",
                                                                 String.class);
        thrown.expect(IllegalArgumentException.class);
        EntityColumn.from(method);
    }

    @Test
    public void fail_to_construct_for_non_serializable_column() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getFoo", BrokenTestEntity.class);
    }

    @Test
    public void fail_to_get_value_from_wrong_object() {
        EntityColumn column = forMethod("getMutableState", TestEntity.class);

        thrown.expect(IllegalArgumentException.class);
        column.getFor(new EntityWithCustomColumnNameForStoring(""));
    }

    @Test
    public void check_value_if_getter_is_not_null() {
        EntityColumn column = forMethod("getNotNull", TestEntity.class);
        
        thrown.expect(NullPointerException.class);
        column.getFor(new TestEntity(""));
    }

    @Test
    public void allow_nulls_if_getter_is_nullable() {
        EntityColumn column = forMethod("getNull", TestEntity.class);
        Object value = column.getFor(new TestEntity(""));
        assertNull(value);
    }

    @Test
    public void tell_if_property_is_nullable() {
        EntityColumn notNullColumn = forMethod("getNotNull", TestEntity.class);
        EntityColumn nullableColumn = forMethod("getNull", TestEntity.class);

        assertFalse(notNullColumn.isNullable());
        assertTrue(nullableColumn.isNullable());
    }

    @Test
    public void contain_property_type() {
        EntityColumn column = forMethod("getLong", TestEntity.class);
        assertEquals(Long.TYPE, column.getType());
    }

    @Test
    public void memoize_value_regarding_nulls() {
        EntityColumn nullableColumn = forMethod("getNull", TestEntity.class);
        MemoizedValue memoizedNull = nullableColumn.memoizeFor(new TestEntity(""));
        assertTrue(memoizedNull.isNull());
        assertNull(memoizedNull.getValue());
    }

    @Test
    public void memoize_value_which_has_reference_on_Column_itself() {
        EntityColumn column = forMethod("getMutableState", TestEntity.class);
        Entity<String, Any> entity = new TestEntity("");
        MemoizedValue memoizedValue = column.memoizeFor(entity);
        assertSame(column, memoizedValue.getSourceColumn());
    }

    @Test
    public void have_valid_name_for_querying_and_storing() {
        EntityColumn column = forMethod("getValue",
                                              EntityWithCustomColumnNameForStoring.class);
        assertEquals("value", column.getName());
        assertEquals(CUSTOM_COLUMN_NAME.trim(), column.getStoredName());
    }

    @Test
    public void have_same_names_for_and_querying_storing_if_last_is_not_specified() {
        EntityColumn column = forMethod("getValue",
                                              EntityWithDefaultColumnNameForStoring.class);
        String expectedName = "value";
        assertEquals(expectedName, column.getName());
        assertEquals(expectedName, column.getStoredName());
    }

    @Test
    public void not_allow_redefine_column_annotation() {
        thrown.expect(IllegalStateException.class);
        forMethod("getVersion", EntityRedefiningColumnAnnotation.class);
    }

    @Test
    public void be_constructed_from_enumerated_type_getter() {
        final EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        final Class<?> expectedType = Integer.class;
        final Class actualType = column.getPersistedType();
        assertEquals(expectedType, actualType);
    }

    @Test
    public void return_same_persisted_type_for_non_enum_getter() {
        final EntityColumn column = forMethod("getLong", TestEntity.class);
        assertEquals(column.getType(), column.getPersistedType());
    }

    @Test
    public void return_persistence_type_for_ordinal_enumerated_value() {
        final EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        final Class expectedType = Integer.class;
        final Class actualType = column.getPersistedType();
        assertEquals(expectedType, actualType);
    }

    @Test
    public void return_persistence_type_for_string_enumerated_value() {
        final EntityColumn column = forMethod("getEnumString", TestEntity.class);
        final Class expectedType = String.class;
        final Class actualType = column.getPersistedType();
        assertEquals(expectedType, actualType);
    }

    @Test
    public void memoize_value_of_enumerated_ordinal_column() {
        final EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        final TestEntity entity = new TestEntity("");
        final MemoizedValue actualValue = column.memoizeFor(entity);
        final int expectedValue = entity.getEnumOrdinal()
                                  .ordinal();
        assertEquals(expectedValue, actualValue.getValue());
    }

    @Test
    public void memoize_value_of_enumerated_string_column() {
        final EntityColumn column = forMethod("getEnumString", TestEntity.class);
        final TestEntity entity = new TestEntity("");
        final MemoizedValue actualValue = column.memoizeFor(entity);
        final String expectedValue = entity.getEnumOrdinal()
                                           .name();
        assertEquals(expectedValue, actualValue.getValue());
    }

    @Test
    public void convert_enumerated_value_to_persistence_type() {
        final EntityColumn columnOrdinal = forMethod("getEnumOrdinal", TestEntity.class);
        final Object ordinalValue = columnOrdinal.toPersistedValue(SUCCESS);
        assertEquals(SUCCESS.ordinal(), ordinalValue);

        final EntityColumn columnString = forMethod("getEnumString", TestEntity.class);
        final Object stringValue = columnString.toPersistedValue(SUCCESS);
        assertEquals(SUCCESS.name(), stringValue);
    }

    @Test
    public void do_identity_conversion_for_non_enum_values() {
        final EntityColumn column = forMethod("getLong", TestEntity.class);
        final Object value = 15L;
        final Object converted = column.toPersistedValue(value);
        assertEquals(value, converted);
    }

    @Test
    public void return_null_on_null_conversion() {
        final EntityColumn column = forMethod("getLong", TestEntity.class);
        final Object value = null;
        final Object converted = column.toPersistedValue(value);
        assertNull(converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void allow_conversion_only_for_type_stored_in_column() {
        final EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        final String value = "test";
        column.toPersistedValue(value);
    }

    private static EntityColumn forMethod(String name, Class<?> enclosingClass) {
        try {
            Method result = enclosingClass.getDeclaredMethod(name);
            return EntityColumn.from(result);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static class EntityWithCustomColumnNameForStoring
            extends AbstractVersionableEntity<String, Any> {
        private EntityWithCustomColumnNameForStoring(String id) {
            super(id);
        }

        @Column(name = CUSTOM_COLUMN_NAME)
        public int getValue() {
            return 0;
        }
    }
}
