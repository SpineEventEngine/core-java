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
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import org.spine3.client.EntityId;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;

/**
 * @author Dmytro Dashenkov
 */
public final class EntityQueryMatcher implements Predicate<EntityRecordWithColumns> {

    private static final Function<EntityId, Any> ENTITY_ID_UNWRAPPER =
            new Function<EntityId, Any>() {
                @Override
                public Any apply(@Nullable EntityId input) {
                    checkNotNull(input);
                    return input.getId();
                }
            };

    private final Collection<Any> acceptedIds;
    private final Multimap<Column<?>, Object> queryParams;

    public EntityQueryMatcher(EntityQuery query) {
        checkNotNull(query);
        final Collection<EntityId> entityIds = query.getIdFilter()
                                                    .getIdsList();
        this.acceptedIds = newHashSet(transform(entityIds, ENTITY_ID_UNWRAPPER));
        this.queryParams = query.getParameters();
    }

    @Override
    public boolean apply(@Nullable EntityRecordWithColumns input) {
        if (input == null) {
            return false;
        }

        if (!acceptedIds.isEmpty()) {
            final Any entityId = input.getRecord()
                                      .getEntityId();
            final boolean idMatches = acceptedIds.contains(entityId);
            if (!idMatches) {
                return false;
            }
        }

        final Map<String, Column.MemoizedValue<?>> entityColumns = input.getColumnValues();
        final Map<Column<?>, Collection<Object>> paramsMap = queryParams.asMap();

        if (!paramsMap.isEmpty()) {
            for (Map.Entry<Column<?>, Collection<Object>> param : paramsMap.entrySet()) {
                final Column<?> column = param.getKey();
                final String columnName = column.getName();

                final Collection<Object> possibleValues = param.getValue();
                final Column.MemoizedValue<?> actualValueWithMetadata = entityColumns.get(columnName);
                final Object actualValue = actualValueWithMetadata != null
                        ? actualValueWithMetadata.getValue()
                        : null;
                final boolean matches = possibleValues.contains(actualValue);
                if (!matches) {
                    return false;
                }
            }
        }

        return true;
    }
}
