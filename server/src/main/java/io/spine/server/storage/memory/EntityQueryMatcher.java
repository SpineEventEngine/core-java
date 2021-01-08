/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.client.Filter;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.CompositeQueryParameter;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.QueryParameters;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.OperatorEvaluator.eval;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.stream.Collectors.toSet;

/**
 * A {@link Predicate} on the {@link EntityRecordWithColumns} matching it upon the given
 * {@link EntityQuery}.
 *
 * @param <I>
 *         the type of the IDs of the matched records
 * @see EntityQuery for the matching contract
 */
final class EntityQueryMatcher<I> implements Predicate<@Nullable EntityRecordWithColumns> {

    private final Set<Any> acceptedIds;
    private final QueryParameters queryParams;

    EntityQueryMatcher(EntityQuery<I> query) {
        checkNotNull(query);
        // Pack IDs from the query for faster search using packed IDs from loaded records.
        Set<I> ids = query.getIds();
        this.acceptedIds = ids.isEmpty()
                           ? ImmutableSet.of()
                           : ids.stream()
                                .map(Identifier::pack)
                                .collect(toSet());
        this.queryParams = query.getParameters();
    }

    @Override
    public boolean test(@Nullable EntityRecordWithColumns input) {
        if (input == null) {
            return false;
        }
        boolean result = idMatches(input) && columnValuesMatch(input);
        return result;
    }

    private boolean idMatches(EntityRecordWithColumns record) {
        if (!acceptedIds.isEmpty()) {
            Any packedId = record.record()
                                 .getEntityId();
            boolean idMatches = acceptedIds.contains(packedId);
            return idMatches;
        }
        return true;
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // Only valuable cases covered
    private boolean columnValuesMatch(EntityRecordWithColumns record) {
        boolean match;
        for (CompositeQueryParameter filter : queryParams) {
            CompositeOperator operator = filter.operator();
            switch (operator) {
                case ALL:
                    match = checkAll(filter.filters(), record);
                    break;
                case EITHER:
                    match = checkEither(filter.filters(), record);
                    break;
                default:
                    throw newIllegalArgumentException("Composite operator %s is invalid.",
                                                      operator);
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkAll(Multimap<Column, Filter> filters,
                                    EntityRecordWithColumns record) {
        if (filters.isEmpty()) {
            return true;
        }
        boolean result =
                filters.entries()
                       .stream()
                       .allMatch(filter -> matches(record, filter));
        return result;
    }

    private static boolean checkEither(Multimap<Column, Filter> filters,
                                       EntityRecordWithColumns record) {
        if (filters.isEmpty()) {
            return true;
        }
        boolean result =
                filters.entries()
                       .stream()
                       .anyMatch(filter -> matches(record, filter));
        return result;
    }

    private static boolean matches(EntityRecordWithColumns record,
                                   Map.Entry<Column, Filter> filter) {
        if (!hasColumn(record, filter)) {
            return false;
        }
        Column column = filter.getKey();
        @Nullable Object columnValue = columnValue(record, column);
        boolean result = checkSingleParameter(filter.getValue(), columnValue, column);
        return result;
    }

    private static boolean hasColumn(EntityRecordWithColumns record,
                                     Map.Entry<Column, Filter> filter) {
        boolean result = record.hasColumn(filter.getKey()
                                                .name());
        return result;
    }

    private static boolean checkSingleParameter(Filter filter,
                                                @Nullable Object actualValue,
                                                Column column) {
        if (actualValue == null) {
            return false;
        }
        Object filterValue;
        Any wrappedValue = filter.getValue();
        Class<?> sourceClass = column.type();
        if (sourceClass != Any.class) {
            filterValue = toObject(wrappedValue, sourceClass);
        } else {
            filterValue = wrappedValue;
        }
        boolean result = eval(actualValue, filter.getOperator(), filterValue);
        return result;
    }

    private static @Nullable Object columnValue(EntityRecordWithColumns record,
                                                Column column) {
        ColumnName columnName = column.name();
        Object value = record.columnValue(columnName);
        return value;
    }
}
