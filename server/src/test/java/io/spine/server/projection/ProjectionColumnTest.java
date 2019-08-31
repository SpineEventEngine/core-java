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

package io.spine.server.projection;

import io.spine.core.Version;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.projection.given.SavingProjection;
import io.spine.server.storage.StorageField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.server.storage.VersionField.version;

@Nested
@DisplayName("Projection should have columns")
class ProjectionColumnTest {

    @Test
    @DisplayName("`version`")
    void version() {
        assertHasColumn(SavingProjection.class, version, Version.class);
    }

    @Test
    @DisplayName("`archived` and `deleted`")
    void lifecycleColumns() {
        assertHasColumn(SavingProjection.class, archived, boolean.class);
        assertHasColumn(SavingProjection.class, deleted, boolean.class);
    }

    private static void assertHasColumn(Class<? extends Projection<?, ?, ?>> projectionType,
                                        StorageField columnName,
                                        Class<?> columnType) {
        EntityColumnCache cache = EntityColumnCache.initializeFor(projectionType);
        EntityColumn column = cache.findColumn(columnName.toString());
        assertThat(column).isNotNull();
        assertThat(column.type()).isEqualTo(columnType);
    }
}
