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
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithColumnFromInterface;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGetters;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGettersDescendant;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithNoStorageFields;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithRepeatedColumnNames;
import io.spine.server.entity.storage.given.ColumnsTestEnv.RealLifeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.ColumnReader.forClass;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ColumnReader should")
class ColumnReaderTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testStaticMethods(ColumnReader.class, Visibility.PACKAGE);
    }

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

            assertThat(entityColumns).hasSize(5);
            assertTrue(containsColumn(entityColumns, "boolean"));
            assertTrue(containsColumn(entityColumns, "booleanWrapper"));
            assertTrue(containsColumn(entityColumns, "someMessage"));
            assertTrue(containsColumn(entityColumns, "integerFieldValue"));
            assertTrue(containsColumn(entityColumns, "floatNull"));
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

            assertThat(entityColumns).hasSize(5);
            assertTrue(containsColumn(entityColumns, archived.name()));
            assertTrue(containsColumn(entityColumns, deleted.name()));
            assertTrue(containsColumn(entityColumns, "visible"));
            assertTrue(containsColumn(entityColumns, version.name()));
            assertTrue(containsColumn(entityColumns, "someTime"));
        }

        @Test
        @DisplayName("from implemented interface")
        void fromImplementedInterface() {
            ColumnReader columnReader = forClass(EntityWithColumnFromInterface.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();

            assertThat(entityColumns).hasSize(1);
            assertTrue(containsColumn(entityColumns, "integerFieldValue"));
        }
    }

    @Nested
    @DisplayName("when extracting columns, ignore")
    class Ignore {

        @Test
        @DisplayName("non-public getters with column annotation from super class")
        void inheritedNonPublicColumns() {
            ColumnReader columnReader = forClass(EntityWithManyGettersDescendant.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();
            assertThat(entityColumns).hasSize(5);
        }

        @Test
        @DisplayName("static members")
        void staticMembers() {
            ColumnReader columnReader = forClass(EntityWithManyGetters.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();
            assertFalse(containsColumn(entityColumns, "staticMember"));
        }

        @Test
        @DisplayName("`Boolean` methods with parameters")
        void booleanGettersWithParam() {
            ColumnReader columnReader = forClass(EntityWithManyGetters.class);
            Collection<EntityColumn> entityColumns = columnReader.readColumns();
            assertFalse(containsColumn(entityColumns, "booleanWithParam"));
        }
    }

    private static boolean containsColumn(Iterable<EntityColumn> entityColumns, String columnName) {
        checkNotNull(entityColumns);
        checkNotEmptyOrBlank(columnName, "column name");

        for (EntityColumn column : entityColumns) {
            if (columnName.equals(column.getName())) {
                return true;
            }
        }
        return false;
    }
}
