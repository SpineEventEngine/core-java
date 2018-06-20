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
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("be serializable")
    void beSerializable() {
        EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        reserializeAndAssert(column);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Just check that operation passes without an exception.
    @Test
    @DisplayName("restore getter when its not null without errors")
    void restoreGetterWhenItsNotNullWithoutErrors() {
        final EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        column.restoreGetter();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Just check that operation passes without an exception.
    @Test
    @DisplayName("restore value converter when its not null without errors")
    void restoreValueConverterWhenItsNotNullWithoutErrors() {
        final EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        column.restoreValueConverter();
    }

    @Test
    @DisplayName("support toString")
    void supportToString() {
        EntityColumn column = forMethod("getVersion", VersionableEntity.class);
        assertEquals("VersionableEntity.version", column.toString());
    }

    @Test
    @DisplayName("invoke getter")
    void invokeGetter() {
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
    @DisplayName("have equals and hashCode")
    void haveEqualsAndHashCode() {
        EntityColumn col1 = forMethod("getVersion", VersionableEntity.class);
        EntityColumn col2 = forMethod("getVersion", VersionableEntity.class);
        EntityColumn col3 = forMethod("isDeleted", EntityWithLifecycle.class);
        new EqualsTester()
                .addEqualityGroup(col1, col2)
                .addEqualityGroup(col3)
                .testEquals();
    }

    @Test
    @DisplayName("memoize value at at point in time")
    void memoizeValueAtAtPointInTime() {
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
    @DisplayName("not be constructed from non getter")
    void notBeConstructedFromNonGetter() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("toString", Object.class);
    }

    @Test
    @DisplayName("not be constructed from non annotated getter")
    void notBeConstructedFromNonAnnotatedGetter() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getClass", Object.class);
    }

    @Test
    @DisplayName("not be constructed from static method")
    void notBeConstructedFromStaticMethod() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getStatic", TestEntity.class);
    }

    @Test
    @DisplayName("not be constructed from private getter")
    void notBeConstructedFromPrivateGetter() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getFortyTwoLong", TestEntity.class);
    }

    @Test
    @DisplayName("not be constructed from getter with parameters")
    void notBeConstructedFromGetterWithParameters() throws NoSuchMethodException {
        Method method = TestEntity.class.getDeclaredMethod("getParameter",
                                                                 String.class);
        thrown.expect(IllegalArgumentException.class);
        EntityColumn.from(method);
    }

    @Test
    @DisplayName("fail to construct for non serializable column")
    void failToConstructForNonSerializableColumn() {
        thrown.expect(IllegalArgumentException.class);
        forMethod("getFoo", BrokenTestEntity.class);
    }

    @Test
    @DisplayName("fail to get value from wrong object")
    void failToGetValueFromWrongObject() {
        EntityColumn column = forMethod("getMutableState", TestEntity.class);

        thrown.expect(IllegalArgumentException.class);
        column.getFor(new EntityWithCustomColumnNameForStoring(""));
    }

    @Test
    @DisplayName("check value if getter is not null")
    void checkValueIfGetterIsNotNull() {
        EntityColumn column = forMethod("getNotNull", TestEntity.class);
        
        thrown.expect(NullPointerException.class);
        column.getFor(new TestEntity(""));
    }

    @Test
    @DisplayName("allow nulls if getter is nullable")
    void allowNullsIfGetterIsNullable() {
        EntityColumn column = forMethod("getNull", TestEntity.class);
        Object value = column.getFor(new TestEntity(""));
        assertNull(value);
    }

    @Test
    @DisplayName("tell if property is nullable")
    void tellIfPropertyIsNullable() {
        EntityColumn notNullColumn = forMethod("getNotNull", TestEntity.class);
        EntityColumn nullableColumn = forMethod("getNull", TestEntity.class);

        assertFalse(notNullColumn.isNullable());
        assertTrue(nullableColumn.isNullable());
    }

    @Test
    @DisplayName("contain property type")
    void containPropertyType() {
        EntityColumn column = forMethod("getLong", TestEntity.class);
        assertEquals(Long.TYPE, column.getType());
    }

    @Test
    @DisplayName("memoize value regarding nulls")
    void memoizeValueRegardingNulls() {
        EntityColumn nullableColumn = forMethod("getNull", TestEntity.class);
        MemoizedValue memoizedNull = nullableColumn.memoizeFor(new TestEntity(""));
        assertTrue(memoizedNull.isNull());
        assertNull(memoizedNull.getValue());
    }

    @Test
    @DisplayName("memoize value which has reference on Column itself")
    void memoizeValueWhichHasReferenceOnColumnItself() {
        EntityColumn column = forMethod("getMutableState", TestEntity.class);
        Entity<String, Any> entity = new TestEntity("");
        MemoizedValue memoizedValue = column.memoizeFor(entity);
        assertSame(column, memoizedValue.getSourceColumn());
    }

    @Test
    @DisplayName("have valid name for querying and storing")
    void haveValidNameForQueryingAndStoring() {
        EntityColumn column = forMethod("getValue",
                                              EntityWithCustomColumnNameForStoring.class);
        assertEquals("value", column.getName());
        assertEquals(CUSTOM_COLUMN_NAME.trim(), column.getStoredName());
    }

    @Test
    @DisplayName("have same names for and querying storing if last is not specified")
    void haveSameNamesForAndQueryingStoringIfLastIsNotSpecified() {
        EntityColumn column = forMethod("getValue",
                                              EntityWithDefaultColumnNameForStoring.class);
        String expectedName = "value";
        assertEquals(expectedName, column.getName());
        assertEquals(expectedName, column.getStoredName());
    }

    @Test
    @DisplayName("not allow redefine column annotation")
    void notAllowRedefineColumnAnnotation() {
        thrown.expect(IllegalStateException.class);
        forMethod("getVersion", EntityRedefiningColumnAnnotation.class);
    }

    @Test
    @DisplayName("be constructed from enumerated type getter")
    void beConstructedFromEnumeratedTypeGetter() {
        EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        Class<?> expectedType = Integer.class;
        Class actualType = column.getPersistedType();
        assertEquals(expectedType, actualType);
    }

    @Test
    @DisplayName("return same persisted type for non enum getter")
    void returnSamePersistedTypeForNonEnumGetter() {
        EntityColumn column = forMethod("getLong", TestEntity.class);
        assertEquals(column.getType(), column.getPersistedType());
    }

    @Test
    @DisplayName("return persistence type for ordinal enumerated value")
    void returnPersistenceTypeForOrdinalEnumeratedValue() {
        EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        Class expectedType = Integer.class;
        Class actualType = column.getPersistedType();
        assertEquals(expectedType, actualType);
    }

    @Test
    @DisplayName("return persistence type for string enumerated value")
    void returnPersistenceTypeForStringEnumeratedValue() {
        EntityColumn column = forMethod("getEnumString", TestEntity.class);
        Class expectedType = String.class;
        Class actualType = column.getPersistedType();
        assertEquals(expectedType, actualType);
    }

    @Test
    @DisplayName("memoize value of enumerated ordinal column")
    void memoizeValueOfEnumeratedOrdinalColumn() {
        EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        TestEntity entity = new TestEntity("");
        MemoizedValue actualValue = column.memoizeFor(entity);
        int expectedValue = entity.getEnumOrdinal()
                                  .ordinal();
        assertEquals(expectedValue, actualValue.getValue());
    }

    @Test
    @DisplayName("memoize value of enumerated string column")
    void memoizeValueOfEnumeratedStringColumn() {
        EntityColumn column = forMethod("getEnumString", TestEntity.class);
        TestEntity entity = new TestEntity("");
        MemoizedValue actualValue = column.memoizeFor(entity);
        String expectedValue = entity.getEnumOrdinal()
                                           .name();
        assertEquals(expectedValue, actualValue.getValue());
    }

    @Test
    @DisplayName("convert enumerated value to persistence type")
    void convertEnumeratedValueToPersistenceType() {
        EntityColumn columnOrdinal = forMethod("getEnumOrdinal", TestEntity.class);
        Object ordinalValue = columnOrdinal.toPersistedValue(SUCCESS);
        assertEquals(SUCCESS.ordinal(), ordinalValue);

        EntityColumn columnString = forMethod("getEnumString", TestEntity.class);
        Object stringValue = columnString.toPersistedValue(SUCCESS);
        assertEquals(SUCCESS.name(), stringValue);
    }

    @Test
    @DisplayName("do identity conversion for non enum values")
    void doIdentityConversionForNonEnumValues() {
        EntityColumn column = forMethod("getLong", TestEntity.class);
        Object value = 15L;
        Object converted = column.toPersistedValue(value);
        assertEquals(value, converted);
    }

    @Test
    @DisplayName("return null on null conversion")
    void returnNullOnNullConversion() {
        EntityColumn column = forMethod("getLong", TestEntity.class);
        Object value = null;
        Object converted = column.toPersistedValue(value);
        assertNull(converted);
    }

    @Test
    @DisplayName("allow conversion only for type stored in column")
    void allowConversionOnlyForTypeStoredInColumn() {
        EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        String value = "test";
        thrown.expect(IllegalArgumentException.class);
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
