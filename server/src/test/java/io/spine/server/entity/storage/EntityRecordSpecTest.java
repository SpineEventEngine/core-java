/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.server.entity.storage.given.TaskListViewProjection;
import io.spine.server.entity.storage.given.TaskViewProjection;
import io.spine.test.entity.TaskView;
import io.spine.test.entity.TaskViewId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("`EntityRecordSpec` should")
class EntityRecordSpecTest {

    private static EntityRecordSpec<TaskViewId, TaskView, TaskViewProjection> spec() {
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
        EntityRecordSpec<?, ?, ?> spec = EntityRecordSpec.of(TaskListViewProjection.class);
        ColumnName columnName = ColumnName.of("description");
        Optional<Column<?, ?>> descriptionColumn = spec.findColumn(columnName);

        assertThat(descriptionColumn).isPresent();
    }

    @Test
    @DisplayName("obtain a column by name")
    void obtainByName() {
        ColumnName columName = ColumnName.of("estimate_in_days");
        Column<?, ?> column = spec().get(columName);

        assertThat(column.type()).isEqualTo(Integer.class);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `IAE` when the column with the specified name is not found")
    void throwOnColumnNotFound() {
        ColumnName nonExistent = ColumnName.of("non-existent-column");

        assertThrows(IllegalArgumentException.class, () -> spec().get(nonExistent));
    }

    @Test
    @DisplayName("search for a column by name")
    void searchByName() {
        ColumnName existingColumn = ColumnName.of("name");
        Optional<Column<?, ?>> column = spec().findColumn(existingColumn);

        assertThat(column).isPresent();
    }

    @Test
    @DisplayName("return empty `Optional` when searching for a non-existent column")
    void returnEmptyOptionalForNonExistent() {
        ColumnName nonExistent = ColumnName.of("non-existent-column");
        Optional<Column<?, ?>> result = spec().findColumn(nonExistent);

        assertThat(result).isEmpty();
    }

    //TODO:2021-01-18:alex.tymchenko: test the types of the `column`-marked columns.
    @Test
    @DisplayName("return the list of columns")
    void returnColumns() {
        int lifecycleColumnCount = EntityRecordColumn.names()
                                                     .size();
        int protoColumnCount = 4;

        int expectedSize = lifecycleColumnCount + protoColumnCount;
        assertThat(spec().columnCount()).isEqualTo(expectedSize);

    }

    @Test
    @DisplayName("extract values from entity")
    void extractColumnValues() {
        TaskViewProjection projection = new TaskViewProjection();
        Map<ColumnName, Object> values = spec().valuesIn(projection);

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
}
