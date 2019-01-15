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

import com.google.common.collect.ImmutableSet;
import io.spine.server.storage.EntityField;
import io.spine.server.storage.LifecycleFlagField;

import java.util.Collection;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utilities for testing columns.
 */
final class ColumnTests {

    private static final ImmutableSet<String> lifecycleColumns =
            Stream.of(LifecycleFlagField.values())
                  .map(Enum::name)
                  .collect(toImmutableSet());

    static final ImmutableSet<String> defaultColumns =
            ImmutableSet.<String>builder()
                    .addAll(lifecycleColumns)
                    .add(EntityField.version.name())
                    .build();

    /** Prevent instantiation of this utility class. */
    private ColumnTests() {
    }

    /**
     * Verifies that a column collection contains the specified columns.
     */
    static void assertContainsColumns(Collection<EntityColumn> actual, String... columnNames) {
        assertTrue(containsColumns(actual, columnNames));
    }

    /**
     * Verifies that a column collection does not contain the specified columns.
     */
    static void assertNotContainsColumns(Collection<EntityColumn> actual, String... columnNames) {
        assertFalse(containsColumns(actual, columnNames));
    }

    /**
     * Checks whether a given column collection contains the specified columns.
     *
     * <p>The collection can be safely stored in {@link ImmutableSet} as it will never contain
     * repeated column names.
     */
    private static boolean containsColumns(Collection<EntityColumn> actual, String... columnNames) {
        checkNotNull(actual);
        checkNotNull(columnNames);

        ImmutableSet<String> expectedColumns = ImmutableSet.copyOf(columnNames);
        ImmutableSet<String> actualColumns = actual
                .stream()
                .map(EntityColumn::getName)
                .collect(toImmutableSet());
        boolean result = expectedColumns.equals(actualColumns);
        return result;
    }
}
