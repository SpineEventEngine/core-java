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

package io.spine.server.event.store;

import com.google.protobuf.Timestamp;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;

import static com.google.common.base.Preconditions.checkNotNull;
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

        RecordQueryBuilder<EventId, Event> builder =
                RecordQuery.newBuilder(EventId.class, Event.class);
        if (!query.includeAll()) {
            addTimeBounds(query, builder);
            addTypeParams(query, builder);
        }
        RecordQuery<EventId, Event> result = addOrderAndLimit(query, builder);
        return result;
    }

    private static void addTimeBounds(EventStreamQuery query,
                                      RecordQueryBuilder<EventId, Event> builder) {
        if (query.hasAfter()) {
            Timestamp timestamp = query.getAfter();
            builder.where(created)
                   .isGreaterThan(timestamp);
        }
        if (query.hasBefore()) {
            Timestamp timestamp = query.getBefore();
            builder.where(created)
                   .isLessThan(timestamp);
        }
    }

    private static void addTypeParams(EventStreamQuery query,
                                      RecordQueryBuilder<EventId, Event> builder) {
        if(query.getFilterCount() == 0) {
            return;
        }
        builder.either((b) -> {
            for (EventFilter eventFilter : query.getFilterList()) {
                String type = eventFilter.getEventType()
                                         .trim();
                if (!type.isEmpty()) {
                    b.where(EventColumn.type)
                     .is(type);
                }
            }
            return b;
        });
    }

    private static RecordQuery<EventId, Event>
    addOrderAndLimit(EventStreamQuery query, RecordQueryBuilder<EventId, Event> builder) {
        builder.sortAscendingBy(created);
        if (query.hasLimit()) {
            builder.limit(query.getLimit()
                               .getValue());
        }

        return builder.build();
    }
}
