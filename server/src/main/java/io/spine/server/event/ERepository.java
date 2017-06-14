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

package io.spine.server.event;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import io.spine.base.Event;
import io.spine.base.EventId;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.server.entity.DefaultRecordBasedRepository;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordWithColumns;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.gt;
import static io.spine.client.ColumnFilters.lt;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.server.event.EEntity.CREATED_TIME_COLUMN;
import static io.spine.server.event.EEntity.TYPE_COLUMN;
import static io.spine.server.event.EEntity.comparator;

/**
 * A storage used by {@link EventStore} for keeping event data.
 *
 * <p>This class allows to hide implementation details of storing events.
 * {@link EventStore} serves as a facade, hiding the fact that the {@code EventStorage}
 * is a {@code Repository}.
 *
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
class ERepository extends DefaultRecordBasedRepository<EventId, EEntity, Event> {

    private static final Function<EEntity, Event> GET_EVENT =
            new Function<EEntity, Event>() {
                @Nullable
                @Override
                public Event apply(@Nullable EEntity input) {
                    if (input == null) {
                        return null;
                    }
                    return input.getState();
                }
            };

    Iterator<Event> iterator(EventStreamQuery query) {
        checkNotNull(query);

        final EntityFilters filters = toEntityFilters(query);
        final Iterable<EEntity> entities = find(filters, FieldMask.getDefaultInstance());
        // A predicate on the Event message and EventContext fields.
        final Predicate<EEntity> detailedLookupFilter = createEntityFilter(query);
        final Iterator<EEntity> entityIterator = FluentIterable.from(entities)
                                                                   .filter(detailedLookupFilter)
                                                                   .toSortedList(comparator())
                                                                   .iterator();
        final Iterator<Event> result = transform(entityIterator, getEvent());
        return result;
    }

    void store(Event event) {
        final EEntity entity = new EEntity(event);
        store(entity);
    }

    void store(Iterable<Event> events) {
        final Iterable<EEntity> entities = Iterables.transform(events, EventToEEntity.instance());
        store(newLinkedList(entities));
    }

    private void store(Collection<EEntity> entities) {
        final Map<EventId, EntityRecordWithColumns> records = new HashMap<>(entities.size());
        for (EEntity entity : entities) {
            final EntityRecord record = entityConverter().convert(entity);
            checkNotNull(record);
            final EntityRecordWithColumns recordWithColumns = EntityRecordWithColumns.create(
                    record,
                    entity
            );
            records.put(entity.getId(), recordWithColumns);
        }
        recordStorage().write(records);
    }

    static Function<EEntity, Event> getEvent() {
        return GET_EVENT;
    }

    private static Predicate<EEntity> createEntityFilter(EventStreamQuery query) {
        return new EEntityMatchesStreamQuery(query);
    }

    /**
     * Creates an instance of {@link EntityFilters} from the given {@link EventStreamQuery}.
     *
     * <p>The resulting filters contain the filtering by {@code before} and {@code after} fields
     * of the source query and by the {@code eventType} field of the underlying
     * {@linkplain EventFilter EventFilters}.
     *
     * @param query the source {@link EventStreamQuery} to get the info from
     * @return new instance of {@link EntityFilters} filtering the events
     */
    private static EntityFilters toEntityFilters(EventStreamQuery query) {
        final CompositeColumnFilter timeFilter = timeFilter(query);
        final CompositeColumnFilter typeFilter = typeFilter(query);
        final EntityFilters entityFilters = EntityFilters.newBuilder()
                                                         .addFilter(timeFilter)
                                                         .addFilter(typeFilter)
                                                         .build();
        return entityFilters;
    }

    private static CompositeColumnFilter timeFilter(EventStreamQuery query) {
        final CompositeColumnFilter.Builder timeFilter = CompositeColumnFilter.newBuilder()
                                                                              .setOperator(ALL);
        if (query.hasAfter()) {
            final Timestamp timestamp = query.getAfter();
            final ColumnFilter filter = gt(CREATED_TIME_COLUMN, timestamp);
            timeFilter.addFilter(filter);
        }
        if (query.hasBefore()) {
            final Timestamp timestamp = query.getBefore();
            final ColumnFilter filter = lt(CREATED_TIME_COLUMN, timestamp);
            timeFilter.addFilter(filter);
        }
        return timeFilter.build();
    }

    private static CompositeColumnFilter typeFilter(EventStreamQuery query) {
        final CompositeColumnFilter.Builder typeFilter = CompositeColumnFilter.newBuilder()
                                                                              .setOperator(EITHER);
        for (EventFilter eventFilter : query.getFilterList()) {
            final String type = eventFilter.getEventType();
            if (!type.isEmpty()) {
                final ColumnFilter filter = eq(TYPE_COLUMN, type);
                typeFilter.addFilter(filter);
            }
        }
        return typeFilter.build();
    }

    private static class EEntityMatchesStreamQuery implements Predicate<EEntity> {

        private final Predicate<Event> filter;

        private EEntityMatchesStreamQuery(EventStreamQuery query) {
            this.filter = new MatchesStreamQuery(query);
        }

        @Override
        public boolean apply(@Nullable EEntity input) {
            if (input == null) {
                return false;
            }
            final Event event = input.getState();
            final boolean result = filter.apply(event);
            return result;
        }
    }

    private enum EventToEEntity implements Function<Event, EEntity> {

        INSTANCE;

        private static Function<Event, EEntity> instance() {
            return INSTANCE;
        }

        @Override
        public EEntity apply(@Nullable Event event) {
            checkNotNull(event);
            return new EEntity(event);
        }
    }
}
