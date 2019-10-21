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

import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.storage.given.TaskListViewProjection;
import io.spine.server.entity.storage.given.TaskViewProjection;
import io.spine.server.storage.LifecycleFlagField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
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

        assertThat(descriptionColumn.isPresent()).isTrue();
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

        assertThat(column.isPresent()).isTrue();
    }

    @Test
    @DisplayName("return empty `Optional` when searching for a non-existent column")
    void returnEmptyOptionalForNonExistent() {
        ColumnName nonExistent = ColumnName.of("non-existent-column");
        Optional<Column> result = columns.find(nonExistent);

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    @DisplayName("return the list of columns")
    void returnColumns() {
        int systemColumnCount = ColumnTests.defaultColumns.size();
        int protoColumnCount = 4;

        assertThat(columns.columnList())
                .hasSize(systemColumnCount + protoColumnCount);

    }

    @Test
    @DisplayName("extract column values from the entity")
    void extractColumnValues() {
        TaskViewProjection projection = new TaskViewProjection();
        Map<ColumnName, Object> values = columns.valuesIn(projection);

        ColumnName name = ColumnName.of("name");
        ColumnName estimateInDays = ColumnName.of("estimate_in_days");
        ColumnName status = ColumnName.of("status");
        ColumnName dueDate = ColumnName.of("due_date");

        assertThat(values).containsAtLeast(name, projection.getName(),
                                           estimateInDays, projection.getEstimateInDays(),
                                           status, projection.getStatus(),
                                           dueDate, projection.getDueDate());
    }

    @Test
    @DisplayName("return lifecycle columns of the entity if they are present")
    void returnLifecycleColumns() {
        Columns columns = this.columns.lifecycleColumns();

        assertThat(columns.columnList()).hasSize(2);

        ColumnName archived = ColumnName.of(LifecycleFlagField.archived);
        Optional<Column> archivedColumn = columns.find(archived);
        assertThat(archivedColumn.isPresent()).isTrue();

        ColumnName deleted = ColumnName.of(LifecycleFlagField.deleted);
        Optional<Column> deletedColumn = columns.find(deleted);
        assertThat(deletedColumn.isPresent()).isTrue();
    }
}
