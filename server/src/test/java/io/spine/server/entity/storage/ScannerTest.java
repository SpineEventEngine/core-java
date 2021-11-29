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

import com.google.common.collect.ImmutableSet;
import io.spine.client.ArchivedColumn;
import io.spine.client.DeletedColumn;
import io.spine.client.VersionColumn;
import io.spine.query.Column;
import io.spine.server.entity.storage.given.TaskListViewProjection;
import io.spine.server.entity.storage.given.TaskViewProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.server.entity.storage.AssertColumns.assertContains;

@DisplayName("`Scanner` should")
class ScannerTest {

    @Test
    @DisplayName("include `Entity` lifecycle columns into the scan results")
    void extractSystemColumns() {
        var entityClass = asEntityClass(TaskViewProjection.class);
        var scanner = new Scanner<>(entityClass);
        var columns = scanner.columns();

        var names = columns.stream()
                .map(Column::name)
                .collect(toImmutableSet());

        assertThat(names).containsAtLeastElementsIn(
                ImmutableSet.of(
                        ArchivedColumn.instance().name(),
                        DeletedColumn.instance().name(),
                        VersionColumn.instance().name()
                )
        );
    }

    @Test
    @DisplayName("extract the columns declared with `(column)` option in the Protobuf message")
    void extractSimpleColumns() {
        var entityClass = asEntityClass(TaskListViewProjection.class);
        var scanner = new Scanner<>(entityClass);

        var columns = scanner.stateColumns();

        assertContains(columns, "description");
    }
}
