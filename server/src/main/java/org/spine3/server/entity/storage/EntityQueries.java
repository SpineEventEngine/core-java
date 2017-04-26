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

package org.spine3.server.entity.storage;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import org.spine3.annotations.Internal;
import org.spine3.base.FieldFilter;
import org.spine3.base.Identifiers;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.protobuf.ProtoToJavaMapper;
import org.spine3.server.entity.Entity;

import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;

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
    public static EntityQuery from(EntityFilters entityFilters,
                                   Class<? extends Entity> entityClass) {
        checkNotNull(entityFilters);
        checkNotNull(entityClass);

        final Multimap<Column<?>, Object> queryParams = toQueryParams(entityFilters, entityClass);
        final Collection<Object> ids = toGenericIdValues(entityFilters);

        final EntityQuery result = EntityQuery.of(ids, queryParams);
        return result;
    }

    private static Multimap<Column<?>, Object> toQueryParams(EntityFilters entityFilters,
                                                             Class<? extends Entity> entityClass) {
        final Multimap<Column<?>, Object> queryParams = HashMultimap.create();
        final Iterable<FieldFilter> fieldFilters = entityFilters.getColumnFilterList();
        for (FieldFilter filter : fieldFilters) {
            final String fieldName = filter.getFieldPath();
            final Column<?> column = Columns.metadata(entityClass, fieldName);
            final Function<Any, ?> typeTransformer = ProtoToJavaMapper.function(column.getType());
            final Collection<?> filterValues = Collections2.transform(filter.getValueList(),
                                                                      typeTransformer);
            queryParams.putAll(column, filterValues);
        }
        return queryParams;
    }

    private static Collection<Object> toGenericIdValues(EntityFilters entityFilters) {
        final EntityIdFilter idFilter = entityFilters.getIdFilter();
        final Collection<Object> ids = new LinkedList<>();
        for (EntityId entityId : idFilter.getIdsList()) {
            final Any wrappedMessageId = entityId.getId();
            final Object genericId = Identifiers.idFromAny(wrappedMessageId);
            ids.add(genericId);
        }
        return ids;
    }
}
