/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.storage.given.TaskListViewProjection;
import io.spine.server.entity.storage.given.TaskViewProjection;
import io.spine.server.storage.LifecycleFlagField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("`Columns` should")
class ColumnsTest {

    private final Columns columns = Columns.of(TaskViewProjection.class);

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Columns.class);
        new NullPointerTester()
                .testAllPublicInstanceMethods(columns);
    }

    @Test
    @DisplayName("be extracted from an entity class")
    void beExtractedFromEntityClass() {
        Columns columns = Columns.of(TaskListViewProjection.class);
        ColumnName columnName = ColumnName.of("description");
        Optional<Column> descriptionColumn = columns.find(columnName);

        assertThat(descriptionColumn).isPresent();
    }

    @Test
    @DisplayName("obtain a column by name")
    void obtainByName() {
        ColumnName columName = ColumnName.of("estimate_in_days");
        Column column = columns.get(columName);

        assertThat(column.type()).isEqualTo(int.class);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `IAE` when the column with the specified name is not found")
    void throwOnColumnNotFound() {
        ColumnName nonExistent = ColumnName.of("non-existent-column");

        assertThrows(IllegalArgumentException.class, () -> columns.get(nonExistent));
    }

    @Test
    @DisplayName("search for a column by name")
    void searchByName() {
        ColumnName existent = ColumnName.of("name");
        Optional<Column> column = columns.find(existent);

        assertThat(column).isPresent();
    }

    @Test
    @DisplayName("return empty `Optional` when searching for a non-existent column")
    void returnEmptyOptionalForNonExistent() {
        ColumnName nonExistent = ColumnName.of("non-existent-column");
        Optional<Column> result = columns.find(nonExistent);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("return the list of columns")
    void returnColumns() {
        int systemColumnCount = ColumnTests.defaultColumns.size();
        int protoColumnCount = 4;

        int expectedSize = systemColumnCount + protoColumnCount;
        assertThat(columns.columnList()).hasSize(expectedSize);

    }

    @Test
    @DisplayName("extract values from entity")
    void extractColumnValues() {
        TaskViewProjection projection = new TaskViewProjection();
        Map<ColumnName, Object> values = columns.valuesIn(projection);

        assertThat(values).containsExactly(
                ColumnName.of("archived"), projection.isArchived(),
                ColumnName.of("deleted"), projection.isDeleted(),
                ColumnName.of("version"), projection.version(),
                ColumnName.of("name"), projection.state()
                                                 .getName(),
                ColumnName.of("estimate_in_days"), projection.state()
                                                             .getEstimateInDays(),
                ColumnName.of("status"), projection.state()
                                                   .getStatus(),
                ColumnName.of("due_date"), projection.state()
                                                     .getDueDate()
        );
    }

    @Test
    @DisplayName("return a map of interface-based columns")
    void extractInterfaceBasedValues() {
        ImmutableMap<ColumnName, InterfaceBasedColumn> values = columns.interfaceBasedColumns();

        assertThat(values).containsKey(ColumnName.of("name"));
        assertThat(values).containsKey(ColumnName.of("estimate_in_days"));
        assertThat(values).containsKey(ColumnName.of("status"));
        assertThat(values).containsKey(ColumnName.of("due_date"));
    }

    @Test
    @DisplayName("return a map of lifecycle columns of the entity")
    void returnLifecycleColumns() {
        ImmutableMap<ColumnName, Column> columns = this.columns.lifecycleColumns();

        assertThat(columns).hasSize(2);

        ColumnName archived = ColumnName.of(LifecycleFlagField.archived);
        Column archivedColumn = columns.get(archived);
        assertThat(archivedColumn).isNotNull();

        ColumnName deleted = ColumnName.of(LifecycleFlagField.deleted);
        Column deletedColumn = columns.get(deleted);
        assertThat(deletedColumn).isNotNull();
    }
}
