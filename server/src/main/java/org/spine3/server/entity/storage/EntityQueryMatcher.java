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
import com.google.common.collect.Multimap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov
 */
public final class EntityQueryMatcher implements Predicate<EntityRecordWithColumns> {

    private final EntityQuery entityQuery;

    public EntityQueryMatcher(EntityQuery query) {
        this.entityQuery = checkNotNull(query);
    }

    @Override
    public boolean apply(@Nullable EntityRecordWithColumns input) {
        if (input == null) {
            return false;
        }

        final Map<String, Column.MemoizedValue<?>> entityColumns = input.getColumnValues();

        final Multimap<Column<?>, Object> params = entityQuery.getParameters();
        final Map<Column<?>, Collection<Object>> paramsMap = params.asMap();

        for (Map.Entry<Column<?>, Collection<Object>> param : paramsMap.entrySet()) {
            final Column<?> column = param.getKey();
            final String columnName = column.getName();

            final Collection<Object> possibleValues = param.getValue();
            final Column.MemoizedValue<?> actualValueWithMetadata = entityColumns.get(columnName);
            final Object actualValue = actualValueWithMetadata.getValue();

            final boolean matches = possibleValues.contains(actualValue);
            if (!matches) {
                return false;
            }
        }

        return true;
    }
}
