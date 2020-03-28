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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.CompositeQueryParameter;
import io.spine.server.entity.storage.QueryParameters;
import io.spine.server.storage.Column;
import io.spine.server.storage.MessageQuery;
import io.spine.server.storage.MessageWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.OperatorEvaluator.eval;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * @author Alex Tymchenko
 */
public class MessageQueryMatcher<I, R extends Message>
        implements Predicate<@Nullable MessageWithColumns<I, R>> {

    private final Set<I> acceptedIds;
    private final QueryParameters queryParams;

    MessageQueryMatcher(MessageQuery<I> query) {
        checkNotNull(query);
        // Pack IDs from the query for faster search using packed IDs from loaded records.
        Set<I> ids = query.getIds();
        this.acceptedIds = ids.isEmpty()
                           ? ImmutableSet.of()
                           : ImmutableSet.copyOf(ids);
        this.queryParams = query.getParameters();
    }

    @Override
    public boolean test(@Nullable MessageWithColumns<I, R> input) {
        if (input == null) {
            return false;
        }
        boolean result = idMatches(input) && columnValuesMatch(input);
        return result;
    }

    private boolean idMatches(MessageWithColumns<I, R> record) {
        if(acceptedIds.isEmpty()) {
            return true;
        }
        I actualId = record.id();
        return acceptedIds.contains(actualId);
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // Only valuable cases covered
    private boolean columnValuesMatch(MessageWithColumns<I, R> record) {
        boolean match;
        for (CompositeQueryParameter filter : queryParams) {
            CompositeFilter.CompositeOperator operator = filter.operator();
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

    private static <I, R extends Message> boolean
    checkAll(Multimap<Column, Filter> filters, MessageWithColumns<I, R> record) {
        if (filters.isEmpty()) {
            return true;
        }
        boolean result =
                filters.entries()
                       .stream()
                       .allMatch(filter -> matches(record, filter));
        return result;
    }

    private static <I, R extends Message> boolean
    checkEither(Multimap<Column, Filter> filters, MessageWithColumns<I, R> record) {
        if (filters.isEmpty()) {
            return true;
        }
        boolean result =
                filters.entries()
                       .stream()
                       .anyMatch(filter -> matches(record, filter));
        return result;
    }

    private static <I, R extends Message> boolean
    matches(MessageWithColumns<I, R> record, Map.Entry<Column, Filter> filter) {
        if (!hasColumn(record, filter)) {
            return false;
        }
        Column column = filter.getKey();
        @Nullable Object columnValue = columnValue(record, column);
        boolean result = checkSingleParameter(filter.getValue(), columnValue, column);
        return result;
    }

    private static <I, R extends Message> boolean
    hasColumn(MessageWithColumns<I, R> record, Map.Entry<Column, Filter> filter) {
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

    private static <I, R extends Message> @Nullable Object
    columnValue(MessageWithColumns<I, R> record, Column column) {
        ColumnName columnName = column.name();
        Object value = record.columnValue(columnName);
        return value;
    }

}
