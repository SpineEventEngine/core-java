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
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.DefaultRecordBasedRepository;
import org.spine3.server.entity.EntityRecord;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A storage used by {@link EventStore} for keeping event data.
 *
 * <p>This class allows to hide implementation details of storing commands.
 * {@link EventStore} serves as a facade, hiding the fact that the {@code EventStorage}
 * is a {@code Repository}.
 *
 * @author Alexander Yevsyukov
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
        final Iterator<EventEntity> filtered = iterator(createFilter(query));
        final List<EventEntity> entities = Lists.newArrayList(filtered);
        Collections.sort(entities, EventEntity.comparator());
        final Iterator<Event> result = Iterators.transform(entities.iterator(), getEventFunc());
        return result;
    }

    private static Predicate<EventEntity> createFilter(EventStreamQuery query) {
        return new EventEntityMatchesStreamQuery(query).toEntityPredicate();
    }

    void store(Event event) {
        final EventEntity entity = new EventEntity(event);
        store(entity);
    }

    private static Function<EventEntity, Event> getEventFunc() {
        return GET_EVENT;
    }

    PTransform<PBegin, PCollection<Event>>
    readTransform(TenantId tenantId, SerializableFunction<Event, Boolean> filter) {
        final PTransform<PBegin, PCollection<EntityRecord>> recordTransform =
                recordStorage().readTransform(tenantId, new FilterEntityRecord(filter));

        //TODO:2017-05-13:alexander.yevsyukov: Finish.
        return null;
    }

    /**
     * Filters {@link EntityRecord} containing event by the passed event filter.
     */
    private static class FilterEntityRecord implements SerializableFunction<EntityRecord, Boolean> {
        private static final long serialVersionUID = 0L;
        private final SerializableFunction<Event, Boolean> filter;

        private FilterEntityRecord(SerializableFunction<Event, Boolean> filter) {
            this.filter = filter;
        }

        @Override
        public Boolean apply(EntityRecord input) {
            final Event event = AnyPacker.unpack(input.getState());
            final boolean result = filter.apply(event);
            return result;
        }
    }

    private static class FilterEventsTransform extends PTransform<PBegin, PCollection<Event>> {

        @Override
        public PCollection<Event> expand(PBegin input) {
            //TODO:2017-05-13:alexander.yevsyukov: Finish.
            return null;
        }
    }

    /**
     * A serializable predicate that filters events matching an {@link EventStreamQuery}.
     */
    private static class EventEntityMatchesStreamQuery implements EventPredicate {

        private static final long serialVersionUID = 0L;
        private final EventPredicate filter;

        private EventEntityMatchesStreamQuery(EventStreamQuery query) {
            this.filter = new MatchesStreamQuery(query);
        }

        @Override
        public Boolean apply(Event input) {
            final boolean result = filter.apply(input);
            return result;
        }

        /**
         * Converts this instance to a predicate that filters {@link EventEntity} instances.
         */
        private Predicate<EventEntity> toEntityPredicate() {
            return new Predicate<EventEntity>() {
                @Override
                public boolean apply(@Nullable EventEntity input) {
                    if (input == null) {
                        return false;
                    }
                    return EventEntityMatchesStreamQuery.this.apply(input.getState());
                }
            };
        }
    }
}
