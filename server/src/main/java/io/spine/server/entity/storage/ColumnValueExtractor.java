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

import io.spine.server.entity.Entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class designated to retrieve {@link EntityColumn} values from the given {@link Entity}
 * using specified {@linkplain EntityColumn entity columns}.
 *
 * <p>Each {@code ColumnValueExtractor} instance is created for the specific {@link Entity} and
 * the specific set of {@linkplain EntityColumn columns}.
 *
 * @see Columns
 * @see EntityColumn
 */
class ColumnValueExtractor {

    private final Entity entity;
    private final Collection<EntityColumn> entityColumns;

    private ColumnValueExtractor(Entity entity, Collection<EntityColumn> entityColumns) {
        this.entity = entity;
        this.entityColumns = entityColumns;
    }

    /**
     * Creates an instance of {@code ColumnValueExtractor} for the given {@link Entity} and
     * {@link Collection} of {@linkplain EntityColumn entity columns}.
     *
     * <p>This instance can be further used to
     * {@linkplain ColumnValueExtractor#extractColumnValues() extract} column values from the given
     * {@link Entity}.
     *
     * <p>This method accepts an {@linkplain Collection#isEmpty() empty} collection of columns as an
     * argument, but no values will be extracted in this case.
     *
     * @param entity        the {@link Entity} for which to create the {@code ColumnValueExtractor}
     * @param entityColumns the list of {@linkplain EntityColumn entity columns} to extract from the
     *                      {@link Entity}
     * @return the new instance of the {@code ColumnValueExtractor}
     */
    static ColumnValueExtractor create(Entity entity, Collection<EntityColumn> entityColumns) {
        checkNotNull(entity);
        checkNotNull(entityColumns);

        return new ColumnValueExtractor(entity, entityColumns);
    }

    /**
     * Extracts the {@linkplain EntityColumn column} values for the processed {@link Entity} using
     * specified {@linkplain EntityColumn entity columns}.
     *
     * @return a {@code Map} of the column {@linkplain EntityColumn#getStoredName()
     *         names for storing} to their {@linkplain EntityColumn.MemoizedValue memoized values}
     * @see EntityColumn.MemoizedValue
     */
    Map<String, EntityColumn.MemoizedValue> extractColumnValues() {
        Map<String, EntityColumn.MemoizedValue> values = new HashMap<>(entityColumns.size());
        for (EntityColumn column : entityColumns) {
            String name = column.getStoredName();
            EntityColumn.MemoizedValue value = column.memoizeFor(entity);
            values.put(name, value);
        }
        return values;
    }
}
