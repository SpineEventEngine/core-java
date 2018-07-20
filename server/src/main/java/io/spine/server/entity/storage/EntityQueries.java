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
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.CompositeColumnFilter.CompositeOperator;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.server.storage.RecordStorage;

import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.HashMultimap.create;
import static com.google.common.primitives.Primitives.wrap;
import static io.spine.protobuf.TypeConverter.toObject;
import static java.lang.String.format;

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
     * Creates a new instance of {@link EntityQuery} from the given {@link EntityFilters} and
     * for the given {@link RecordStorage}.
     *
     * @param  entityFilters the filters for the Entities specifying the query predicate
     * @param  storage the storage for which the query is created
     * @return new instance of the {@code EntityQuery} with the specified attributes
     */
    public static <I> EntityQuery<I> from(EntityFilters entityFilters,
                                          RecordStorage<I> storage) {
        checkNotNull(entityFilters);
        checkNotNull(storage);

        Collection<EntityColumn> entityColumns = storage.entityColumns();
        EntityQuery<I> result = from(entityFilters, entityColumns);
        return result;
    }

    @VisibleForTesting
    static <I> EntityQuery<I> from(EntityFilters entityFilters,
                                   Collection<EntityColumn> entityColumns) {
        checkNotNull(entityFilters);
        checkNotNull(entityColumns);

        QueryParameters queryParams = toQueryParams(entityFilters, entityColumns);
        Collection<I> ids = toGenericIdValues(entityFilters);

        EntityQuery<I> result = EntityQuery.of(ids, queryParams);
        return result;
    }

    private static QueryParameters toQueryParams(EntityFilters entityFilters,
                                                 Collection<EntityColumn> entityColumns) {
        QueryParameters.Builder builder = QueryParameters.newBuilder();

        for (CompositeColumnFilter filter : entityFilters.getFilterList()) {
            Multimap<EntityColumn, ColumnFilter> columnFilters =
                    splitFilters(filter, entityColumns);
            CompositeOperator operator = filter.getOperator();
            CompositeQueryParameter parameter =
                    CompositeQueryParameter.from(columnFilters, operator);
            builder.add(parameter);
        }
        return builder.build();
    }

    private static Multimap<EntityColumn, ColumnFilter> splitFilters(CompositeColumnFilter filter,
                                                                     Collection<EntityColumn> entityColumns) {
        Multimap<EntityColumn, ColumnFilter> columnFilters =
                create(filter.getFilterCount(), 1);
        for (ColumnFilter columnFilter : filter.getFilterList()) {
            EntityColumn column = findMatchingColumn(columnFilter, entityColumns);
            checkFilterType(column, columnFilter);
            columnFilters.put(column, columnFilter);
        }
        return columnFilters;
    }

    private static EntityColumn findMatchingColumn(ColumnFilter filter,
                                                   Collection<EntityColumn> entityColumns) {
        for (EntityColumn column : entityColumns) {
            if (column.getName().equals(filter.getColumnName())) {
                return column;
            }
        }

        throw new IllegalArgumentException(
                format("Could not find an EntityColumn description for column with name %s.",
                        filter.getColumnName()));
    }

    private static void checkFilterType(EntityColumn column, ColumnFilter filter) {
        Class<?> expectedType = column.getType();
        Any filterConvent = filter.getValue();
        Object filterValue = toObject(filterConvent, expectedType);
        Class<?> actualType = filterValue.getClass();
        checkArgument(wrap(expectedType).isAssignableFrom(wrap(actualType)),
                      "EntityColumn type mismatch. EntityColumn %s cannot have value %s.",
                      column,
                      filterValue);
    }

    private static <I> Collection<I> toGenericIdValues(EntityFilters entityFilters) {
        EntityIdFilter idFilter = entityFilters.getIdFilter();
        Collection<I> ids = new LinkedList<>();
        for (EntityId entityId : idFilter.getIdsList()) {
            Any wrappedMessageId = entityId.getId();
            I genericId = Identifier.unpack(wrappedMessageId);
            ids.add(genericId);
        }
        return ids;
    }
}
