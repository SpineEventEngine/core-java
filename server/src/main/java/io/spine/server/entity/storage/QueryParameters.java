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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.SPI;
import io.spine.client.Filter;
import io.spine.client.Filters;
import io.spine.server.storage.Column;
import io.spine.server.storage.Columns;
import io.spine.server.storage.MessageColumn;
import io.spine.server.storage.RecordStorage;

import java.util.Iterator;
import java.util.Optional;

import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;

/**
 * The parameters of an {@link EntityQuery}.
 *
 * <p>{@code QueryParameters} are passed into the {@link io.spine.server.storage.Storage Storage}
 * implementations.
 */
@SPI // Available to SPI users providing own `Storage` implementations.
public final class QueryParameters implements Iterable<CompositeQueryParameter> {

    public static final String FIELD_PARAMETERS = "parameters";

    private final ImmutableList<CompositeQueryParameter> parameters;

    /**
     * A flag that shows if the current instance of {@code CompositeQueryParameter} has
     * the {@link io.spine.server.storage.LifecycleFlagField lifecycle attributes} set or not.
     *
     * <p>This flag turns into {@code true} if at least one of the underlying
     * {@linkplain CompositeQueryParameter parameters}
     * {@linkplain CompositeQueryParameter#hasLifecycle() contains Lifecycle attributes}. Otherwise
     * it is {@code false}.
     */
    private final boolean hasLifecycle;

    private QueryParameters(Builder builder) {
        this.parameters = builder.getParameters()
                                 .build();
        this.hasLifecycle = builder.hasLifecycle;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(QueryParameters parameters) {
        return new Builder()
                .addAll(parameters);
    }

    /**
     * Creates a new {@code QueryParameters} instance which includes filters for column lifecycle
     * flags to equal {@code false}. Such an entity is considered to be active.
     *
     * @param storage
     *         the record storage persisting target entities
     * @return new {@code QueryParameters} with {@linkplain io.spine.server.entity.LifecycleFlags
     *         lifecycle flags} filters
     */
    @Deprecated
    //TODO:2020-03-19:alex.tymchenko: remove this one.
    public static QueryParameters activeEntityQueryParams(RecordStorage<?> storage) {
        ImmutableMap<ColumnName, Column> lifecycleColumns = storage.lifecycleColumns();

        ColumnName archivedColumnName = ColumnName.of(archived);
        ColumnName deletedColumnName = ColumnName.of(deleted);
        Column archivedColumn = lifecycleColumns.get(archivedColumnName);
        Column deletedColumn = lifecycleColumns.get(deletedColumnName);
        boolean entityHasLifecycle = archivedColumn != null && deletedColumn != null;
        if (!entityHasLifecycle) {
            return newBuilder().build();
        }
        CompositeQueryParameter lifecycleParameter = CompositeQueryParameter.from(
                ImmutableMultimap.of(archivedColumn, Filters.eq(archivedColumnName.value(), false),
                                     deletedColumn, Filters.eq(deletedColumnName.value(), false)),
                ALL
        );
        return newBuilder().add(lifecycleParameter)
                           .build();
    }

    //TODO:2020-04-01:alex.tymchenko: use `EntityColumns` here.
    public static QueryParameters activeEntityQueryParams(Columns<?> columns) {
        ColumnName archivedColumnName = ColumnName.of(archived);
        ColumnName deletedColumnName = ColumnName.of(deleted);
        Optional<Column> archivedColumn = columns.find(archivedColumnName);
        Optional<Column> deletedColumn = columns.find(deletedColumnName);
        boolean entityHasLifecycle = archivedColumn.isPresent() && deletedColumn.isPresent();
        if (!entityHasLifecycle) {
            return newBuilder().build();
        }
        CompositeQueryParameter lifecycleParameter = CompositeQueryParameter.from(
                ImmutableMultimap.of(archivedColumn.get(),
                                     Filters.eq(archivedColumnName.value(), false),
                                     deletedColumn.get(),
                                     Filters.eq(deletedColumnName.value(), false)),
                ALL
        );
        return newBuilder().add(lifecycleParameter)
                           .build();
    }

    public static <V> QueryParameters eq(MessageColumn<?, ?> column, V value) {
        return forSingleColumn(column, Filters.eq(column.name()
                                                        .value(), value));
    }

    public static <V> QueryParameters gt(MessageColumn<?, ?> column, V value) {
        return forSingleColumn(column, Filters.gt(column.name()
                                                        .value(), value));
    }

    public static <V> QueryParameters ge(MessageColumn<?, ?> column, V value) {
        return forSingleColumn(column, Filters.ge(column.name()
                                                        .value(), value));
    }

    public static <V> QueryParameters lt(MessageColumn<?, ?> column, V value) {
        return forSingleColumn(column, Filters.lt(column.name()
                                                        .value(), value));
    }

    public static <V> QueryParameters le(MessageColumn<?, ?> column, V value) {
        return forSingleColumn(column, Filters.le(column.name()
                                                        .value(), value));
    }

    private static <V> QueryParameters forSingleColumn(MessageColumn<?, ?> column, Filter filter) {
        ImmutableMultimap<Column, Filter> filters = ImmutableMultimap.of(column, filter);
        CompositeQueryParameter parameter = CompositeQueryParameter.from(filters, ALL);
        return newBuilder().add(parameter)
                           .build();
    }

    /**
     * Returns an iterator over the {@linkplain Filter column filters}.
     *
     * <p>The resulting {@code Iterator} throws {@link UnsupportedOperationException} on call
     * to {@link Iterator#remove() Iterator.remove()}.
     *
     * @return an {@link Iterator}.
     */
    @Override
    public Iterator<CompositeQueryParameter> iterator() {
        return parameters.iterator();
    }

    /**
     * Verifies whether this parameters include filters by
     * the {@linkplain io.spine.server.entity.LifecycleFlags Entity lifecycle flags} or not.
     */
    public boolean isLifecycleAttributesSet() {
        return hasLifecycle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryParameters that = (QueryParameters) o;
        return Objects.equal(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameters);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add(FIELD_PARAMETERS, parameters)
                          .add("hasLifecycle", hasLifecycle)
                          .toString();
    }

    /**
     * The builder for the {@code QueryParameters}.
     */
    public static class Builder {

        private final ImmutableList.Builder<CompositeQueryParameter> parameters;

        private boolean hasLifecycle;

        private Builder() {
            parameters = ImmutableList.builder();
        }

        @CanIgnoreReturnValue
        public Builder add(CompositeQueryParameter parameter) {
            parameters.add(parameter);
            hasLifecycle |= parameter.hasLifecycle();
            return this;
        }

        @CanIgnoreReturnValue
        public Builder addAll(Iterable<CompositeQueryParameter> parameters) {
            for (CompositeQueryParameter parameter : parameters) {
                add(parameter);
            }
            return this;
        }

        public ImmutableList.Builder<CompositeQueryParameter> getParameters() {
            return parameters;
        }

        /**
         * Creates a new instance of {@code QueryParameters} with the collected parameters.
         *
         * @return a new instance of {@code QueryParameters}
         */
        public QueryParameters build() {
            return new QueryParameters(this);
        }
    }
}
