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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.testing.NullPointerTester;
import io.spine.client.Filter;
import io.spine.server.entity.storage.given.TestEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeFilter.CompositeOperator.CCF_CO_UNDEFINED;
import static io.spine.client.Filters.eq;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CompositeQueryParameter should")
class CompositeQueryParameterTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testStaticMethods(CompositeQueryParameter.class, PACKAGE);
    }

    @Test
    @DisplayName("fail to construct for invalid operator")
    void rejectInvalidOperator() {
        assertThrows(IllegalArgumentException.class,
                     () -> CompositeQueryParameter.from(ImmutableMultimap.of(), CCF_CO_UNDEFINED));
    }

    @Test
    @DisplayName("merge with other instances")
    void mergeWithOtherInstances() {
        Columns columns = Columns.of(TestEntity.class);

        ColumnName archivedColumnName = ColumnName.of(archived);
        ColumnName deletedColumnName = ColumnName.of(deleted);

        Column archivedColumn = columns.get(archivedColumnName);
        Column deletedColumn = columns.get(deletedColumnName);

        Filter archived = eq(archivedColumnName.value(), true);
        Filter deleted = eq(deletedColumnName.value(), false);

        CompositeQueryParameter lifecycle =
                CompositeQueryParameter.from(
                        ImmutableMultimap.of(archivedColumn, archived, deletedColumn, deleted),
                        ALL
                );

        // Check
        assertEquals(lifecycle.operator(), ALL);

        Multimap<Column, Filter> asMultimap = lifecycle.filters();

        assertThat(asMultimap.get(archivedColumn)).containsExactly(archived);
        assertThat(asMultimap.get(deletedColumn)).containsExactly(deleted);
    }

    @Test
    @DisplayName("merge with single filter")
    void mergeWithSingleFilter() {
        Columns columns = Columns.of(TestEntity.class);

        ColumnName archivedColumnName = ColumnName.of(archived);
        ColumnName deletedColumnName = ColumnName.of(deleted);

        Column archivedColumn = columns.get(archivedColumnName);
        Column deletedColumn = columns.get(deletedColumnName);

        Filter archived = eq(archivedColumnName.value(), false);
        Filter deleted = eq(deletedColumnName.value(), false);

        CompositeQueryParameter lifecycle =
                CompositeQueryParameter.from(
                        ImmutableMultimap.of(archivedColumn, archived, deletedColumn, deleted),
                        ALL
                );

        // Check
        assertEquals(lifecycle.operator(), ALL);

        Multimap<Column, Filter> asMultimap = lifecycle.filters();

        assertThat(asMultimap.get(archivedColumn)).containsExactly(archived);
        assertThat(asMultimap.get(deletedColumn)).containsExactly(deleted);
    }
}
