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

package io.spine.server.projection;

import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.Columns;
import io.spine.server.projection.given.SavingProjection;
import io.spine.server.storage.StorageField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth8.assertThat;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.server.storage.VersionField.version;

@Nested
@DisplayName("Projection should have columns")
class ProjectionColumnTest {

    @Test
    @DisplayName("`version`")
    void version() {
        assertHasColumn(SavingProjection.class, version);
    }

    @Test
    @DisplayName("`archived` and `deleted`")
    void lifecycleColumns() {
        assertHasColumn(SavingProjection.class, archived);
        assertHasColumn(SavingProjection.class, deleted);
    }

    private static void assertHasColumn(Class<? extends Projection<?, ?, ?>> projectionType,
                                        StorageField storageField) {
        Columns columns = Columns.of(projectionType);
        ColumnName columnName = ColumnName.of(storageField);
        Optional<Column> result = columns.find(columnName);
        assertThat(result).isPresent();
    }
}
