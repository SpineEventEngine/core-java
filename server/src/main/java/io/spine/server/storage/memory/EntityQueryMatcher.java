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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter.CompositeOperator;
import io.spine.server.entity.storage.CompositeQueryParameter;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.QueryParameters;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.server.storage.OperatorEvaluator.eval;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.stream.Collectors.toSet;

/**
 * A {@link Predicate} on the {@link EntityRecordWithColumns} matching it upon the given
 * {@link EntityQuery}.
 *
 * @param <I> the type of the IDs of the matched records
 * @author Dmytro Dashenkov
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
            Any packedId = record.getRecord()
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
            CompositeOperator operator = filter.getOperator();
            switch (operator) {
                case ALL:
                    match = checkAll(filter.getFilters(), record);
                    break;
                case EITHER:
                    match = checkEither(filter.getFilters(), record);
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

    private static boolean checkAll(Multimap<EntityColumn, ColumnFilter> filters,
                                    EntityRecordWithColumns record) {
        for (Map.Entry<EntityColumn, ColumnFilter> filter : filters.entries()) {
            Optional<MemoizedValue> columnValue = getColumnValue(record, filter.getKey());
            if (!columnValue.isPresent()) {
                return false;
            }
            boolean matches = checkSingleParameter(filter.getValue(), columnValue.get());
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkEither(Multimap<EntityColumn, ColumnFilter> filters,
                                       EntityRecordWithColumns record) {
        for (Map.Entry<EntityColumn, ColumnFilter> filter : filters.entries()) {
            Optional<MemoizedValue> columnValue = getColumnValue(record, filter.getKey());
            if (columnValue.isPresent()) {
                boolean matches = checkSingleParameter(filter.getValue(), columnValue.get());
                if (matches) {
                    return true;
                }
            }
        }
        return filters.isEmpty();
    }

    private static boolean checkSingleParameter(ColumnFilter filter,
                                                @Nullable MemoizedValue actualValue) {
        if (actualValue == null) {
            return false;
        }
        Object filterValue;
        Any wrappedValue = filter.getValue();
        EntityColumn sourceColumn = actualValue.getSourceColumn();
        Class<?> sourceClass = sourceColumn.getType();
        if (sourceClass != Any.class) {
            filterValue = toObject(wrappedValue, sourceClass);
        } else {
            filterValue = wrappedValue;
        }
        Object columnValue = sourceColumn.toPersistedValue(filterValue);
        boolean result = eval(actualValue.getValue(), filter.getOperator(), columnValue);
        return result;
    }

    private static Optional<MemoizedValue> getColumnValue(EntityRecordWithColumns record,
                                                          EntityColumn column) {
        String storedName = column.getStoredName();
        if (!record.getColumnNames()
                   .contains(storedName)) {
            return Optional.empty();
        }

        MemoizedValue value = record.getColumnValue(storedName);
        return Optional.of(value);
    }
}
