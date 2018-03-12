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
import io.spine.server.entity.storage.given.ColumnsTestEnv.*;
import org.junit.Test;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.test.Verify.*;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;

/**
 * @author Dmytro Kuzmin
 */
public class ColumnReaderShould {

    @Test
    public void pass_null_check() {
        new NullPointerTester().testStaticMethods(ColumnReader.class, Visibility.PACKAGE);
    }

    @Test
    public void retrieve_entity_columns_from_class() {
        final ColumnReader columnReader = ColumnReader.createForClass(EntityWithManyGetters.class);
        final Collection<EntityColumn> entityColumns = columnReader.readColumns();

        assertSize(3, entityColumns);
        assertTrue(containsColumn(entityColumns, "someMessage"));
        assertTrue(containsColumn(entityColumns, "integerFieldValue"));
        assertTrue(containsColumn(entityColumns, "floatNull"));
    }

    @Test
    public void handle_class_without_columns() {
        final ColumnReader columnReader = ColumnReader.createForClass(EntityWithNoStorageFields.class);
        final Collection<EntityColumn> entityColumns = columnReader.readColumns();

        assertNotNull(entityColumns);
        assertTrue(entityColumns.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_error_on_invalid_column_definitions() {
        final ColumnReader columnReader = ColumnReader.createForClass(EntityWithRepeatedColumnNames.class);
        columnReader.readColumns();
    }


    @Test
    public void ignore_non_public_getters_with_column_annotation_from_super_class() {
        final ColumnReader columnReader = ColumnReader.createForClass(EntityWithManyGettersDescendant.class);
        final Collection<EntityColumn> entityColumns = columnReader.readColumns();
        assertSize(3, entityColumns);
    }

    @Test
    public void ignore_static_members() {
        final ColumnReader columnReader = ColumnReader.createForClass(EntityWithManyGetters.class);
        final Collection<EntityColumn> entityColumns = columnReader.readColumns();
        assertFalse(containsColumn(entityColumns, "staticMember"));
    }

    @Test
    public void handle_inherited_fields() {
        final ColumnReader columnReader = ColumnReader.createForClass(RealLifeEntity.class);
        final Collection<EntityColumn> entityColumns = columnReader.readColumns();

        assertSize(5, entityColumns);
        assertTrue(containsColumn(entityColumns, archived.name()));
        assertTrue(containsColumn(entityColumns, deleted.name()));
        assertTrue(containsColumn(entityColumns, "visible"));
        assertTrue(containsColumn(entityColumns, version.name()));
        assertTrue(containsColumn(entityColumns, "someTime"));
    }

    @Test
    public void obtain_fields_from_implemented_interfaces() {
        final ColumnReader columnReader = ColumnReader.createForClass(EntityWithColumnFromInterface.class);
        final Collection<EntityColumn> entityColumns = columnReader.readColumns();

        assertSize(1, entityColumns);
        assertTrue(containsColumn(entityColumns, "integerFieldValue"));
    }

    private static boolean containsColumn(Iterable<EntityColumn> entityColumns, String columnName) {
        checkNotNull(entityColumns);
        checkNotEmptyOrBlank(columnName, "column name");

        for (EntityColumn column : entityColumns) {
            if (column.getName()
                    .equals(columnName)) {
                return true;
            }
        }
        return false;
    }
}
