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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithASetterButNoGetter;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithBooleanColumns;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithColumnFromInterface;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGetters;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGettersDescendant;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithNoStorageFields;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithRepeatedColumnNames;
import io.spine.server.entity.storage.given.ColumnsTestEnv.RealLifeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.server.entity.storage.ColumnReader.forClass;
import static io.spine.server.entity.storage.given.ColumnsTestEnv.assertContainsColumns;
import static io.spine.server.entity.storage.given.ColumnsTestEnv.assertNotContainsColumns;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("DuplicateStringLiteralInspection") // Lots of literals for column names.
@DisplayName("ColumnReader should")
class ColumnReaderTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testStaticMethods(ColumnReader.class, Visibility.PACKAGE);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Call the method to throw exception.
    @Test
    @DisplayName("throw ISE on invalid column definitions")
    void throwOnInvalidColumns() {
        ColumnReader columnReader = forClass(EntityWithRepeatedColumnNames.class);
        assertThrows(IllegalStateException.class, columnReader::readColumns);
    }

    @Nested
    @DisplayName("retrieve entity columns")
    class RetrieveColumns {

        @Test
        @DisplayName("from class having them")
        void fromClassWithColumns() {
            ColumnReader columnReader = forClass(EntityWithManyGetters.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();

            assertContainsColumns(
                    entityColumns,
                    "boolean", "booleanWrapper", "someMessage", "integerFieldValue", "floatNull"
            );
        }

        @Test
        @DisplayName("from class without columns")
        void fromClassWithoutColumns() {
            ColumnReader columnReader = forClass(EntityWithNoStorageFields.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();

            assertNotNull(entityColumns);
            assertTrue(entityColumns.isEmpty());
        }

        @Test
        @DisplayName("from class inheriting columns")
        void fromClassInheritingColumns() {
            ColumnReader columnReader = forClass(RealLifeEntity.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();

            assertContainsColumns(
                    entityColumns,
                    archived.name(), deleted.name(), "visible", version.name(), "someTime"
            );
        }

        @Test
        @DisplayName("from implemented interface")
        void fromImplementedInterface() {
            ColumnReader columnReader = forClass(EntityWithColumnFromInterface.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();
            assertContainsColumns(entityColumns, "integerFieldValue");
        }
    }

    @Test
    @DisplayName("not confuse a setter method with a property mutator")
    void testSetterDeclaringEntity() {
        ColumnReader columnReader = forClass(EntityWithASetterButNoGetter.class);
        Collection<EntityColumn> entityColumns = columnReader.readColumns();
        assertNotContainsColumns(entityColumns, "secretNumber");
    }

    @Nested
    @DisplayName("when extracting columns, ignore")
    class Ignore {

        @Test
        @DisplayName("non-public getters with column annotation from super class")
        void inheritedNonPublicColumns() {
            ColumnReader columnReader = forClass(EntityWithManyGettersDescendant.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();
            assertNotContainsColumns(entityColumns, "someNonPublicMethod");
        }

        @Test
        @DisplayName("static members")
        void staticMembers() {
            ColumnReader columnReader = forClass(EntityWithManyGetters.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();
            assertNotContainsColumns(entityColumns, "staticMember");
        }
    }

    @Nested
    @DisplayName("look for `Boolean` columns via predicate which returns")
    class InBooleanLookupPredicate {

        private final Predicate<Method> predicate = ColumnReader.isBooleanWrapperProperty;

        @Test
        @DisplayName("`true` for correct `Boolean` column")
        void findCorrect() throws NoSuchMethodException {
            boolean result = predicate.test(method("isBooleanWrapperColumn"));
            assertTrue(result);
        }

        @Test
        @DisplayName("`false` for column starting with `get`")
        void notFindStartingWithGet() throws NoSuchMethodException {
            boolean result = predicate.test(method("getBooleanWrapperColumn"));
            assertFalse(result);
        }

        @Test
        @DisplayName("`false` for non-`Boolean` column")
        void notFindNonBoolean() throws NoSuchMethodException {
            boolean result = predicate.test(method("isNonBoolean"));
            assertFalse(result);
        }

        @Test
        @DisplayName("`false` for non-`Boolean` column starting with `get`")
        void notFindNonBooleanStartingWithGet() throws NoSuchMethodException {
            boolean result = predicate.test(method("getNonBoolean"));
            assertFalse(result);
        }

        @Test
        @DisplayName("`false` for `Boolean` column with params")
        void notFindBooleanWithParams() throws NoSuchMethodException {
            boolean result = predicate.test(method("isBooleanWithParam", int.class));
            assertFalse(result);
        }

        @Test
        @DisplayName("`false` for `Boolean` column with params starting with `get`")
        void notFindBooleanWithParamsStartingWithGet() throws NoSuchMethodException {
            boolean result = predicate.test(method("getBooleanWithParam", int.class));
            assertFalse(result);
        }

        @Test
        @DisplayName("`false` for non-`Boolean` column with params")
        void notFindNonBooleanWithParams() throws NoSuchMethodException {
            boolean result = predicate.test(method("isNonBooleanWithParam", int.class));
            assertFalse(result);
        }

        @Test
        @DisplayName("`false` for non-`Boolean` column with params starting with `get`")
        void notFindNonBooleanWithParamsStartingWithGet() throws NoSuchMethodException {
            boolean result = predicate.test(method("getNonBooleanWithParam", int.class));
            assertFalse(result);
        }

        private Method method(String name, Class<?>... parameterTypes)
                throws NoSuchMethodException {
            return EntityWithBooleanColumns.class.getDeclaredMethod(name, parameterTypes);
        }
    }
}
