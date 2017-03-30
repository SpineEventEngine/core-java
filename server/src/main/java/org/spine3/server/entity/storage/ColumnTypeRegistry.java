/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A registry of type conversion strategies for the Storage Fields.
 *
 * @param <C> storage-specific implementation of the {@link ColumnType}
 * @author Dmytro Dashenkov
 */
public class ColumnTypeRegistry<C extends ColumnType> {

    private final ImmutableMap<Class, C> columnTypeMap;

    private ColumnTypeRegistry(Builder<C> builder) {
        this.columnTypeMap = ImmutableMap.copyOf(builder.columnTypeMap);
    }

    /**
     * @return an empty registry, used for
     * {@linkplain org.spine3.server.storage.memory.InMemoryStorageFactory in-memory storages} etc.
     */
    public static <C extends ColumnType> ColumnTypeRegistry<C> empty() {
        return ColumnTypeRegistry.<C>newBuilder().build();
    }

    /**
     * Retrieves the {@link ColumnType} for specified {@link Column}.
     *
     * <p>By default, this method returns the {@link ColumnType} for the
     * {@linkplain Column column's} {@linkplain Column#getType() type}.
     *
     * @param field the {@link Column} to get type conversion strategy for
     * @return the {@link ColumnType} for the given {@link Column}
     */
    public C get(Column<?> field) {
        checkNotNull(field);
        final Class javaType = field.getType();
        final C type = columnTypeMap.get(javaType);
        checkState(type != null,
                   "Could not find storage type for %s.",
                   javaType.getName());
        return type;
    }

    @VisibleForTesting
    Map<Class, C> getColumnTypeMap() {
        return Collections.unmodifiableMap(columnTypeMap);
    }

    public static <C extends ColumnType> Builder<C> newBuilder() {
        return new Builder<>();
    }

    public static <C extends ColumnType> Builder<C> newBuilder(ColumnTypeRegistry<C> src) {
        final Builder<C> builder = new Builder<>();
        builder.columnTypeMap.putAll(src.columnTypeMap);
        return builder;
    }

    public static class Builder<C extends ColumnType> {

        private final Map<Class, C> columnTypeMap = new HashMap<>();

        private Builder() {
        }

        /**
         * Create the mapping between the Java class and the database
         * {@linkplain ColumnType column type}.
         *
         * @param javaType   the Java class to map the value from
         * @param columnType the the database {@linkplain ColumnType column type} to map value to;
         *                   must be an instance of {@code <C>}
         * @param <J>        the Java type
         * @return self for call chaining
         */
        public <J> Builder<C> put(Class<J> javaType, ColumnType<J, ?, ?, ?> columnType) {
            @SuppressWarnings("unchecked") final C columnTypeResolved = (C) columnType;
            columnTypeMap.put(javaType, columnTypeResolved);
            return this;
        }

        public ColumnTypeRegistry<C> build() {
            return new ColumnTypeRegistry<>(this);
        }
    }
}
