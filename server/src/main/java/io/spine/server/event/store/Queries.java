/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.event.store;

import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.query.Either;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.server.event.store.EventColumn.created;

/**
 * Converts {@link EventStreamQuery} to {@link RecordQuery}.
 */
final class Queries {

    private Queries() {
    }

    /**
     * Converts the {@code EventStreamQuery} to the record query.
     */
    static RecordQuery<EventId, Event> convert(EventStreamQuery query) {
        checkNotNull(query);

        var builder = RecordQuery.newBuilder(EventId.class, Event.class);
        if (!query.includeAll()) {
            addTimeBounds(query, builder);
            addTypeParams(query, builder);
        }
        var result = addOrderAndLimit(query, builder);
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")  /* Configuring `builder` step-by-step. */
    private static void addTimeBounds(EventStreamQuery query,
                                      RecordQueryBuilder<EventId, Event> builder) {
        if (query.hasAfter()) {
            var timestamp = query.getAfter();
            builder.where(created).isGreaterThan(timestamp);
        }
        if (query.hasBefore()) {
            var timestamp = query.getBefore();
            builder.where(created).isLessThan(timestamp);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")  /* Configuring `builder` step-by-step. */
    private static void addTypeParams(EventStreamQuery query,
                                      RecordQueryBuilder<EventId, Event> builder) {
        if (query.getFilterCount() == 0) {
            return;
        }
        var filters = query.getFilterList();
        var eitherStatements = filters.stream()
                .filter(Queries::hasEventType)
                .map(Queries::toEither)
                .collect(toImmutableList());
        builder.either(eitherStatements);
    }

    private static boolean hasEventType(EventFilter f) {
        return !f.getEventType().trim().isEmpty();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")  /* Configuring `builder` step-by-step. */
    private static Either<RecordQueryBuilder<EventId, Event>> toEither(EventFilter filter) {
        return builder -> {
            var type = filter.getEventType().trim();
            builder.where(EventColumn.type).is(type);
            return builder;
        };
    }

    private static RecordQuery<EventId, Event>
    addOrderAndLimit(EventStreamQuery query, RecordQueryBuilder<EventId, Event> builder) {
        builder.sortAscendingBy(created);
        if (query.hasLimit()) {
            builder.limit(query.getLimit().getValue());
        }

        return builder.build();
    }
}
