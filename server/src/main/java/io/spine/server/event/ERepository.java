/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.server.entity.DefaultRecordBasedRepository;
import io.spine.server.storage.RecordStorage;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.gt;
import static io.spine.client.ColumnFilters.lt;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.server.event.EEntity.CREATED_TIME_COLUMN;
import static io.spine.server.event.EEntity.TYPE_COLUMN;
import static io.spine.server.event.EEntity.comparator;
import static java.util.stream.Collectors.toList;

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
                @Override
                public @Nullable Event apply(@Nullable EEntity input) {
                    if (input == null) {
                        return null;
                    }
                    return input.getState();
                }
            };

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open the method to the package.
     */
    @SuppressWarnings("RedundantMethodOverride") // See doc.
    @Override
    protected RecordStorage<EventId> recordStorage() {
        return super.recordStorage();
    }

    Iterator<Event> iterator(EventStreamQuery query) {
        checkNotNull(query);

        EntityFilters filters = toEntityFilters(query);
        Iterator<EEntity> entities = find(filters, FieldMask.getDefaultInstance());
        // A predicate on the Event message and EventContext fields.
        Predicate<EEntity> detailedLookupFilter = createEntityFilter(query);
        Iterator<EEntity> filtered = Streams.stream(entities)
                                            .filter(detailedLookupFilter)
                                            .collect(toList())
                                            .iterator();
        List<EEntity> entityList = newArrayList(filtered);
        entityList.sort(comparator());
        Iterator<Event> result = entityList.stream()
                                           .map(getEvent())
                                           .collect(toList())
                                           .iterator();
        return result;
    }

    void store(Event event) {
        EEntity entity = new EEntity(event);
        store(entity);
    }

    void store(Iterable<Event> events) {
        Iterable<EEntity> entities = Streams.stream(events)
                                            .map(EventToEEntity.instance())
                                            .collect(toList());
        store(newArrayList(entities));
    }

    private static Function<EEntity, Event> getEvent() {
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
    @SuppressWarnings("CheckReturnValue") // calling builder
    @VisibleForTesting
    static EntityFilters toEntityFilters(EventStreamQuery query) {
        EntityFilters.Builder builder = EntityFilters.newBuilder();

        Optional<CompositeColumnFilter> timeFilter = timeFilter(query);
        timeFilter.ifPresent(builder::addFilter);

        Optional<CompositeColumnFilter> typeFilter = typeFilter(query);
        typeFilter.ifPresent(builder::addFilter);

        return builder.build();
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private static Optional<CompositeColumnFilter> timeFilter(EventStreamQuery query) {
        CompositeColumnFilter.Builder timeFilter = CompositeColumnFilter.newBuilder()
                                                                        .setOperator(ALL);
        if (query.hasAfter()) {
            Timestamp timestamp = query.getAfter();
            ColumnFilter filter = gt(CREATED_TIME_COLUMN, timestamp);
            timeFilter.addFilter(filter);
        }
        if (query.hasBefore()) {
            Timestamp timestamp = query.getBefore();
            ColumnFilter filter = lt(CREATED_TIME_COLUMN, timestamp);
            timeFilter.addFilter(filter);
        }

        return buildFilter(timeFilter);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private static Optional<CompositeColumnFilter> typeFilter(EventStreamQuery query) {
        CompositeColumnFilter.Builder typeFilter =
                CompositeColumnFilter.newBuilder()
                                     .setOperator(EITHER);
        for (EventFilter eventFilter : query.getFilterList()) {
            String type = eventFilter.getEventType()
                                     .trim();
            if (!type.isEmpty()) {
                ColumnFilter filter = eq(TYPE_COLUMN, type);
                typeFilter.addFilter(filter);
            }
        }

        return buildFilter(typeFilter);
    }

    /**
     * Obtains a {@code CompositeColumnFilter} from the specified builder.
     *
     * @param builder the builder of the filter
     * @return {@code Optional} of the {@code CompositeColumnFilter}, if there are column filters
     * in the builder; {@code Optional.empty()} otherwise
     */
    private static Optional<CompositeColumnFilter> buildFilter(
            CompositeColumnFilter.Builder builder) {
        boolean filterIsEmpty = builder.getFilterList()
                                       .isEmpty();
        return filterIsEmpty
               ? Optional.empty()
               : Optional.of(builder.build());
    }

    private static class EEntityMatchesStreamQuery implements Predicate<EEntity> {

        private final Predicate<Event> filter;

        private EEntityMatchesStreamQuery(EventStreamQuery query) {
            this.filter = new MatchesStreamQuery(query);
        }

        @Override
        public boolean test(@Nullable EEntity input) {
            if (input == null) {
                return false;
            }
            Event event = input.getState();
            boolean result = filter.test(event);
            return result;
        }
    }

    /**
     * Transforms an {@link Event} to the {@linkplain EEntity Event Entity}.
     */
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
