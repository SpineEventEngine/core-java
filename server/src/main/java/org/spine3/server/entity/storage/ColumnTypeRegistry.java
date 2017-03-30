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
 * @author Dmytro Dashenkov
 */
public class ColumnTypeRegistry {

    private static final ColumnTypeRegistry EMPTY_INSTANCE = newBuilder().build();

    private final ImmutableMap<Class, ColumnType> columnTypeMap;

    private ColumnTypeRegistry(Builder builder) {
        this.columnTypeMap = ImmutableMap.copyOf(builder.columnTypeMap);
    }

    /**
     * @return an empty registry, used for
     * {@linkplain org.spine3.server.storage.memory.InMemoryStorageFactory in-memory storages} etc.
     */
    public static ColumnTypeRegistry empty() {
        return EMPTY_INSTANCE;
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
    public ColumnType get(Column<?> field) {
        checkNotNull(field);
        final Class javaType = field.getType();
        final ColumnType type = columnTypeMap.get(javaType);
        checkState(type != null,
                   "Could not find storage type for %s.",
                   javaType.getName());
        return type;
    }

    @VisibleForTesting
    Map<Class, ColumnType> getColumnTypeMap() {
        return Collections.unmodifiableMap(columnTypeMap);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ColumnTypeRegistry src) {
        final Builder builder = new Builder();
        builder.columnTypeMap.putAll(src.columnTypeMap);
        return builder;
    }

    public static class Builder {

        private final Map<Class, ColumnType> columnTypeMap = new HashMap<>();

        private Builder() {
        }

        /**
         * Create the mapping between the Java class and the database
         * {@linkplain ColumnType column type}.
         *
         * @param javaType   the Java class to map the value from
         * @param columnType the the database {@linkplain ColumnType column type} to map value to
         * @param <J>        the Java type
         * @return self for call chaining
         */
        public <J> Builder put(Class<J> javaType, ColumnType<J, ?, ?, ?> columnType) {
            columnTypeMap.put(javaType, columnType);
            return this;
        }

        public ColumnTypeRegistry build() {
            return new ColumnTypeRegistry(this);
        }
    }
}
