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

import com.google.common.testing.EqualsTester;
import com.google.protobuf.Any;
import io.spine.core.Version;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.server.entity.storage.given.column.BrokenTestEntity;
import io.spine.server.entity.storage.given.column.EntityRedefiningColumnAnnotation;
import io.spine.server.entity.storage.given.column.EntityWithCustomColumnNameForStoring;
import io.spine.server.entity.storage.given.column.EntityWithDefaultColumnNameForStoring;
import io.spine.server.entity.storage.given.column.TestAggregate;
import io.spine.server.entity.storage.given.column.TestEntity;
import io.spine.testing.server.entity.given.Given;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.server.entity.storage.given.column.EntityWithCustomColumnNameForStoring.CUSTOM_COLUMN_NAME;
import static io.spine.server.entity.storage.given.column.TaskStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Many string literals for method names. */})
@DisplayName("Column should")
class ColumnTest {

    static EntityColumn forMethod(String name, Class<?> enclosingClass) {
        try {
            Method result = enclosingClass.getDeclaredMethod(name);
            return EntityColumn.from(result);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        EntityColumn column = forMethod("getVersion", Entity.class);
        reserializeAndAssert(column);
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @SuppressWarnings("ResultOfMethodCallIgnored")
        // Just check that operation passes without an exception.
        @Test
        @DisplayName("non-null getter without errors")
        void getter() {
            EntityColumn column = forMethod("getVersion", Entity.class);
            column.restoreGetter();
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        // Just check that operation passes without an exception.
        @Test
        @DisplayName("non-null value converter without errors")
        void valueConverter() {
            EntityColumn column = forMethod("getVersion", Entity.class);
            column.restoreValueConverter();
        }
    }

    @Test
    @DisplayName("support `toString`")
    void supportToString() {
        EntityColumn column = forMethod("getVersion", Entity.class);
        assertEquals("Entity.version", column.toString());
    }

    @Test
    @DisplayName("invoke getter")
    void invokeGetter() {
        int version = 2;
        EntityColumn column = forMethod("getVersion", Entity.class);
        TestAggregate entity = Given.aggregateOfClass(TestAggregate.class)
                                    .withId(1L)
                                    .withVersion(version)
                                    .build();
        Version actualVersion = (Version) column.getFor(entity);
        assertEquals(version, actualVersion.getNumber());
    }

    @Test
    @DisplayName("have `equals` and `hashCode`")
    void haveEqualsAndHashCode() {
        EntityColumn col1 = forMethod("getVersion", Entity.class);
        EntityColumn col2 = forMethod("getVersion", Entity.class);
        EntityColumn col3 = forMethod("isDeleted", Entity.class);
        new EqualsTester()
                .addEqualityGroup(col1, col2)
                .addEqualityGroup(col3)
                .testEquals();
    }

    @Test
    @DisplayName("memoize value at point in time")
    void memoizeValue() {
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

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Just check that column is constructed without an exception.
    @Nested
    @DisplayName("allow `is` prefix")
    class AllowIsPrefix {

        @Test
        @DisplayName("for `boolean` properties")
        void forBooleanProperties() {
            forMethod("isBoolean", TestEntity.class);
        }

        @Test
        @DisplayName("for `Boolean` properties")
        void forBooleanWrapperProperties() {
            forMethod("isBooleanWrapper", TestEntity.class);
        }
    }

    @Nested
    @DisplayName("not be constructed from")
    class NotBeConstructedFrom {

        @Test
        @DisplayName("non-getter")
        void nonGetter() {
            assertThrows(IllegalArgumentException.class,
                         () -> forMethod("toString", Object.class));
        }

        @Test
        @DisplayName("non-annotated getter")
        void nonAnnotatedGetter() {
            assertThrows(IllegalArgumentException.class,
                         () -> forMethod("getClass", Object.class));
        }

        @Test
        @DisplayName("static method")
        void staticMethod() {
            assertThrows(IllegalArgumentException.class,
                         () -> forMethod("getStatic", TestEntity.class));
        }

        @Test
        @DisplayName("private getter")
        void privateGetter() {
            assertThrows(IllegalArgumentException.class,
                         () -> forMethod("getFortyTwoLong", TestEntity.class));
        }

        @Test
        @DisplayName("getter with non-serializable return type")
        void nonSerializableGetter() {
            assertThrows(IllegalArgumentException.class,
                         () -> forMethod("getFoo", BrokenTestEntity.class));
        }

        @Test
        @DisplayName("getter with parameters")
        void getterWithParams() throws NoSuchMethodException {
            Method method = TestEntity.class.getDeclaredMethod("getParameter", String.class);
            assertThrows(IllegalArgumentException.class, () -> EntityColumn.from(method));
        }

        @Test
        @DisplayName("getter with `is` prefix and non-boolean return type")
        void nonBooleanIsGetter() {
            assertThrows(IllegalArgumentException.class,
                         () -> forMethod("isNonBoolean", TestEntity.class));
        }
    }

    @Test
    @DisplayName("fail to get value from wrong object")
    void notGetForWrongObject() {
        EntityColumn column = forMethod("getMutableState", TestEntity.class);

        assertThrows(IllegalArgumentException.class,
                     () -> column.getFor(new EntityWithCustomColumnNameForStoring("")));
    }

    @Test
    @DisplayName("tell if property is nullable")
    void tellIfNullable() {
        EntityColumn notNullColumn = forMethod("getNotNull", TestEntity.class);
        EntityColumn nullableColumn = forMethod("getNull", TestEntity.class);

        assertFalse(notNullColumn.isNullable());
        assertTrue(nullableColumn.isNullable());
    }

    @Test
    @DisplayName("check value for null if getter is not nullable")
    void checkNonNullable() {
        EntityColumn column = forMethod("getNotNull", TestEntity.class);

        assertThrows(NullPointerException.class, () -> column.getFor(new TestEntity("")));
    }

    @Test
    @DisplayName("allow null values if getter is nullable")
    void allowNullForNullable() {
        EntityColumn column = forMethod("getNull", TestEntity.class);
        Object value = column.getFor(new TestEntity(""));
        assertNull(value);
    }

    @Test
    @DisplayName("contain property type")
    void containType() {
        EntityColumn column = forMethod("getLong", TestEntity.class);
        assertEquals(Long.TYPE, column.getType());
    }

    @Nested
    @DisplayName("memoize value")
    class MemoizeValue {

        @Test
        @DisplayName("which is null")
        void whichIsNull() {
            EntityColumn nullableColumn = forMethod("getNull", TestEntity.class);
            MemoizedValue memoizedNull = nullableColumn.memoizeFor(new TestEntity(""));
            assertTrue(memoizedNull.isNull());
            assertNull(memoizedNull.getValue());
        }

        @Test
        @DisplayName("referencing column itself")
        void referencingColumn() {
            EntityColumn column = forMethod("getMutableState", TestEntity.class);
            Entity<String, Any> entity = new TestEntity("");
            MemoizedValue memoizedValue = column.memoizeFor(entity);
            assertSame(column, memoizedValue.getSourceColumn());
        }

        @Test
        @DisplayName("of ordinal enum type")
        void ofOrdinalEnumType() {
            EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
            TestEntity entity = new TestEntity("");
            MemoizedValue actualValue = column.memoizeFor(entity);
            int expectedValue = entity.getEnumOrdinal()
                                      .ordinal();
            assertEquals(expectedValue, actualValue.getValue());
        }

        @Test
        @DisplayName("of string enum type")
        void ofStringEnumType() {
            EntityColumn column = forMethod("getEnumString", TestEntity.class);
            TestEntity entity = new TestEntity("");
            MemoizedValue actualValue = column.memoizeFor(entity);
            String expectedValue = entity.getEnumOrdinal()
                                         .name();
            assertEquals(expectedValue, actualValue.getValue());
        }
    }

    @Nested
    @DisplayName("have name for storing")
    class HaveNameForStoring {

        @Test
        @DisplayName("custom if specified")
        void custom() {
            EntityColumn column = forMethod("getValue", EntityWithCustomColumnNameForStoring.class);
            assertEquals("value", column.getName());
            assertEquals(CUSTOM_COLUMN_NAME.trim(), column.getStoredName());
        }

        @Test
        @DisplayName("same as getter name if no custom one was specified")
        void sameAsGetter() {
            EntityColumn column = forMethod("getValue",
                                            EntityWithDefaultColumnNameForStoring.class);
            String expectedName = "value";
            assertEquals(expectedName, column.getName());
            assertEquals(expectedName, column.getStoredName());
        }
    }

    @Test
    @DisplayName("not allow to redefine column annotation")
    void rejectRedefinedAnnotation() {
        assertThrows(IllegalStateException.class,
                     () -> forMethod("getCustomColumn", EntityRedefiningColumnAnnotation.class));
    }

    @Test
    @DisplayName("be constructed from enumerated type getter")
    void acceptEnumGetter() {
        EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        Class<?> expectedType = Integer.class;
        Class actualType = column.getPersistedType();
        assertEquals(expectedType, actualType);
    }

    @Nested
    @DisplayName("return persisted type which")
    class ReturnPersistedType {

        @Test
        @DisplayName("is same as column type for non-enum getter")
        void forNonEnumGetter() {
            EntityColumn column = forMethod("getLong", TestEntity.class);
            assertEquals(column.getType(), column.getPersistedType());
        }

        @Test
        @DisplayName("is Integer for ordinal enum getter")
        void forOrdinalEnumGetter() {
            EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
            Class expectedType = Integer.class;
            Class actualType = column.getPersistedType();
            assertEquals(expectedType, actualType);
        }

        @Test
        @DisplayName("is String for string enum getter")
        void forStringEnumGetter() {
            EntityColumn column = forMethod("getEnumString", TestEntity.class);
            Class expectedType = String.class;
            Class actualType = column.getPersistedType();
            assertEquals(expectedType, actualType);
        }
    }

    @Nested
    @DisplayName("when converting to persisted type, return")
    class Convert {

        @Test
        @DisplayName("same value for non-enum values")
        void nonEnumToSame() {
            EntityColumn column = forMethod("getLong", TestEntity.class);
            Object value = 15L;
            Object converted = column.toPersistedValue(value);
            assertEquals(value, converted);
        }

        @Test
        @DisplayName("persisted value for enum values")
        void enumToPersisted() {
            EntityColumn columnOrdinal = forMethod("getEnumOrdinal", TestEntity.class);
            Object ordinalValue = columnOrdinal.toPersistedValue(SUCCESS);
            assertEquals(SUCCESS.ordinal(), ordinalValue);

            EntityColumn columnString = forMethod("getEnumString", TestEntity.class);
            Object stringValue = columnString.toPersistedValue(SUCCESS);
            assertEquals(SUCCESS.name(), stringValue);
        }

        @Test
        @DisplayName("null for null values")
        void nullToNull() {
            EntityColumn column = forMethod("getLong", TestEntity.class);
            Object value = null;
            Object converted = column.toPersistedValue(value);
            assertNull(converted);
        }
    }

    @Test
    @DisplayName("allow conversion only for type stored in column")
    void convertOnlyStoredType() {
        EntityColumn column = forMethod("getEnumOrdinal", TestEntity.class);
        String value = "test";
        assertThrows(IllegalArgumentException.class, () -> column.toPersistedValue(value));
    }
}
