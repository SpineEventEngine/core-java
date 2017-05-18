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

package org.spine3.server.event;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.client.ColumnFilter;
import org.spine3.client.ColumnFilters;
import org.spine3.client.EntityFilters;
import org.spine3.server.entity.DefaultRecordBasedRepository;

import javax.annotation.Nullable;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.event.EventEntity.CREATED_TIME_COLUMN;
import static org.spine3.server.event.EventEntity.comparator;

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
class EventStorage extends DefaultRecordBasedRepository<EventId, EventEntity, Event> {

    private static final Function<EventEntity, Event> GET_EVENT =
            new Function<EventEntity, Event>() {
                @Nullable
                @Override
                public Event apply(@Nullable EventEntity input) {
                    if (input == null) {
                        return null;
                    }
                    return input.getState();
                }
            };

    Iterator<Event> iterator(EventStreamQuery query) {
        checkNotNull(query);

        final EntityFilters filters = toEntityFilters(query);
        final Iterable<EventEntity> entities = find(filters, FieldMask.getDefaultInstance());

        // Optimize the filtering by excluding the time bounds.
        final EventStreamQuery queryRegardlessTime = query.toBuilder()
                                                          .clearBefore()
                                                          .clearAfter()
                                                          .build();
        final Predicate<EventEntity> detailedLookupFilter = createEntityFilter(queryRegardlessTime);

        final Iterator<EventEntity> entityIterator = FluentIterable.from(entities)
                                                                   .filter(detailedLookupFilter)
                                                                   .toSortedList(comparator())
                                                                   .iterator();
        final Iterator<Event> result = Iterators.transform(entityIterator, getEventFunc());
        return result;
    }

    void store(Event event) {
        final EventEntity entity = new EventEntity(event);
        store(entity);
    }

    static Function<EventEntity, Event> getEventFunc() {
        return GET_EVENT;
    }

    private static Predicate<EventEntity> createEntityFilter(EventStreamQuery query) {
        return new EventEntityMatchesStreamQuery(query);
    }

    /**
     * Creates an instance of {@link EntityFilters} from the given {@link EventStreamQuery}.
     *
     * <p>The resulting filters only contain the filtering by the {@code after} field of the source
     * query compared to the {@link EventEntity#getCreated()} column.
     *
     * @param query the source {@link EventStreamQuery} to get the info from
     * @return new instance of {@link EntityFilters} filtering the events by their timestamp
     */
    private static EntityFilters toEntityFilters(EventStreamQuery query) {
        final EntityFilters.Builder builder = EntityFilters.newBuilder();
        if (query.hasAfter()) {
            final Timestamp timestamp = query.getAfter();
            final ColumnFilter filter = ColumnFilters.gt(CREATED_TIME_COLUMN, timestamp);
            builder.addColumnFilter(filter);
        }
        if (query.hasBefore()) {
            final Timestamp timestamp = query.getBefore();
            final ColumnFilter filter = ColumnFilters.lt(CREATED_TIME_COLUMN, timestamp);
            builder.addColumnFilter(filter);
        }
        final EntityFilters entityFilters = builder.build();
        return entityFilters;
    }

    private static class EventEntityMatchesStreamQuery implements Predicate<EventEntity> {

        private final Predicate<Event> filter;

        private EventEntityMatchesStreamQuery(EventStreamQuery query) {
            this.filter = new MatchesStreamQuery(query);
        }

        @Override
        public boolean apply(@Nullable EventEntity input) {
            if (input == null) {
                return false;
            }
            final Event event = input.getState();
            final boolean result = filter.apply(event);
            return result;
        }
    }
}
