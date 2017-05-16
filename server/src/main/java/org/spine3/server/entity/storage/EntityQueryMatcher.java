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

import com.google.common.base.Predicate;
import com.google.protobuf.Any;
import org.spine3.annotation.Internal;
import org.spine3.base.Identifiers;
import org.spine3.client.QueryOperator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.client.QueryOperator.compare;

/**
 * A {@link Predicate} on the {@link EntityRecordWithColumns} matching it upon the given
 * {@link EntityQuery}.
 *
 * @param <I> the type of the IDs of the matched records
 * @author Dmytro Dashenkov
 * @see EntityQuery for the matching contract
 */
@Internal
public final class EntityQueryMatcher<I> implements Predicate<EntityRecordWithColumns> {

    private final Collection<I> acceptedIds;
    private final QueryParameters queryParams;

    public EntityQueryMatcher(EntityQuery<I> query) {
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
            final Object genericId = Identifiers.idFromAny(entityId);
            @SuppressWarnings("SuspiciousMethodCalls")
            // The Collection.contains behavior about the non-assignable types is acceptable
            final boolean idMatches = acceptedIds.contains(genericId);
            if (!idMatches) {
                return false;
            }
        }
        return true;
    }

    private boolean columnValuesMatch(EntityRecordWithColumns record) {
        final Map<String, Column.MemoizedValue<?>> entityColumns = record.getColumnValues();
        final ColumnValueMatcher columnValueMatcher = new ColumnValueMatcher(entityColumns);
        queryParams.forEach(columnValueMatcher);
        return columnValueMatcher.matches();
    }

    /**
     * A stateful parameter processor matching the given Entity Columns to a number of
     * {@linkplain QueryParameters Query parameters}.
     */
    private static class ColumnValueMatcher implements QueryParameters.ParameterConsumer {

        private final Map<String, Column.MemoizedValue<?>> entityColumns;
        private boolean currentlyMatches = true;

        private ColumnValueMatcher(Map<String, Column.MemoizedValue<?>> entityColumns) {
            this.entityColumns = entityColumns;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Each next call to this method will execute differently depending on the previous
         * calls. If at least one of the previous calls upon a single instance has failed to match
         * the Entity Columns to a given parameter, then all the subsequent calls will exit at
         * the beginning and won't try to match the remaining parameters.
         */
        @Override
        public void consume(QueryOperator operator, Column<?> column, @Nullable Object value) {
            if (!currentlyMatches) {
                return;
            }
            final String columnName = column.getName();
            final Column.MemoizedValue<?> actualValue = entityColumns.get(columnName);
            currentlyMatches = actualValue != null
                               && compare(value, operator, actualValue.getValue());
        }

        /**
         * Shows if the given Entity Columns match all the consumed parameters.
         *
         * <p>The default value is {@link true}.
         *
         * <p>Once {@link #consume} has been called at least once, the value may change to
         * {@code false}. Once changed, it can never become {@code true} again.
         *
         * @return {@code true} if the given Entity Columns match all the passed Query parameters,
         * {@code false} otherwise.
         */
        public boolean matches() {
            return currentlyMatches;
        }
    }
}
