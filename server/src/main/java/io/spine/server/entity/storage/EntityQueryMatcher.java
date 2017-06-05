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

import com.google.common.base.Predicate;
import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private final Map<Column<?>, Object> queryParams;

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
            final Object genericId = Identifier.idFromAny(entityId);
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
        if (!queryParams.isEmpty()) {
            for (Map.Entry<Column<?>, Object> param : queryParams.entrySet()) {
                final Column<?> column = param.getKey();
                final String columnName = column.getName();

                final Object requiredValue = param.getValue();
                final Column.MemoizedValue<?> actualValueWithMetadata =
                        entityColumns.get(columnName);
                if (actualValueWithMetadata == null) {
                    return false;
                }
                final Object actualValue = actualValueWithMetadata.getValue();
                final boolean matches = Objects.equals(requiredValue, actualValue);
                if (!matches) {
                    return false;
                }
            }
        }
        return true;
    }
}
