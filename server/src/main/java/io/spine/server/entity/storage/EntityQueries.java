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

package io.spine.server.entity.storage;

import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.Identifier;
import io.spine.annotation.Internal;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.CompositeColumnFilter.CompositeOperator;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.server.entity.Entity;

import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.HashMultimap.create;
import static com.google.common.primitives.Primitives.wrap;
import static io.spine.protobuf.TypeConverter.toObject;

/**
 * A utility class for working with {@link EntityQuery} instances.
 *
 * @author Dmytro Dashenkov
 * @see EntityQuery
 */
@Internal
public final class EntityQueries {

    private EntityQueries() {
        // Prevent utility class initialization
    }

    /**
     * Creates a new instance of {@link EntityQuery} from the given {@link EntityFilters} targeting
     * the given Entity class.
     *
     * @param entityFilters the filters for the Entities specifying the query predicate
     * @param entityClass   the Entity class specifying the query target
     * @return new instance of the {@code EntityQuery} with the specified attributes
     */
    public static <I> EntityQuery<I> from(EntityFilters entityFilters,
                                          Class<? extends Entity> entityClass) {
        checkNotNull(entityFilters);
        checkNotNull(entityClass);

        final QueryParameters queryParams = toQueryParams(entityFilters, entityClass);
        final Collection<I> ids = toGenericIdValues(entityFilters);

        final EntityQuery<I> result = EntityQuery.of(ids, queryParams);
        return result;
    }

    private static QueryParameters toQueryParams(EntityFilters entityFilters,
                                                 Class<? extends Entity> entityClass) {
        final QueryParameters.Builder builder = QueryParameters.newBuilder();

        for (CompositeColumnFilter filter : entityFilters.getFilterList()) {
            final Multimap<EntityColumn, ColumnFilter> columnFilters = splitFilters(filter,
                                                                                    entityClass);
            final CompositeOperator operator = filter.getOperator();
            final CompositeQueryParameter parameter =
                    CompositeQueryParameter.from(columnFilters, operator);
            builder.add(parameter);
        }
        return builder.build();
    }

    private static Multimap<EntityColumn, ColumnFilter> splitFilters(
            CompositeColumnFilter filter,
            Class<? extends Entity> entityClass) {
        final Multimap<EntityColumn, ColumnFilter> columnFilters = create(filter.getFilterCount(),
                                                                          1);
        for (ColumnFilter columnFilter : filter.getFilterList()) {
            final EntityColumn column = Columns.findColumn(entityClass, columnFilter.getColumnName());
            checkFilterType(column, columnFilter);
            columnFilters.put(column, columnFilter);
        }
        return columnFilters;
    }

    private static void checkFilterType(EntityColumn column, ColumnFilter filter) {
        final Class<?> expectedType = column.getType();
        final Any filterConvent = filter.getValue();
        final Object filterValue = toObject(filterConvent, expectedType);
        final Class<?> actualType = filterValue.getClass();
        checkArgument(wrap(expectedType).isAssignableFrom(wrap(actualType)),
                      "EntityColumn type mismatch. EntityColumn %s cannot have value %s.",
                      column,
                      filterValue);
    }

    private static <I> Collection<I> toGenericIdValues(EntityFilters entityFilters) {
        final EntityIdFilter idFilter = entityFilters.getIdFilter();
        final Collection<I> ids = new LinkedList<>();
        for (EntityId entityId : idFilter.getIdsList()) {
            final Any wrappedMessageId = entityId.getId();
            final I genericId = Identifier.unpack(wrappedMessageId);
            ids.add(genericId);
        }
        return ids;
    }
}
