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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.SPI;
import io.spine.client.ColumnFilter;
import io.spine.client.OrderBy;
import io.spine.server.storage.RecordStorage;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;

/**
 * The parameters of an {@link EntityQuery}.
 *
 * <p>{@code QueryParameters} are passed into the {@link io.spine.server.storage.Storage Storage}
 * implementations.
 */
@SPI /* Available to SPI users, providing own {@code Storage} implementations. */
public final class QueryParameters implements Iterable<CompositeQueryParameter>, Serializable {

    private static final long serialVersionUID = 0L;
    static final String FIELD_PARAMETERS = "parameters";

    private final ImmutableCollection<CompositeQueryParameter> parameters;

    /**
     * A flag that shows if the current instance of {@code CompositeQueryParameter} has
     * the {@link io.spine.server.storage.LifecycleFlagField lifecycle attributes} set of not.
     *
     * <p>This flag turns into {@code true} if at least one of the underlying
     * {@linkplain CompositeQueryParameter parameters}
     * {@linkplain CompositeQueryParameter#hasLifecycle() contains Lifecycle attributes}. Otherwise
     * it is {@code false}.
     */
    private final boolean hasLifecycle;
    private final int limit;
    private final OrderBy orderBy;

    private QueryParameters(Builder builder) {
        this.parameters = builder.getParameters()
                                 .build();
        this.limit = builder.limit();
        this.orderBy = builder.orderBy();
        this.hasLifecycle = builder.hasLifecycle;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(QueryParameters parameters) {
        return new Builder()
                .addAll(parameters)
                .orderBy(parameters.orderBy())
                .limit(parameters.limit());
    }
    
    public static QueryParameters activeEntityQueryParams(RecordStorage<?> storage) {
        Map<String, EntityColumn> lifecycleColumns = storage.entityLifecycleColumns();
        EntityColumn archivedColumn = lifecycleColumns.get(archived.name());
        EntityColumn deletedColumn = lifecycleColumns.get(deleted.name());
        CompositeQueryParameter lifecycleParameter = CompositeQueryParameter.from(
                ImmutableMultimap.of(archivedColumn, eq(archivedColumn.getStoredName(), false),
                                     deletedColumn, eq(deletedColumn.getStoredName(), false)),
                ALL
        );
        return newBuilder().add(lifecycleParameter).build();
    }

    /**
     * Returns an iterator over the {@linkplain ColumnFilter column filters}.
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

    public OrderBy orderBy() {
        return orderBy;
    }

    public boolean ordered() {
        return !orderBy.equals(OrderBy.getDefaultInstance());
    }

    public int limit() {
        return limit;
    }

    public boolean limited() {
        return limit != 0;
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

        private final ImmutableCollection.Builder<CompositeQueryParameter> parameters;

        private boolean hasLifecycle;
        private OrderBy orderBy;
        private int limit;

        private Builder() {
            parameters = ImmutableList.builder();
            orderBy = OrderBy.getDefaultInstance();
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

        public ImmutableCollection.Builder<CompositeQueryParameter> getParameters() {
            return parameters;
        }

        public Builder limit(int value) {
            limit = value;
            return this;
        }

        public int limit() {
            return limit;
        }

        public Builder orderBy(OrderBy orderBy) {
            checkNotNull(orderBy);
            this.orderBy = orderBy;
            return this;
        }

        public OrderBy orderBy() {
            return orderBy;
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
