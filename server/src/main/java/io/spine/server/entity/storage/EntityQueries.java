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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.FieldPath;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.client.Filter;
import io.spine.client.IdFilter;
import io.spine.client.OrderBy;
import io.spine.client.Pagination;
import io.spine.client.TargetFilters;
import io.spine.server.storage.RecordStorage;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.HashMultimap.create;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Primitives.wrap;
import static io.spine.protobuf.TypeConverter.toObject;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

/**
 * A utility class for working with {@link EntityQuery} instances.
 *
 * @author Dmytro Dashenkov
 * @see EntityQuery
 */
@Internal
public final class EntityQueries {

    /** Prevents instantiation of this utility class. */
    private EntityQueries() {
    }

    /**
     * Creates new {@link EntityQuery} instances for the given {@link OrderBy},
     * {@link TargetFilters}, {@link Pagination}, and {@link RecordStorage}.
     *
     * @param filters
     *         filters for the Entities specifying the query predicate
     * @param orderBy
     *         an order to sort the data matching filters by
     * @param pagination
     *         a specification of a way to paginate the query results
     * @param storage
     *         a storage for which the query is created
     * @return new instance of the {@code EntityQuery} with the specified attributes
     */
    public static <I> EntityQuery<I> from(TargetFilters filters,
                                          OrderBy orderBy,
                                          Pagination pagination,
                                          RecordStorage<I> storage) {
        checkNotNull(orderBy);
        checkNotNull(filters);
        checkNotNull(pagination);
        checkNotNull(storage);

        Collection<EntityColumn> entityColumns = storage.entityColumns();
        EntityQuery<I> result = from(filters, orderBy, pagination, entityColumns);
        return result;
    }

    @VisibleForTesting
    static <I> EntityQuery<I> from(TargetFilters filters,
                                   OrderBy orderBy,
                                   Pagination pagination,
                                   Collection<EntityColumn> columns) {
        checkNotNull(orderBy);
        checkNotNull(filters);
        checkNotNull(pagination);
        checkNotNull(columns);

        QueryParameters queryParams = toQueryParams(orderBy, filters, pagination, columns);
        Collection<I> ids = toGenericIdValues(filters);

        EntityQuery<I> result = EntityQuery.of(ids, queryParams);
        return result;
    }

    private static QueryParameters toQueryParams(OrderBy orderBy,
                                                 TargetFilters filters,
                                                 Pagination pagination,
                                                 Collection<EntityColumn> entityColumns) {

        List<CompositeQueryParameter> parameters = getFiltersQueryParams(filters, entityColumns);
        return newQueryParameters(parameters, orderBy, pagination);
    }

    private static List<CompositeQueryParameter>
    getFiltersQueryParams(TargetFilters filters, Collection<EntityColumn> entityColumns) {
        return filters.getFilterList()
                      .stream()
                      .map(filter -> queryParameterFromFilter(filter, entityColumns))
                      .collect(toList());
    }

    private static QueryParameters newQueryParameters(List<CompositeQueryParameter> parameters,
                                                      OrderBy orderBy, Pagination pagination) {
        return QueryParameters.newBuilder()
                              .addAll(parameters)
                              .limit(pagination.getPageSize())
                              .orderBy(orderBy)
                              .build();
    }

    private static CompositeQueryParameter
    queryParameterFromFilter(CompositeFilter filter, Collection<EntityColumn> entityColumns) {
        Multimap<EntityColumn, Filter> filters = splitFilters(filter, entityColumns);
        CompositeOperator operator = filter.getOperator();
        return CompositeQueryParameter.from(filters, operator);
    }

    private static Multimap<EntityColumn, Filter>
    splitFilters(CompositeFilter filter, Collection<EntityColumn> entityColumns) {
        Multimap<EntityColumn, Filter> filters =
                create(filter.getFilterCount(), 1);
        for (Filter columnFilter : filter.getFilterList()) {
            EntityColumn column = findMatchingColumn(columnFilter, entityColumns);
            checkFilterType(column, columnFilter);
            filters.put(column, columnFilter);
        }
        return filters;
    }

    private static EntityColumn findMatchingColumn(Filter filter,
                                                   Collection<EntityColumn> entityColumns) {
        FieldPath fieldPath = filter.getFieldPath();
        checkArgument(fieldPath.getFieldNameCount() == 1,
                      "Incorrect Entity Column name in Entity Filter: %s",
                      join(".", fieldPath.getFieldNameList()));
        String columnName = fieldPath.getFieldName(0);
        for (EntityColumn column : entityColumns) {
            if (column.getName()
                      .equals(columnName)) {
                return column;
            }
        }

        throw new IllegalArgumentException(
                format("Could not find an EntityColumn description for column with name %s.",
                       columnName));
    }

    private static void checkFilterType(EntityColumn column, Filter filter) {
        Class<?> expectedType = column.getType();
        Any filterConvent = filter.getValue();
        Object filterValue = toObject(filterConvent, expectedType);
        Class<?> actualType = filterValue.getClass();
        checkArgument(wrap(expectedType).isAssignableFrom(wrap(actualType)),
                      "EntityColumn type mismatch. EntityColumn %s cannot have value %s.",
                      column,
                      filterValue);
    }

    private static <I> Collection<I> toGenericIdValues(TargetFilters filters) {
        IdFilter idFilter = filters.getIdFilter();
        Collection<I> ids = newArrayList();
        for (Any entityId : idFilter.getIdsList()) {
            @SuppressWarnings("unchecked" /* The caller is responsible to pass the proper IDs. */)
            I unpackedId = (I) Identifier.unpack(entityId);
            ids.add(unpackedId);
        }
        return ids;
    }
}
