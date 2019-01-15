/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;

import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.gt;
import static io.spine.client.ColumnFilters.lt;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;

/**
 * Converts {@link EventStreamQuery} to {@link EntityFilters}.
 *
 * <p>The resulting filters contain the filtering by {@code before} and {@code after} fields
 * of the source query and by the {@code eventType} field of the underlying
 * {@linkplain EventFilter EventFilters}.
 */
final class QueryToFilters {

    private final EventStreamQuery query;
    private final EntityFilters.Builder builder;

    /**
     * Creates an instance of {@link EntityFilters} from the given {@link EventStreamQuery}.
     *
     * <p>The resulting filters contain the filtering by {@code before} and {@code after} fields
     * of the source query and by the {@code eventType} field of the underlying
     * {@linkplain EventFilter EventFilters}.
     *
     * @param query
     *         the source {@link EventStreamQuery} to get the info from
     * @return new instance of {@link EntityFilters} filtering the events
     */
    static EntityFilters convert(EventStreamQuery query) {
        EntityFilters result = new QueryToFilters(query).convert();
        return result;
    }

    private QueryToFilters(EventStreamQuery query) {
        this.query = query;
        this.builder = EntityFilters.newBuilder();
    }

    private EntityFilters convert() {
        addTimeFilter();
        addTypeFilter();
        return builder.build();
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private void addTimeFilter() {
        CompositeColumnFilter.Builder timeFilter = CompositeColumnFilter
                .newBuilder()
                .setOperator(ALL);
        String createdColumn = ColumnName.created.name();
        if (query.hasAfter()) {
            Timestamp timestamp = query.getAfter();
            ColumnFilter filter = gt(createdColumn, timestamp);
            timeFilter.addFilter(filter);
        }
        if (query.hasBefore()) {
            Timestamp timestamp = query.getBefore();
            ColumnFilter filter = lt(createdColumn, timestamp);
            timeFilter.addFilter(filter);
        }
        add(timeFilter);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private void addTypeFilter() {
        CompositeColumnFilter.Builder typeFilter = CompositeColumnFilter
                .newBuilder()
                .setOperator(EITHER);
        String typeColumn = ColumnName.type.name();
        for (EventFilter eventFilter : query.getFilterList()) {
            String type = eventFilter.getEventType()
                                     .trim();
            if (!type.isEmpty()) {
                ColumnFilter filter = eq(typeColumn, type);
                typeFilter.addFilter(filter);
            }
        }
        add(typeFilter);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private void add(CompositeColumnFilter.Builder filter) {
        boolean filterIsEmpty = filter.getFilterList()
                                      .isEmpty();
        if (!filterIsEmpty) {
            builder.addFilter(filter.build());
        }
    }
}
