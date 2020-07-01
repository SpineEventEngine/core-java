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
import io.spine.server.storage.OldColumn;
import io.spine.test.entity.TaskListViewId;
import io.spine.test.entity.TaskViewId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.LifecycleColumn.archived;
import static io.spine.server.entity.storage.LifecycleColumn.deleted;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("`EntityRecordSpec` should")
class EntityRecordSpecTest {

    private static EntityRecordSpec<TaskViewId> spec() {
        return EntityRecordSpec.of(TaskViewProjection.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EntityRecordSpec.class);
        new NullPointerTester()
                .testAllPublicInstanceMethods(spec());
    }

    @Test
    @DisplayName("be extracted from an entity class")
    void beExtractedFromEntityClass() {
        EntityRecordSpec<TaskListViewId> spec = EntityRecordSpec.of(TaskListViewProjection.class);
        OldColumnName columnName = OldColumnName.of("description");
        Optional<OldColumn> descriptionColumn = spec.find(columnName);

        assertThat(descriptionColumn.isPresent()).isTrue();
    }

    @Test
    @DisplayName("obtain a column by name")
    void obtainByName() {
        OldColumnName columName = OldColumnName.of("estimate_in_days");
        OldColumn column = spec().get(columName);

        assertThat(column.type()).isEqualTo(int.class);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `IAE` when the column with the specified name is not found")
    void throwOnColumnNotFound() {
        OldColumnName nonExistent = OldColumnName.of("non-existent-column");

        assertThrows(IllegalArgumentException.class, () -> spec().get(nonExistent));
    }

    @Test
    @DisplayName("search for a column by name")
    void searchByName() {
        OldColumnName existent = OldColumnName.of("name");
        Optional<OldColumn> column = spec().find(existent);

        assertThat(column.isPresent()).isTrue();
    }

    @Test
    @DisplayName("return empty `Optional` when searching for a non-existent column")
    void returnEmptyOptionalForNonExistent() {
        OldColumnName nonExistent = OldColumnName.of("non-existent-column");
        Optional<OldColumn> result = spec().find(nonExistent);

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    @DisplayName("return the list of columns")
    void returnColumns() {
        int systemColumnCount = GivenEntityColumns.defaultEntityColumns.size();
        int protoColumnCount = 4;

        int expectedSize = systemColumnCount + protoColumnCount;
        assertThat(spec().columnList()).hasSize(expectedSize);

    }

    @Test
    @DisplayName("extract values from entity")
    void extractColumnValues() {
        TaskViewProjection projection = new TaskViewProjection();
        Map<OldColumnName, Object> values = spec().valuesIn(projection);

        assertThat(values).containsExactly(
                OldColumnName.of("archived"), projection.isArchived(),
                OldColumnName.of("deleted"), projection.isDeleted(),
                OldColumnName.of("version"), projection.version(),
                OldColumnName.of("name"), projection.state()
                                                    .getName(),
                OldColumnName.of("estimate_in_days"), projection.state()
                                                                .getEstimateInDays(),
                OldColumnName.of("status"), projection.state()
                                                      .getStatus(),
                OldColumnName.of("due_date"), projection.state()
                                                        .getDueDate()
        );
    }

    @Test
    @DisplayName("return a map of interface-based columns")
    void extractInterfaceBasedValues() {
        ImmutableMap<OldColumnName, InterfaceBasedColumn> values = spec().interfaceBasedColumns();

        assertThat(values).containsKey(OldColumnName.of("name"));
        assertThat(values).containsKey(OldColumnName.of("estimate_in_days"));
        assertThat(values).containsKey(OldColumnName.of("status"));
        assertThat(values).containsKey(OldColumnName.of("due_date"));
    }

    @Test
    @DisplayName("return a map of lifecycle columns of the entity")
    void returnLifecycleColumns() {
        ImmutableMap<OldColumnName, OldColumn> columns = spec().lifecycleColumns();

        assertThat(columns).hasSize(2);

        OldColumn archivedColumn = columns.get(archived.columnName());
        assertThat(archivedColumn).isNotNull();

        OldColumn deletedColumn = columns.get(deleted.columnName());
        assertThat(deletedColumn).isNotNull();
    }
}
