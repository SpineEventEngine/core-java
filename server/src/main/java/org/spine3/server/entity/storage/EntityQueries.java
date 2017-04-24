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
import org.spine3.base.FieldFilter;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityIdFilter;
import org.spine3.server.entity.Entity;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov
 */
public final class EntityQueries {

    private static final String FIELD_PATH_SEPARATOR = ".";

    private EntityQueries() {
        // Prevent utility class initialization
    }

    public static EntityQuery from(EntityFilters entityFilters,
                                   Class<? extends Entity> entityClass) {
        checkNotNull(entityFilters);
        checkNotNull(entityClass);

        final Multimap<Column<?>, Object> queryParams = HashMultimap.create();
        final Iterable<FieldFilter> fieldFilters = entityFilters.getColumnFilterList();
        for (FieldFilter filter : fieldFilters) {
            final String fieldName = getFieldName(filter);
            final Column<?> column = Columns.metadata(entityClass, fieldName);
            final Function<Any, ?> typeTransformer = ProtoToJavaMapper.function(column.getType());
            final Collection<?> filterValues = Collections2.transform(filter.getValueList(),
                                                                           typeTransformer);
            queryParams.putAll(column, filterValues);
        }
        final EntityIdFilter idFilter = entityFilters.getIdFilter();
        final EntityQuery query = EntityQuery.of(idFilter, queryParams);
        return query;
    }

    private static String getFieldName(FieldFilter fieldFilter) {
        final String path = fieldFilter.getFieldPath();
        final int fieldNameStartIndex = path.lastIndexOf(FIELD_PATH_SEPARATOR) + 1;
        checkArgument(fieldNameStartIndex > 0 && fieldNameStartIndex < path.length(),
                      "Invalid field path %s.",
                      path);
        final String result = path.substring(fieldNameStartIndex);
        return result;
    }
}
