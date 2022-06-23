/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.aggregate;

import io.spine.core.Event;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayListWithCapacity;

/**
 * An {@link Aggregate} write operation.
 *
 * <p>Stores the given aggregate into the associated storage.
 */
final class Write<I> {

    private final AggregateStorage<I> storage;
    private final Aggregate<I, ?, ?> aggregate;
    private final I id;
    private final int snapshotTrigger;

    private Write(AggregateStorage<I> storage,
                  Aggregate<I, ?, ?> aggregate,
                  I id,
                  int snapshotTrigger) {
        this.storage = storage;
        this.aggregate = aggregate;
        this.id = id;
        this.snapshotTrigger = snapshotTrigger;
    }

    /**
     * Creates a new instance of {@code Write} operation.
     *
     * <p>The resulting operation stores the given {@link Aggregate} into the given
     * {@link AggregateRepository}.
     *
     * @param repository the target {@link AggregateRepository}
     * @param aggregate  the {@link Aggregate} to write
     * @param <I>        the type of the aggregate ID
     * @return new {@code Write} operation
     */
    static <I> Write<I> operationFor(AggregateRepository<I, ?> repository,
                                     Aggregate<I, ?, ?> aggregate) {
        checkNotNull(repository);
        checkNotNull(aggregate);

        AggregateStorage<I> storage = repository.aggregateStorage();
        int snapshotTrigger = repository.snapshotTrigger();
        I id = aggregate.id();
        return new Write<>(storage, aggregate, id, snapshotTrigger);
    }

    /**
     * Performs this write operation.
     */
    void perform() {
        UncommittedEvents uncommittedEvents = aggregate.getUncommittedEvents();
        List<Event> eventsToStore = uncommittedEvents.list();
        writeEvents(eventsToStore);
    }

    private void writeEvents(List<Event> events) {
        int eventCount = aggregate.eventCountAfterLastSnapshot();
        Collection<Event> eventBatch = newArrayListWithCapacity(snapshotTrigger);
        for (Event event : events) {
            eventBatch.add(event);
            eventCount++;
            if (eventCount >= snapshotTrigger) {
                persist(events, aggregate.toSnapshot());
                aggregate.clearRecentHistory();
                eventBatch.clear();
                eventCount = 0;
            }
        }
        if (!eventBatch.isEmpty()) {
            persist(eventBatch);
        }
        commit(eventCount);
    }

    private void persist(Collection<Event> events, Snapshot snapshot) {
        AggregateHistory record = AggregateHistory
                .newBuilder()
                .addAllEvent(events)
                .setSnapshot(snapshot)
                .build();
        persist(record);
    }

    private void persist(Collection<Event> events) {
        AggregateHistory record = AggregateHistory
                .newBuilder()
                .addAllEvent(events)
                .build();
        persist(record);
    }

    private void persist(AggregateHistory record) {
        storage.write(id, record);
    }

    private void commit(int eventCount) {
        aggregate.commitEvents();
        aggregate.setEventCountAfterLastSnapshot(eventCount);
        if (aggregate.lifecycleFlagsChanged()) {
            storage.writeLifecycleFlags(aggregate.id(), aggregate.lifecycleFlags());
        }
    }
}
