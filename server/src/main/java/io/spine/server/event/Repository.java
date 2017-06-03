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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import io.spine.base.Event;
import io.spine.base.EventId;
import io.spine.server.entity.DefaultRecordBasedRepository;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.EventRecordStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;

/**
 * A storage used by {@link EventStore} for keeping event data.
 *
 * <p>This class allows to hide implementation details of storing commands.
 * {@link EventStore} serves as a facade, hiding the fact that the {@code EventStorage}
 * is a {@code Repository}.
 *
 * @author Alexander Yevsyukov
 */
class Repository extends DefaultRecordBasedRepository<EventId, Entity, Event> {

    private static final Function<Entity, Event> GET_EVENT =
            new Function<Entity, Event>() {
                @Nullable
                @Override
                public Event apply(@Nullable Entity input) {
                    if (input == null) {
                        return null;
                    }
                    return input.getState();
                }
            };

    @Override
    protected EventRecordStorage createStorage(StorageFactory factory) {
        final RecordStorage<EventId> recordStorage = super.createStorage(factory);
        final EventRecordStorage storage =
                factory.createEventStorage(recordStorage);
        return storage;
    }

    @Nonnull
    @Override
    protected EventRecordStorage recordStorage() {
        return (EventRecordStorage) super.recordStorage();
    }

    Iterator<Event> iterator(EventStreamQuery query) {
        final EventRecordStorage storage = recordStorage();
        final Map<EventId, EntityRecord> records = storage.readAll(query);
        final Collection<Entity> entities = transform(records.entrySet(),
                                                      storageRecordToEntity());
        // TODO:2017-05-19:dmytro.dashenkov: Remove after the Entity Column approach is implemented.
        final Collection<Entity> filtered = filter(entities, createEntityFilter(query));

        final List<Entity> entityList = newArrayList(filtered);
        Collections.sort(entityList, Entity.comparator());
        final Iterator<Event> result = Iterators.transform(entityList.iterator(), getEventFunc());
        return result;
    }

    @VisibleForTesting
    static Predicate<Entity> createEntityFilter(EventStreamQuery query) {
        return new EventEntityMatchesStreamQuery(query);
    }

    void store(Event event) {
        final Entity entity = new Entity(event);
        store(entity);
    }

    static Function<Entity, Event> getEventFunc() {
        return GET_EVENT;
    }

    private static class EventEntityMatchesStreamQuery implements Predicate<Entity> {

        private final Predicate<Event> filter;

        private EventEntityMatchesStreamQuery(EventStreamQuery query) {
            this.filter = new MatchesStreamQuery(query);
        }

        @Override
        public boolean apply(@Nullable Entity input) {
            if (input == null) {
                return false;
            }
            final Event event = input.getState();
            final boolean result = filter.apply(event);
            return result;
        }
    }
}
