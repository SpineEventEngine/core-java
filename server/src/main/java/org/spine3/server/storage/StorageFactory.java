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

package org.spine3.server.storage;

import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateStorage;
import org.spine3.server.command.CommandStorage;
import org.spine3.server.entity.Entity;
import org.spine3.server.event.EventStorage;
import org.spine3.server.projection.ProjectionStorage;
import org.spine3.server.stand.StandStorage;

/**
 * A factory for creating storages used by repositories,
 * {@link org.spine3.server.command.CommandStore CommandStore},
 * {@link org.spine3.server.event.EventStore EventStore},
 * and {@link org.spine3.server.stand.Stand Stand}.
 *
 * @author Alexander Yevsyukov
 */
public interface StorageFactory extends AutoCloseable {

    /**
     * Verifies if the storage factory is configured to serve a multi-tenant application.
     *
     * @return {@code true} if the factory would produce multi-tenant storages,
     *         {@code false} otherwise
     */
    boolean isMultitenant();

    /** Creates a new {@link CommandStorage} instance. */
    CommandStorage createCommandStorage();

    /** Creates a new {@link EventStorage} instance. */
    EventStorage createEventStorage();

    /** Creates a new {@link StandStorage} instance. */
    StandStorage createStandStorage();

    /**
     * Creates a new {@link AggregateStorage} instance.
     *
     * @param <I>            the type of aggregate IDs
     * @param aggregateClass the class of aggregates to store
     */
    <I> AggregateStorage<I> createAggregateStorage(Class<? extends Aggregate<I, ?, ?>> aggregateClass);

    /**
     * Creates a new {@link RecordStorage} instance.
     *
     * @param <I>         the type of entity IDs
     * @param entityClass the class of entities to store
     */
    <I> RecordStorage<I> createRecordStorage(Class<? extends Entity<I,?>> entityClass);

    /**
     * Creates a new {@link ProjectionStorage} instance.
     *
     * @param <I>             the type of stream projection IDs
     * @param projectionClass the class of projections to store
     */
    <I> ProjectionStorage<I> createProjectionStorage(Class<? extends Entity<I,?>> projectionClass);
}
