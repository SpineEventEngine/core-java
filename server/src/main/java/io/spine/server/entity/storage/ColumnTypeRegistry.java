/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A registry of type conversion strategies for the {@linkplain EntityColumn entity columns}.
 *
 * <p>To register new {@link Class} to {@link ColumnType} mapping, do:
 * <pre>
 *     {@code
 *         final VarcharDateType varcharType = new VarcharDateType();
 *         final ColumnTypeRegistry registry = ColumnTypeRegistry.newBuilder()
 *                                                               .put(Date.class, varcharType)
 *                                                               .build();
 *
 *         MyJdbcBasedStorageFactory.newBuilder()
 *                                  // ...
 *                                  .setColumnTypeRegistry(registry)
 *                                  .build();
 *     }
 * </pre>
 *
 * <p>To retrieve the {@link ColumnType} instance (in case if you implement your own
 * {@linkplain io.spine.server.storage.Storage Storage}) call {@link #get(EntityColumn)}
 * on the column which you'd like to retrieve the {@linkplain ColumnType type} for.
 *
 * <p>Note, that in the example above, if you try to get the {@linkplain ColumnType type} for
 * a {@link EntityColumn} of e.g. class {@code java.sql.Timestamp}, which {@code extends Date}
 * unless you specify a {@link ColumnType} for {@code java.sql.Timestamp} explicitly,
 * the same value as for the class {@code Date} will be returned. I.e. the {@link ColumnType}
 * of a parent class can be used for a derived class.
 * But be careful, the {@code interface}s are not supported within this feature.
 *
 * <p>If several {@linkplain ColumnType ColumnTypes} are written under a single {@linkplain Class}
 * object, the latest {@code put} overrides all the previous ones.
 *
 * @param <C> storage-specific implementation of the {@link ColumnType}
 * @author Dmytro Dashenkov
 */
public final class ColumnTypeRegistry<C extends ColumnType> {

    private final ImmutableMap<Class, C> columnTypeMap;

    private ColumnTypeRegistry(Builder<C> builder) {
        this.columnTypeMap = ImmutableMap.copyOf(builder.columnTypeMap);
    }

    /**
     * Retrieves the {@link ColumnType} for specified {@link EntityColumn}.
     *
     * <p>By default, this method returns the {@link ColumnType} for the
     * {@linkplain EntityColumn column's} {@linkplain EntityColumn#getPersistedType() type}.
     *
     * <p>If the {@link ColumnType} was not found by the {@code class} of the {@link EntityColumn},
     * its superclasses are checked one by one until a {@link ColumnType} is found or until
     * the look up reaches the class representing {@link Object}.
     *
     * <p>If no {@link ColumnType} is found, an {@link IllegalStateException} is thrown.
     *
     * @param field the {@link EntityColumn} to get type conversion strategy for
     * @return the {@link ColumnType} for the given {@link EntityColumn}
     */
    public C get(EntityColumn field) {
        checkNotNull(field);

        Class javaType = field.getPersistedType();
        javaType = Primitives.wrap(javaType);
        C type = null;

        while (type == null && javaType != null) {
            type = columnTypeMap.get(javaType);
            javaType = javaType.getSuperclass();
        }

        checkState(type != null,
                   "Could not find storage type for %s.",
                   field.getPersistedType());
        return type;
    }

    @VisibleForTesting
    Map<Class, C> getColumnTypeMap() {
        return Collections.unmodifiableMap(columnTypeMap);
    }

    public static <C extends ColumnType> Builder<C> newBuilder() {
        return new Builder<>();
    }

    @SuppressWarnings({
            "unchecked" /* Unchecked copying from the src instance never leads to a failure,
                           since checked while writing into the instance itself.  */,
            "CheckReturnValue" // calling builder
    })
    public static <C extends ColumnType> Builder<C> newBuilder(ColumnTypeRegistry<C> src) {
        Builder<C> builder = newBuilder();
        for (Map.Entry<Class, C> typeMapping : src.columnTypeMap.entrySet()) {
            builder.put(typeMapping.getKey(), typeMapping.getValue());
        }
        return builder;
    }

    public static class Builder<C extends ColumnType> {

        private final Map<Class, C> columnTypeMap = new HashMap<>();

        /** Prevents instantiation from outside. */
        private Builder() {
        }

        /**
         * Create a mapping between the Java class and the database
         * {@linkplain ColumnType column type}.
         *
         * @param javaType   the Java class to map the value from
         * @param columnType the the database {@linkplain ColumnType column type} to map value to;
         *                   must be an instance of {@code <C>}
         * @param <J>        the Java type
         * @return self for call chaining
         */
        @CanIgnoreReturnValue
        public <J> Builder<C> put(Class<J> javaType, ColumnType<J, ?, ?, ?> columnType) {
            checkNotNull(javaType);
            checkNotNull(columnType);

            @SuppressWarnings("unchecked")
            C columnTypeResolved = (C) columnType;
            Class<J> wrapped = Primitives.wrap(javaType);
            columnTypeMap.put(wrapped, columnTypeResolved);
            return this;
        }

        public ColumnTypeRegistry<C> build() {
            return new ColumnTypeRegistry<>(this);
        }
    }
}
