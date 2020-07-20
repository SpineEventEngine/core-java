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

import com.google.common.collect.ImmutableSet;
import io.spine.query.CustomColumn;
import io.spine.query.EntityColumn;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.entity.storage.given.TaskListViewProjection;
import io.spine.server.entity.storage.given.TaskViewProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.server.entity.storage.AssertColumns.assertContains;

@SuppressWarnings({"rawtypes", "unchecked"}) // using `Scanner` with no generic args for simplicity
@DisplayName("`Scanner` should")
class ScannerTest {

    @Test
    @DisplayName("extract system columns from the entity class")
    void extractSystemColumns() {
        EntityClass<TaskViewProjection> entityClass = asEntityClass(TaskViewProjection.class);
        Scanner scanner = new Scanner(entityClass);
        ImmutableSet<CustomColumn<?, ?>> systemColumns = scanner.systemColumns();

        assertThat(systemColumns)
                .containsExactlyElementsIn(EntityRecordColumn.columns());
    }


    @Test
    @DisplayName("extract simple Protobuf field based columns")
    void extractSimpleColumns() {
        EntityClass<TaskListViewProjection> entityClass =
                asEntityClass(TaskListViewProjection.class);
        Scanner scanner = new Scanner(entityClass);

        ImmutableSet<EntityColumn<?, ?>> columns = scanner.simpleColumns();

        assertContains(columns, "description");
    }
}
