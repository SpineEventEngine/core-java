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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.server.entity.DefaultRecordBasedRepository;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.StorageFactory;

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

    @Override
    protected org.spine3.server.storage.EventStorage createStorage(StorageFactory factory) {
        final RecordStorage<EventId> recordStorage = super.createStorage(factory);
        final org.spine3.server.storage.EventStorage storage =
                factory.createEventStorage(recordStorage);
        return storage;
    }

    @Nonnull
    @Override
    protected org.spine3.server.storage.EventStorage recordStorage() {
        return (org.spine3.server.storage.EventStorage) super.recordStorage();
    }

    Iterator<Event> iterator(EventStreamQuery query) {
        final org.spine3.server.storage.EventStorage storage = recordStorage();
        final Map<EventId, EntityRecord> records = storage.readAll(query);
        final Collection<EventEntity> entities = transform(records.entrySet(),
                                                           storageRecordToEntity());
        // TODO:2017-05-19:dmytro.dashenkov: Remove after the Entity Column approach is implemented.
        final Collection<EventEntity> filtered = filter(entities, createEntityFilter(query));

        final List<EventEntity> entityList = newArrayList(filtered);
        Collections.sort(entityList, EventEntity.comparator());
        final Iterator<Event> result = Iterators.transform(entityList.iterator(), getEventFunc());
        return result;
    }

    @VisibleForTesting
    static Predicate<EventEntity> createEntityFilter(EventStreamQuery query) {
        return new EventEntityMatchesStreamQuery(query);
    }

    void store(Event event) {
        final EventEntity entity = new EventEntity(event);
        store(entity);
    }

    static Function<EventEntity, Event> getEventFunc() {
        return GET_EVENT;
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
