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

package io.spine.server.storage;

import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.entity.Entity;
import io.spine.server.event.store.EventStore;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;

/**
 * A factory for creating storages used by repositories
 * {@link EventStore EventStore}
 * and {@link io.spine.server.stand.Stand Stand}.
 */
public interface StorageFactory extends AutoCloseable {

    /**
     * Creates a new {@link AggregateStorage}.
     *
     * @param <I>
     *         the type of aggregate IDs
     * @param context
     *         specification of the Bounded Context {@code AggregateRepository} of which
     *         requests the creation of the storage
     * @param aggregateClass
     *         the class of {@code Aggregate}s to be stored
     */
    <I> AggregateStorage<I>
    createAggregateStorage(ContextSpec context, Class<? extends Aggregate<I, ?, ?>> aggregateClass);

    /**
     * Creates a new {@link RecordStorage}.
     *
     * @param <I>
     *         the type of entity IDs
     * @param context
     *         specification of the Bounded Context {@code RecordBasedRepository} of which
     *         requests the creation of the storage
     * @param entityClass
     *         the class of entities to be stored
     */
    <I> RecordStorage<I>
    createRecordStorage(ContextSpec context, Class<? extends Entity<I, ?>> entityClass);

    /**
     * Creates a new {@link ProjectionStorage}.
     *
     * @param <I>
     *         the type of stream projection IDs
     * @param context
     *         specification of the Bounded Context {@code ProjectionRepository} of which
     *         requests the creation of the storage
     * @param projectionClass
     *         the class of {@code Projection}s to be stored
     */
    <I> ProjectionStorage<I>
    createProjectionStorage(ContextSpec context,
                            Class<? extends Projection<I, ?, ?>> projectionClass);
}
