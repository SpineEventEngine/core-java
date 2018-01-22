/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter.CompositeOperator;

import javax.annotation.Nullable;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMultimap.copyOf;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;

/**
 * A set of {@link ColumnFilter} instances joined by a logical
 * {@link CompositeOperator composite operator}.
 *
 * @author Dmytro Dashenkov
 */
public final class CompositeQueryParameter implements Serializable {

    private static final Predicate<EntityColumn> isLifecycleColumn = new Predicate<EntityColumn>() {
        @Override
        public boolean apply(@Nullable EntityColumn column) {
            checkNotNull(column);
            final boolean result = archived.name().equals(column.getName())
                    || deleted.name().equals(column.getName());
            return result;
        }
    };

    private static final long serialVersionUID = 0L;

    private final CompositeOperator operator;

    private final ImmutableMultimap<EntityColumn, ColumnFilter> filters;

    /**
     * A flag that shows if current instance of {@code CompositeQueryParameter} has
     * the {@link io.spine.server.storage.LifecycleFlagField lifecycle attributes} set of not.
     */
    private final boolean hasLifecycle;

    /**
     * Creates a new instance of {@code CompositeQueryParameter} from the given filters joined
     * by the given operator.
     *
     * @param filters  the filters to aggregate
     * @param operator the operator to apply to the given filters
     * @return new instance of {@code CompositeQueryParameter}
     */
    static CompositeQueryParameter from(Multimap<EntityColumn, ColumnFilter> filters,
                                        CompositeOperator operator) {
        checkNotNull(filters);
        checkNotNull(operator);
        checkArgument(operator.getNumber() > 0, "Invalid aggregating operator %s.", operator);

        return new CompositeQueryParameter(operator, filters);
    }

    private CompositeQueryParameter(CompositeOperator operator,
                                    Multimap<EntityColumn, ColumnFilter> filters) {
        this.operator = operator;
        this.filters = copyOf(filters);
        this.hasLifecycle = containsLifecycle(filters.keySet());
    }

    private static boolean containsLifecycle(Iterable<EntityColumn> columns) {
        final boolean result = Iterables.any(columns, isLifecycleColumn);
        return result;
    }

    /**
     * @return the composite operator
     */
    public CompositeOperator getOperator() {
        return operator;
    }

    /**
     * @return the joined entity column {@linkplain ColumnFilter filters}
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public ImmutableMultimap<EntityColumn, ColumnFilter> getFilters() {
        return filters;
    }

    /**
     * Merges current instance with the given instances by the rules of conjunction.
     *
     * <p>The resulting {@code CompositeQueryParameter} contains all the filters of the current and
     * the given instances joined by the {@linkplain CompositeOperator#ALL conjunction operator}.
     *
     * @param other the instances of the {@code CompositeQueryParameter} to merge with
     * @return new instance of {@code CompositeQueryParameter} joining all the parameters
     */
    public CompositeQueryParameter conjunct(Iterable<CompositeQueryParameter> other) {
        checkNotNull(other);

        final Multimap<EntityColumn, ColumnFilter> mergedFilters = LinkedListMultimap.create();
        mergedFilters.putAll(filters);
        for (CompositeQueryParameter parameter : other) {
            mergedFilters.putAll(parameter.getFilters());
        }
        final CompositeQueryParameter result = from(mergedFilters, ALL);
        return result;
    }

    /**
     * Merges current instance with the given filter.
     *
     * <p>The resulting {@code CompositeQueryParameter} is joined with
     * the {@link CompositeOperator#ALL ALL} operator.
     *
     * @param column the {@link EntityColumn} to add the filter to
     * @param columnFilter the value of the filter to add
     * @return new instance of {@code CompositeQueryParameter} merged from current instance and
     * the given filter
     */
    public CompositeQueryParameter and(EntityColumn column, ColumnFilter columnFilter) {
        checkNotNull(column);
        checkNotNull(columnFilter);

        final Multimap<EntityColumn, ColumnFilter> newFilters = HashMultimap.create(filters);
        newFilters.put(column, columnFilter);
        final CompositeQueryParameter parameter = from(newFilters, ALL);
        return parameter;
    }

    /**
     * @return {@code true} if this parameter contains filters by
     * the {@linkplain io.spine.server.entity.LifecycleFlags Entity lifecycle columns},
     * {@code false} otherwise
     */
    public boolean hasLifecycle() {
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
        CompositeQueryParameter parameter = (CompositeQueryParameter) o;
        return getOperator() == parameter.getOperator() &&
                Objects.equal(getFilters(), parameter.getFilters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getOperator(), getFilters());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("operator", operator)
                          .add("filters", filters)
                          .toString();
    }
}
