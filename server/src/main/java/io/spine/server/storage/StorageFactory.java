/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateEventStorage;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.delivery.CatchUpStorage;
import io.spine.server.delivery.InboxStorage;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.event.EventStore;
import io.spine.server.event.store.DefaultEventStore;

/**
 * A factory for creating storages used by repositories {@link EventStore EventStore}
 * and {@link io.spine.server.stand.Stand Stand}.
 */
public interface StorageFactory extends AutoCloseable {

    <I, M extends Message> MessageStorage<I, M>
    createMessageStorage(Columns<M> columns, boolean multitenant);

    /**
     * Creates a new {@link AggregateStorage}.
     *
     * @param <I>
     *         the type of aggregate IDs
     * @param context
     *         specification of the Bounded Context {@code AggregateRepository} of which
     *         requests the creation of the storage
     * @param aggregateCls
     *         the class of {@code Aggregate}s to be stored
     */
    default <I> AggregateStorage<I>
    createAggregateStorage(ContextSpec context, Class<? extends Aggregate<I, ?, ?>> aggregateCls) {
        return new AggregateStorage<>(context, aggregateCls, this);
    }

    /**
     * Creates a new {@link AggregateEventStorage}.
     *
     * @param multitenant
     *         whether the created storage should be multi-tenant
     */
    default AggregateEventStorage createAggregateEventStorage(boolean multitenant) {
        return new AggregateEventStorage(this, multitenant);
    }

    /**
     * Creates a new {@link InboxStorage}.
     *
     * <p>The instance of {@code InboxStorage} is used in the {@link
     * io.spine.server.delivery.Delivery Delivery} operations. Therefore there is typically just
     * a single instance of {@code InboxStorage} per {@link io.spine.server.ServerEnvironment
     * ServerEnvironment} instance, unlike other {@code Storage} types which instances are created
     * per-{@link io.spine.server.BoundedContext BoundedContext}.
     *
     * @param multitenant
     *         whether the created storage should be multi-tenant
     */
    default InboxStorage createInboxStorage(boolean multitenant) {
        return new InboxStorage(this, multitenant);
    }

    /**
     * Creates a new {@link CatchUpStorage}.
     *
     * <p>Similar to {@link InboxStorage}, this type of storage is also used in the {@link
     * io.spine.server.delivery.Delivery Delivery} routines. So by default there is a single
     * instance of {@code CatchUpStorage} per {@link io.spine.server.ServerEnvironment
     * ServerEnvironment}.
     *
     * @param multitenant
     *         whether the created storage should be multi-tenant
     */
    default CatchUpStorage createCatchUpStorage(boolean multitenant) {
        return new CatchUpStorage(this, multitenant);
    }

    /**
     * Creates a new {@link EventStore}.
     *
     * @param context
     *         specification of the Bounded Context events of which the store would serve
     */
    default EventStore createEventStore(@SuppressWarnings("unused") ContextSpec context) {
        return new DefaultEventStore(this, context.isMultitenant());
    }

    /**
     * Creates a new {@link EntityRecordStorage}.
     *
     * @param <I>
     *         the type of entity IDs
     * @param context
     *         specification of the Bounded Context {@code RecordBasedRepository} of which
     *         requests the creation of the storage
     * @param entityClass
     *         the class of entities to be stored
     */
    default <I> EntityRecordStorage<I>
    createEntityRecordStorage(ContextSpec context, Class<? extends Entity<I, ?>> entityClass) {
        EntityRecordStorage<I> result =
                new EntityRecordStorage<>(this, entityClass, context.isMultitenant());
        return result;
    }
}
