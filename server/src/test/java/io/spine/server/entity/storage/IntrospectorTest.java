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

import com.google.common.collect.ImmutableMap;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.entity.storage.given.IntrospectorTestEnv.InvalidEntityWithColumns;
import io.spine.server.entity.storage.given.TaskListViewProjection;
import io.spine.server.entity.storage.given.TaskViewProjection;
import io.spine.server.projection.Projection;
import io.spine.test.entity.TaskListView;
import io.spine.test.entity.TaskListViewId;
import io.spine.test.entity.TaskListViewWithColumns;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.server.storage.VersionField.version;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Introspector` should")
class IntrospectorTest {

    @Test
    @DisplayName("extract system columns from the entity class")
    void extractSystemColumns() {
        EntityClass<TaskViewProjection> entityClass = asEntityClass(TaskViewProjection.class);
        Introspector introspector = new Introspector(entityClass);
        ImmutableMap<ColumnName, Column> systemColumns = introspector.systemColumns();

        assertThat(systemColumns).containsKey(ColumnName.of(archived));
        assertThat(systemColumns).containsKey(ColumnName.of(deleted));
        assertThat(systemColumns).containsKey(ColumnName.of(version));
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    @Nested
    @DisplayName("extract columns defined in Protobuf")
    class ExtractProtoColumns {

        @Test
        @DisplayName("from the entity which implements an `EntityWithColumns` interface")
        void fromImplementedInterface() {
            EntityClass<TaskViewProjection> entityClass = asEntityClass(TaskViewProjection.class);
            Introspector introspector = new Introspector(entityClass);
            ImmutableMap<ColumnName, Column> columns = introspector.protoColumns();

            assertThat(columns).containsKey(ColumnName.of("name"));
            assertThat(columns).containsKey(ColumnName.of("estimate_in_days"));
            assertThat(columns).containsKey(ColumnName.of("status"));
            assertThat(columns).containsKey(ColumnName.of("due_date"));
        }

        @Test
        @DisplayName("from the entity which updates column values by modifying own state")
        void fromEntityState() {
            EntityClass<TaskListViewProjection> entityClass =
                    asEntityClass(TaskListViewProjection.class);
            Introspector introspector = new Introspector(entityClass);

            ImmutableMap<ColumnName, Column> columns = introspector.protoColumns();

            assertThat(columns).containsKey(ColumnName.of("description"));
        }
    }

    @Test
    @DisplayName("extract columns from the non-`public` entity class")
    void extractFromNonPublic() {
        EntityClass<PrivateProjection> entityClass = asEntityClass(PrivateProjection.class);
        Introspector introspector = new Introspector(entityClass);

        ImmutableMap<ColumnName, Column> columns = introspector.protoColumns();

        ColumnName description = ColumnName.of("description");
        Column column = columns.get(description);

        Entity<TaskListViewId, TaskListView> projection = new PrivateProjection();

        assertThat(column.valueFromInterface(projection)).isEqualTo(PrivateProjection.DESCRIPTION);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `ISE` if getter with the expected name is not found in the entity class")
    void throwOnGetterNotFound() {
        EntityClass<InvalidEntityWithColumns> entityClass =
                asEntityClass(InvalidEntityWithColumns.class);
        Introspector introspector = new Introspector(entityClass);

        assertThrows(IllegalStateException.class, introspector::protoColumns);
    }

    /**
     * A projection with a non-{@code public} access.
     *
     * <p>Due to the access restrictions, can't be moved to the test env.
     */
    private static class PrivateProjection
            extends Projection<TaskListViewId, TaskListView, TaskListView.Builder>
            implements TaskListViewWithColumns {

        private static final String DESCRIPTION = "some-list-description";

        @Override
        public String getDescription() {
            return DESCRIPTION;
        }
    }
}
