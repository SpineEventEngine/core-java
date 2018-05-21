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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.server.storage.OperatorEvaluator.eval;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A {@link Predicate} on the {@link EntityRecordWithColumns} matching it upon the given
 * {@link EntityQuery}.
 *
 * @param <I> the type of the IDs of the matched records
 * @author Dmytro Dashenkov
 * @see EntityQuery for the matching contract
 */
final class EntityQueryMatcher<I> implements Predicate<EntityRecordWithColumns> {

    private final Collection<I> acceptedIds;
    private final QueryParameters queryParams;

    EntityQueryMatcher(EntityQuery<I> query) {
        checkNotNull(query);
        this.acceptedIds = query.getIds();
        this.queryParams = query.getParameters();
    }

    @Override
    public boolean apply(@Nullable EntityRecordWithColumns input) {
        if (input == null) {
            return false;
        }
        final boolean result = idMatches(input) && columnValuesMatch(input);
        return result;
    }

    private boolean idMatches(EntityRecordWithColumns record) {
        if (!acceptedIds.isEmpty()) {
            final Any entityId = record.getRecord()
                                       .getEntityId();
            final I genericId = Identifier.unpack(entityId);
            final boolean idMatches = acceptedIds.contains(genericId);
            if (!idMatches) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // Only valuable cases covered
    private boolean columnValuesMatch(EntityRecordWithColumns record) {
        boolean match;
        for (CompositeQueryParameter filter : queryParams) {
            final CompositeOperator operator = filter.getOperator();
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
            final Optional<MemoizedValue> columnValue = getColumnValue(record, filter.getKey());
            if (!columnValue.isPresent()) {
                return false;
            }
            final boolean matches = checkSingleParameter(filter.getValue(), columnValue.get());
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkEither(Multimap<EntityColumn, ColumnFilter> filters,
                                       EntityRecordWithColumns record) {
        for (Map.Entry<EntityColumn, ColumnFilter> filter : filters.entries()) {
            final Optional<MemoizedValue> columnValue = getColumnValue(record, filter.getKey());
            if (columnValue.isPresent()) {
                final boolean matches = checkSingleParameter(filter.getValue(), columnValue.get());
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
        final Object value;
        final Any wrappedValue = filter.getValue();
        final Class<?> sourceClass = actualValue.getSourceColumn()
                                                .getType();
        if (sourceClass != Any.class) {
            value = toObject(wrappedValue, sourceClass);
        } else {
            value = wrappedValue;
        }
        final boolean result = eval(actualValue.getValue(), filter.getOperator(), value);
        return result;
    }

    private static Optional<MemoizedValue> getColumnValue(EntityRecordWithColumns record,
                                                          EntityColumn column) {
        final String storedName = column.getStoredName();
        if (!record.getColumnNames()
                   .contains(storedName)) {
            return Optional.absent();
        }

        final MemoizedValue value = record.getColumnValue(storedName);
        return Optional.of(value);
    }
}
