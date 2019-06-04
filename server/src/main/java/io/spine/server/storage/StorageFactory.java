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

import io.spine.core.BoundedContextName;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.entity.Entity;
import io.spine.server.event.store.EventStore;
import io.spine.server.inbox.InboxStorage;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;

/**
 * A factory for creating storages used by repositories
 * {@link EventStore EventStore}
 * and {@link io.spine.server.stand.Stand Stand}.
 */
public interface StorageFactory extends AutoCloseable {

    /**
     * Verifies if the storage factory is configured to serve a multi-tenant application.
     *
     * @return {@code true} if the factory produces multi-tenant storages,
     *         {@code false} otherwise
     */
    boolean isMultitenant();

    /**
     * Creates a new {@link AggregateStorage} instance.
     *
     * @param <I>            the type of aggregate IDs
     * @param aggregateClass the class of aggregates to store
     */
    <I> AggregateStorage<I> createAggregateStorage(
            Class<? extends Aggregate<I, ?, ?>> aggregateClass);

    /**
     * Creates a new {@link RecordStorage} instance.
     *
     * @param <I>         the type of entity IDs
     * @param entityClass the class of entities to store
     */
    <I> RecordStorage<I> createRecordStorage(Class<? extends Entity<I, ?>> entityClass);

    /**
     * Creates a new {@link ProjectionStorage} instance.
     *
     * @param <I>             the type of stream projection IDs
     * @param projectionClass the class of projections to be stored
     */
    <I> ProjectionStorage<I> createProjectionStorage(
            Class<? extends Projection<I, ?, ?>> projectionClass);

    /**
     * Creates a new {@link InboxStorage InboxStorage} instance.
     */
    InboxStorage createInboxStorage();

    /**
     * Creates a single-tenant version of the factory.
     *
     * <p>This method is needed for creating single-tenant storages using
     * a multi-tenant instance of a {@code StorageFactory}.
     *
     * @return a single-tenant version of the factory, or {@code this}
     *         if the factory is single-tenant
     */
    StorageFactory toSingleTenant();

    /**
     * Creates a new instance for serving a {@code BoundedContext} with the passed name
     * and multi-tenancy status.
     */
    StorageFactory copyFor(BoundedContextName name, boolean multitenant);
}
