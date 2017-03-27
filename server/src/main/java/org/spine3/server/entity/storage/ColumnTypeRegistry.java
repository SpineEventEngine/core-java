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
import org.spine3.server.entity.storage.reflect.Column;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnTypeRegistry {

    private static final ColumnTypeRegistry EMPTY_INSTANCE = newBuilder().build();

    private final Map<Class, ColumnType> columnTypeMap;

    private ColumnTypeRegistry(Map<Class, ColumnType> columnTypeMap) {
        this.columnTypeMap = columnTypeMap;
    }

    public static ColumnTypeRegistry empty() {
        return EMPTY_INSTANCE;
    }

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

    public static <R, C> Builder<R, C> newBuilder() {
        return new Builder<>();
    }

    public static class Builder<R, C> {

        private final Map<Class, ColumnType> columnTypeMap = new HashMap<>();

        private Builder() {
        }

        public <J, S> Builder put(Class<J> javaType, ColumnType<J, S, R, C> columnType) {
            columnTypeMap.put(javaType, columnType);
            return this;
        }

        public ColumnTypeRegistry build() {
            return new ColumnTypeRegistry(ImmutableMap.copyOf(columnTypeMap));
        }
    }
}
