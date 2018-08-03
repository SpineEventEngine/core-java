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

package io.spine.server.aggregate;

import io.spine.core.Event;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link Aggregate} write operation.
 *
 * <p>Stores the given aggregate into
 *
 * @author Dmytro Dashenkov
 */
final class Write<I> {

    private final AggregateStorage<I> storage;
    private final Aggregate<I, ?, ?> aggregate;
    private final int snapshotTrigger;

    private Write(Builder<I> builder) {
        this.storage = builder.repository.aggregateStorage();
        this.snapshotTrigger = builder.repository.getSnapshotTrigger();
        this.aggregate = builder.aggregate;
    }

    /**
     * Performs this write operation.
     */
    void perform() {
        I id = aggregate.getId();
        int eventCount = storage.readEventCountAfterLastSnapshot(id);
        UncommittedEvents uncommittedEvents = aggregate.getUncommittedEvents();
        for (Event event : uncommittedEvents.list()) {
            eventCount = writeEvent(event, eventCount);
        }
        commit(eventCount);
    }

    private int writeEvent(Event event, int eventCount) {
        storage.writeEvent(aggregate.getId(), event);
        int newEventCount = eventCount + 1;
        if (newEventCount >= snapshotTrigger) {
            writeSnapshot(aggregate);
            newEventCount = 0;
        }
        return newEventCount;
    }

    private void writeSnapshot(Aggregate<I, ? ,?> aggregate) {
        Snapshot snapshot = aggregate.toShapshot();
        aggregate.clearRecentHistory();
        storage.writeSnapshot(aggregate.getId(), snapshot);
    }

    private void commit(int eventCount) {
        aggregate.commitEvents();
        storage.writeEventCountAfterLastSnapshot(aggregate.getId(), eventCount);

        if (aggregate.lifecycleFlagsChanged()) {
            storage.writeLifecycleFlags(aggregate.getId(), aggregate.getLifecycleFlags());
        }
    }

    /**
     * Creates a new {@link Builder} for a {@code Write} operation.
     *
     * @param <I> the type of ID of the {@link Aggregate} which is written
     * @return new {@link Builder}
     */
    static <I> Builder<I> operation() {
        return new Builder<>();
    }

    /**
     * A builder for a {@code Write} operation.
     *
     * @param <I> the type of ID of the {@link Aggregate} which is written
     */
    static final class Builder<I> {

        private AggregateRepository<I, ? extends Aggregate<I, ?, ?>> repository;
        private Aggregate<I, ?, ?> aggregate;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        /**
         * @param aggregate the {@link Aggregate} to store
         */
        Builder<I> write(Aggregate<I, ?, ?> aggregate) {
            this.aggregate = checkNotNull(aggregate);
            return this;
        }

        /**
         * @param repository the {@link AggregateRepository} to store the aggregate into
         */
        Builder<I> into(AggregateRepository<I, ? extends Aggregate<I, ?, ?>> repository) {
            this.repository = checkNotNull(repository);
            return this;
        }

        /**
         * Creates a new instance of {@code Write} with the set attributes.
         *
         * @return new instance of {@code Write} operation
         */
        Write<I> prepare() {
            checkNotNull(aggregate);
            checkNotNull(repository);

            return new Write<>(this);
        }
    }
}
