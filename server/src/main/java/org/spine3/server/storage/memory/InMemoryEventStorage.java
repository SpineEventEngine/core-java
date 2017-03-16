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

package org.spine3.server.storage.memory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.event.EventStorage;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.event.MatchesStreamQuery;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.filter;

/**
 * In-memory implementation of {@link EventStorage}.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
class InMemoryEventStorage extends EventStorage {

    private final MultitenantStorage<TenantEvents> multitenantStorage;

    protected InMemoryEventStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantEvents>(multitenant) {
            @Override
            TenantEvents createSlice() {
                return new TenantEvents();
            }
        };
    }

    @Override
    public Iterator<Event> iterator(EventStreamQuery query) {
        return getStorage().iterator(query);
    }

    @Override
    public void write(EventId id, Event event) {
        checkNotNull(id);
        checkNotNull(event);
        checkNotClosed();
        getStorage().put(id, event);
    }

    @Override
    public Iterator<EventId> index() {
        return getStorage().index();
    }

    @Override
    public Optional<Event> read(EventId eventId) {
        checkNotNull(eventId);
        checkNotClosed();
        return getStorage().get(eventId);
    }

    private TenantEvents getStorage() {
        return multitenantStorage.getStorage();
    }

    /**
     * Data “slice” of a tenant that stores events.
     */
    private static class TenantEvents implements TenantStorage<EventId, Event> {

        private static final int INITIAL_CAPACITY = 100;

        @SuppressWarnings("CollectionDeclaredAsConcreteClass") // to stress that the queue is sorted.
        private final PriorityQueue<Event> storage = new PriorityQueue<>(
                INITIAL_CAPACITY,
                Events.eventComparator());
        /**
         * The index of records where keys are string values of {@code EventId}s. */
        private final Map<EventId, Event> index = Maps.newConcurrentMap();

        @Override
        public Iterator<EventId> index() {
            final Iterator<EventId> result = index.keySet()
                                                  .iterator();
            return result;
        }

        @Nullable
        @Override
        public Optional<Event> get(EventId id) {
            return Optional.fromNullable(index.get(id));
        }

        private Iterator<Event> iterator(EventStreamQuery query) {
            final Predicate<Event> matchesQuery = new MatchesStreamQuery(query);
            final Iterator<Event> result = filter(storage.iterator(), matchesQuery);
            return result;
        }

        @Override
        public void put(EventId id, Event record) {
            index.put(id, record);
            storage.add(record);
        }

        @Override
        public boolean isEmpty() {
            return storage.isEmpty();
        }
    }
}
