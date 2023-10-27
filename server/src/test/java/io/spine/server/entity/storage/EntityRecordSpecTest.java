/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import io.spine.base.Identifier;
import io.spine.client.ArchivedColumn;
import io.spine.client.DeletedColumn;
import io.spine.client.VersionColumn;
import io.spine.core.Versions;
import io.spine.protobuf.AnyPacker;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.EntityColumn;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.given.TaskViewProjection;
import io.spine.test.entity.TaskView;
import io.spine.test.entity.TaskViewId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.base.Time.currentTime;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.declaredColumns;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.spec;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("`EntityRecordSpec` should")
class EntityRecordSpecTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EntityRecordSpec.class);
        new NullPointerTester()
                .testAllPublicInstanceMethods(spec());
    }

    // TODO:alex.tymchenko:2023-10-27: rewrite!
//    @Test
//    @DisplayName("be extracted from an entity class")
//    void beExtractedFromEntityClass() {
//        EntityRecordSpec<?, ?, ?> spec = EntityRecordSpec.of(TaskListViewProjection.class);
//        var columnName = ColumnName.of("description");
//        var descriptionColumn = spec.findColumn(columnName);
//
//        assertThat(descriptionColumn).isPresent();
//    }

    @Test
    @DisplayName("obtain a column by name")
    void obtainByName() {
        declaredColumns().forEach(
                EntityRecordSpecTest::testFindAndCompare
        );
    }

    private static void testFindAndCompare(EntityColumn<TaskView, ?> column) {
        var columnName = column.name();
        var type = column.type();

        assertThat(spec().get(columnName)
                         .type()).isEqualTo(type);
    }

    @Test
    @DisplayName("return all definitions of the columns")
    void returnAllColumns() {
        var columns = spec().columns();
        var actualNames = toNames(columns);

        var expected = ImmutableSet.<Column<?, ?>>builder()
                .addAll(declaredColumns())
                .add(ArchivedColumn.instance(),
                     DeletedColumn.instance(),
                     VersionColumn.instance())
                .build();
        var expectedNames = toNames(expected);

        assertThat(actualNames).containsExactlyElementsIn(expectedNames);
    }

    private static ImmutableSet<ColumnName> toNames(ImmutableSet<? extends Column<?, ?>> columns) {
        return columns.stream()
                      .map(Column::name)
                      .collect(toImmutableSet());
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `IAE` when the column with the specified name is not found")
    void throwOnColumnNotFound() {
        var nonExistent = ColumnName.of("non-existent-column");

        assertThrows(IllegalArgumentException.class, () -> spec().get(nonExistent));
    }

    @Test
    @DisplayName("search for a column by name")
    void searchByName() {
        var existingColumn = ColumnName.of("name");
        var column = spec().findColumn(existingColumn);

        assertThat(column).isPresent();
    }

    @Test
    @DisplayName("return empty `Optional` when searching for a non-existent column")
    void returnEmptyOptionalForNonExistent() {
        var nonExistent = ColumnName.of("non-existent-column");
        var result = spec().findColumn(nonExistent);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("return the list of columns")
    void returnColumns() {
        var lifecycleColumnCount = EntityRecordColumn.names()
                                                     .size();
        var protoColumnCount = 4;

        var expectedSize = lifecycleColumnCount + protoColumnCount;
        assertThat(spec().columnCount()).isEqualTo(expectedSize);
    }

    // TODO:alex.tymchenko:2023-10-27:  rewrite or kill?
//    @Test
//    @DisplayName("extract values from entity")
//    void extractColumnValues() {
//        var projection = new TaskViewProjection();
//        var values = spec().valuesIn(projection);
//
//        assertThat(values).containsExactly(
//                ColumnName.of("archived"), projection.isArchived(),
//                ColumnName.of("deleted"), projection.isDeleted(),
//                ColumnName.of("version"), projection.version(),
//                ColumnName.of("name"), projection.state()
//                                                 .getName(),
//                ColumnName.of("estimate_in_days"), projection.state()
//                                                             .getEstimateInDays(),
//                ColumnName.of("status"), projection.state()
//                                                   .getStatus(),
//                ColumnName.of("due_date"), projection.state()
//                                                     .getDueDate()
//        );
//    }

    @Test
    @DisplayName("extract ID value from `EntityRecord`")
    void extractIdValue() {
        var projection = new TaskViewProjection();
        var state = projection.state();
        var id = TaskViewId.newBuilder()
                .setId(93)
                .build();
        var record = EntityRecord.newBuilder()
                .setEntityId(Identifier.pack(id))
                .setState(AnyPacker.pack(state))
                .setVersion(Versions.newVersion(3, currentTime()))
                .build();
        var actual = spec().idFromRecord(record);
        assertThat(actual).isEqualTo(id);
    }
}
