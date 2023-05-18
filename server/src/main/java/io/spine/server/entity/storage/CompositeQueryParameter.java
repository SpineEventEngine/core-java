/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.client.Filter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;

/**
 * A set of {@link Filter} instances joined by a logical
 * {@link CompositeOperator composite operator}.
 */
public final class CompositeQueryParameter {

    private final CompositeOperator operator;

    private final ImmutableMultimap<Column, Filter> filters;

    /**
     * A flag that shows if current instance of {@code CompositeQueryParameter} has
     * the {@link io.spine.server.storage.LifecycleFlagField lifecycle attributes} set or not.
     */
    private final boolean hasLifecycle;

    /**
     * Creates a new instance of {@code CompositeQueryParameter} from the given filters joined
     * by the given operator.
     *
     * @param filters
     *         the filters to aggregate
     * @param operator
     *         the operator to apply to the given filters
     * @return new instance of {@code CompositeQueryParameter}
     */
    static CompositeQueryParameter from(Multimap<Column, Filter> filters,
                                        CompositeOperator operator) {
        checkNotNull(filters);
        checkNotNull(operator);
        checkArgument(operator.getNumber() > 0, "Invalid aggregating operator %s.", operator);

        return new CompositeQueryParameter(operator, filters);
    }

    private CompositeQueryParameter(CompositeOperator operator,
                                    Multimap<Column, Filter> filters) {
        this.operator = operator;
        this.filters = ImmutableMultimap.copyOf(filters);
        this.hasLifecycle = containsLifecycle(filters.keySet());
    }

    private static boolean containsLifecycle(Iterable<Column> columns) {
        boolean result = Streams.stream(columns)
                                .anyMatch(CompositeQueryParameter::isLifecycleColumn);
        return result;
    }

    private static boolean isLifecycleColumn(Column column) {
        checkNotNull(column);
        boolean result = archived.name().equals(column.name().value())
                || deleted.name().equals(column.name().value());
        return result;
    }

    /**
     * Obtains the composite operator.
     */
    public CompositeOperator operator() {
        return operator;
    }

    /**
     * Returns the joined entity column {@linkplain Filter filters}.
     */
    public ImmutableMultimap<Column, Filter> filters() {
        return filters;
    }

    /**
     * Merges current instance with the given instances by the rules of conjunction.
     *
     * <p>The resulting {@code CompositeQueryParameter} contains all the filters of the current and
     * the given instances joined by the {@linkplain CompositeOperator#ALL conjunction operator}.
     *
     * @param other
     *         the instances of the {@code CompositeQueryParameter} to merge with
     * @return new instance of {@code CompositeQueryParameter} joining all the parameters
     */
    public CompositeQueryParameter conjunct(Iterable<CompositeQueryParameter> other) {
        checkNotNull(other);

        Multimap<Column, Filter> mergedFilters = LinkedListMultimap.create();
        mergedFilters.putAll(filters);
        for (CompositeQueryParameter parameter : other) {
            mergedFilters.putAll(parameter.filters());
        }
        CompositeQueryParameter result = from(mergedFilters, ALL);
        return result;
    }

    /**
     * Merges current instance with the given filter.
     *
     * <p>The resulting {@code CompositeQueryParameter} is joined with
     * the {@link CompositeOperator#ALL ALL} operator.
     *
     * @param column
     *         the {@link Column} to add the filter to
     * @param filter
     *         the value of the filter to add
     * @return new instance of {@code CompositeQueryParameter} merged from current instance and
     * the given filter
     */
    public CompositeQueryParameter and(Column column, Filter filter) {
        checkNotNull(column);
        checkNotNull(filter);

        Multimap<Column, Filter> newFilters = HashMultimap.create(filters);
        newFilters.put(column, filter);
        CompositeQueryParameter parameter = from(newFilters, ALL);
        return parameter;
    }

    /**
     * Returns {@code true} if this parameter contains filters by
     * the {@linkplain io.spine.server.entity.LifecycleFlags Entity lifecycle columns},
     * {@code false} otherwise.
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
        return operator() == parameter.operator() &&
                Objects.equal(filters(), parameter.filters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(operator(), filters());
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // In generated code.
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("operator", operator)
                          .add("filters", filters)
                          .toString();
    }
}
