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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.FieldPath;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.client.Filter;
import io.spine.client.TargetFilters;
import io.spine.server.storage.RecordStorage;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.primitives.Primitives.wrap;
import static io.spine.protobuf.TypeConverter.toObject;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

/**
 * A utility class for working with {@link EntityQuery} instances.
 *
 * @see EntityQuery
 */
@Internal
public final class EntityQueries {

    /**
     * Prevents instantiation of this utility class.
     */
    private EntityQueries() {
    }

    /**
     * Creates new {@link EntityQuery} instances for the given {@link TargetFilters} and
     * {@link RecordStorage}.
     *
     * @param filters
     *         filters for the Entities specifying the query predicate
     * @param storage
     *         a storage for which the query is created
     * @return new instance of the {@code EntityQuery} with the specified attributes
     */
    public static <I> EntityQuery<I> from(TargetFilters filters,
                                          RecordStorage<I> storage) {
        checkNotNull(filters);
        checkNotNull(storage);

        Columns entityColumns = storage.columns();
        EntityQuery<I> result = from(filters, entityColumns);
        return result;
    }

    @VisibleForTesting
    static <I> EntityQuery<I> from(TargetFilters filters, Columns columns) {
        checkNotNull(filters);
        checkNotNull(columns);

        QueryParameters queryParams = toQueryParams(filters, columns);
        List<I> ids = toIdentifiers(filters);

        EntityQuery<I> result = EntityQuery.of(ids, queryParams);
        return result;
    }

    private static QueryParameters toQueryParams(TargetFilters filters, Columns columns) {

        List<CompositeQueryParameter> parameters = getFiltersQueryParams(filters, columns);
        return newQueryParameters(parameters);
    }

    private static List<CompositeQueryParameter>
    getFiltersQueryParams(TargetFilters filters, Columns columns) {
        return filters.getFilterList()
                      .stream()
                      .map(filter -> queryParameterFromFilter(filter, columns))
                      .collect(toList());
    }

    private static QueryParameters newQueryParameters(List<CompositeQueryParameter> parameters) {
        return QueryParameters
                .newBuilder()
                .addAll(parameters)
                .build();
    }

    private static CompositeQueryParameter
    queryParameterFromFilter(CompositeFilter filter, Columns columns) {
        Multimap<Column, Filter> filters = splitFilters(filter, columns);
        CompositeOperator operator = filter.getOperator();
        return CompositeQueryParameter.from(filters, operator);
    }

    private static Multimap<Column, Filter>
    splitFilters(CompositeFilter filter, Columns columns) {
        Multimap<Column, Filter> filters = HashMultimap.create(filter.getFilterCount(), 1);
        for (Filter columnFilter : filter.getFilterList()) {
            Column column = findMatchingColumn(columnFilter, columns);
            checkFilterType(column, columnFilter);
            filters.put(column, columnFilter);
        }
        return filters;
    }

    private static Column findMatchingColumn(Filter filter, Columns columns) {
        FieldPath fieldPath = filter.getFieldPath();
        checkArgument(fieldPath.getFieldNameCount() == 1,
                      "Incorrect Column name in Entity Filter: %s",
                      join(".", fieldPath.getFieldNameList()));
        String column = fieldPath.getFieldName(0);
        ColumnName columnName = ColumnName.of(column);
        return columns.get(columnName);
    }

    private static void checkFilterType(Column column, Filter filter) {
        Class<?> expectedType = column.type();
        Any filterConvent = filter.getValue();
        Object filterValue = toObject(filterConvent, expectedType);
        Class<?> actualType = filterValue.getClass();
        checkArgument(wrap(expectedType).isAssignableFrom(wrap(actualType)),
                      "Column type mismatch. Column `%s` cannot have value `%s`.",
                      column,
                      filterValue);
    }

    @SuppressWarnings("unchecked" /* The caller is responsible to pass the proper IDs. */)
    private static <I> List<I> toIdentifiers(TargetFilters filters) {
        ImmutableList<I> result =
                filters.getIdFilter()
                       .getIdList()
                       .stream()
                       .map(Identifier::unpack)
                       .map(i -> (I) i)
                       .collect(toImmutableList());
        return result;
    }
}
