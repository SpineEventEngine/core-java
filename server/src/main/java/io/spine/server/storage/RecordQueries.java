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

package io.spine.server.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.FieldPath;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.TargetFilters;
import io.spine.server.entity.storage.ColumnName;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.HashMultimap.create;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.primitives.Primitives.wrap;
import static io.spine.protobuf.TypeConverter.toObject;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

/**
 * A set of the factory methods for the creation of {@link RecordQuery} instances.
 */
@Internal
public final class RecordQueries {

    private static final QueryParameters EMPTY_PARAMS = QueryParameters.newBuilder()
                                                                       .build();

    /**
     * Prevents this utility class from instantiation.
     */
    private RecordQueries() {
    }

    /**
     * Creates a new {@code RecordQuery} which targets all the records of the storage.
     *
     * @param <I>
     *         the type of the record identifiers
     */
    public static <I> RecordQuery<I> all() {
        return new RecordQuery<>(ImmutableSet.of(), EMPTY_PARAMS);
    }

    /**
     * Creates a new {@code RecordQuery} targeting the records with the given identifiers.
     *
     * @param ids
     *         the identifiers of the records to query
     * @param <I>
     *         the type of the record identifiers
     */
    public static <I> RecordQuery<I> of(Iterable<I> ids) {
        checkNotNull(ids);
        return new RecordQuery<>(ids, EMPTY_PARAMS);
    }

    /**
     * Creates a new {@code RecordQuery} targeting the records with the given identifiers which
     * satisfy the specified query parameters.
     *
     * @param ids
     *         the identifiers of the records to query
     * @param parameters
     *         criteria applied to the records
     * @param <I>
     *         the type of the record identifiers
     */
    public static <I> RecordQuery<I> of(Iterable<I> ids, QueryParameters parameters) {
        checkNotNull(ids);
        checkNotNull(parameters);
        return new RecordQuery<>(ids, parameters);
    }

    /**
     * Creates a new {@code RecordQuery} targeting the records which match the specified
     * query parameters.
     *
     * @param parameters
     *         criteria applied to the records
     * @param <I>
     *         the type of the record identifiers
     */
    public static <I> RecordQuery<I> of(QueryParameters parameters) {
        checkNotNull(parameters);
        return new RecordQuery<>(ImmutableSet.of(), parameters);
    }

    /**
     * Creates a new {@code RecordQuery} targeting the records which specific column matches the
     * passed column value.
     *
     * @param column
     *         the column of the record to query
     * @param value
     *         the expected value to which the actual column value of the record should match
     * @param <I>
     *         the type of the record identifiers
     * @param <V>
     *         the type of the values stored in the column
     */
    public static <I, V> RecordQuery<I> byColumn(RecordColumn<?, ?> column, V value) {
        checkNotNull(column);
        checkNotNull(value);
        QueryParameters queryParams = QueryParameters.eq(column, value);
        return of(queryParams);
    }

    /**
     * Creates a new {@code RecordQuery} targeting the records which queryable field matches the
     * passed field value.
     *
     * @param field
     *         the field of the record to query
     * @param value
     *         the expected value to which the actual field value of the record should match
     * @param <I>
     *         the type of the record identifiers
     * @param <V>
     *         the type of the field
     */
    public static <I, V> RecordQuery<I> byField(QueryableField<?> field, V value) {
        checkNotNull(field);
        checkNotNull(value);
        QueryParameters queryParams = QueryParameters.eq(field.column(), value);
        return of(queryParams);
    }

    /**
     * Creates a new {@code RecordQuery} targeting the records which satisfy
     * the passed {@link TargetFilters} and have the passed column definitions.
     *
     * @param filters
     *         the filters applied to the records
     * @param recordSpec
     *         the definitions of columns stored along with each record in the storage
     * @param <I>
     *         the type of the record identifiers
     */
    public static <I> RecordQuery<I> from(TargetFilters filters, RecordSpec<?> recordSpec) {
        checkNotNull(filters);
        checkNotNull(recordSpec);

        QueryParameters queryParams = toQueryParams(filters, recordSpec);
        List<I> ids = toIdentifiers(filters);

        RecordQuery<I> result = of(ids, queryParams);
        return result;
    }

    private static QueryParameters toQueryParams(TargetFilters filters, RecordSpec<?> recordSpec) {
        List<CompositeQueryParameter> parameters = getFiltersQueryParams(filters, recordSpec);
        return newQueryParameters(parameters);
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

    private static List<CompositeQueryParameter>
    getFiltersQueryParams(TargetFilters filters, RecordSpec<?> recordSpec) {
        return filters.getFilterList()
                      .stream()
                      .map(filter -> queryParameterFromFilter(filter, recordSpec))
                      .collect(toList());
    }

    private static QueryParameters newQueryParameters(List<CompositeQueryParameter> parameters) {
        return QueryParameters.newBuilder()
                              .addAll(parameters)
                              .build();
    }

    private static CompositeQueryParameter
    queryParameterFromFilter(CompositeFilter filter, RecordSpec<?> recordSpec) {
        Multimap<Column, Filter> filters = splitFilters(filter, recordSpec);
        CompositeFilter.CompositeOperator operator = filter.getOperator();
        return CompositeQueryParameter.from(filters, operator);
    }

    private static Multimap<Column, Filter>
    splitFilters(CompositeFilter filter, RecordSpec<?> recordSpec) {
        Multimap<Column, Filter> filters = create(filter.getFilterCount(), 1);
        for (Filter columnFilter : filter.getFilterList()) {
            Column column = findMatchingColumn(columnFilter, recordSpec);
            checkFilterType(column, columnFilter);
            filters.put(column, columnFilter);
        }
        return filters;
    }

    private static Column findMatchingColumn(Filter filter, RecordSpec<?> recordSpec) {
        FieldPath fieldPath = filter.getFieldPath();
        checkArgument(fieldPath.getFieldNameCount() == 1,
                      "Incorrect column name in the passed `Filter`: %s",
                      join(".", fieldPath.getFieldNameList()));
        String column = fieldPath.getFieldName(0);
        ColumnName columnName = ColumnName.of(column);
        return recordSpec.get(columnName);
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
}
