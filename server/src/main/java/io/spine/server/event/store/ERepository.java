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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.protobuf.FieldMask;
import io.spine.client.EntityFilters;
import io.spine.client.OrderBy;
import io.spine.client.Pagination;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.server.entity.DefaultRecordBasedRepository;
import io.spine.server.event.EventStreamQuery;

import java.util.Iterator;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.core.Events.eventComparator;

/**
 * A storage used by {@link EventStore} for keeping event data.
 *
 * <p>This class allows to hide implementation details of storing events.
 * {@link EventStore} serves as a facade, hiding the fact that the {@code EventStorage}
 * is a {@code Repository}.
 */
final class ERepository extends DefaultRecordBasedRepository<EventId, EEntity, Event> {

    /**
     * Obtains an iterator over events matching the passed query.
     * The iteration is chronologically sorted.
     */
    Iterator<Event> iterator(EventStreamQuery query) {
        checkNotNull(query);
        Iterator<EEntity> entities = find(query);
        Predicate<Event> predicate = new MatchesStreamQuery(query);
        Iterator<Event> result =
                Streams.stream(entities)
                       .map(EEntity::getState)
                       .filter(predicate)
                       .sorted(eventComparator())
                       .iterator();
        return result;
    }

    /**
     * Obtains iteration over entities matching the passed query.
     */
    private Iterator<EEntity> find(EventStreamQuery query) {
        EntityFilters filters = QueryToFilters.convert(query);
        return find(filters, OrderBy.getDefaultInstance(),
                    Pagination.getDefaultInstance(),
                    FieldMask.getDefaultInstance());
    }

    void store(Event event) {
        EEntity entity = EEntity.create(event);
        store(entity);
    }

    void store(Iterable<Event> events) {
        ImmutableList<EEntity> entities =
                Streams.stream(events)
                       .map(EEntity::create)
                       .collect(toImmutableList());
        store(entities);
    }
}
