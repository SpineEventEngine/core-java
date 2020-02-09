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

package io.spine.server.storage.memory;

import io.spine.base.EntityState;
import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.delivery.CatchUpStorage;
import io.spine.server.delivery.InboxStorage;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;

import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;

/**
 * A factory for in-memory storages.
 */
public final class InMemoryStorageFactory implements StorageFactory {

    /**
     * Creates new instance of the factory which would serve the specified context.
     *
     * @return new instance of the factory
     */
    public static InMemoryStorageFactory newInstance() {
        return new InMemoryStorageFactory();
    }

    private InMemoryStorageFactory() {
    }

    /** <b>NOTE</b>: the parameter is unused. */
    @Override
    public <I> AggregateStorage<I>
    createAggregateStorage(ContextSpec context, Class<? extends Aggregate<I, ?, ?>> unused) {
        return new InMemoryAggregateStorage<>(context.isMultitenant());
    }

    @Override
    public <I> RecordStorage<I>
    createRecordStorage(ContextSpec context, Class<? extends Entity<I, ?>> entityClass) {
        EntityClass<?> modelClass = asEntityClass(entityClass);
        StorageSpec<I> storageSpec = toStorageSpec(context, modelClass);
        return new InMemoryRecordStorage<>(storageSpec, entityClass, context.isMultitenant());
    }

    @Override
    public <I> ProjectionStorage<I>
    createProjectionStorage(ContextSpec context,
                            Class<? extends Projection<I, ?, ?>> projectionClass) {
        EntityClass<?> modelClass = asProjectionClass(projectionClass);
        StorageSpec<I> storageSpec = toStorageSpec(context, modelClass);
        InMemoryRecordStorage<I> recordStorage =
                new InMemoryRecordStorage<>(storageSpec, projectionClass, context.isMultitenant());
        return new InMemoryProjectionStorage<>(projectionClass, recordStorage);
    }

    @Override
    public InboxStorage createInboxStorage(boolean multitenant) {
        return new InMemoryInboxStorage(multitenant);
    }

    @Override
    public CatchUpStorage createCatchUpStorage(boolean multitenant) {
        return new InMemoryCatchUpStorage(multitenant);
    }

    /**
     * Obtains storage specification for the passed entity class.
     */
    private static <I>
    StorageSpec<I> toStorageSpec(ContextSpec context, EntityClass<?> modelClass) {
        Class<? extends EntityState> stateClass = modelClass.stateClass();
        @SuppressWarnings("unchecked") // The cast is protected by generic parameters of the method.
        Class<I> idClass = (Class<I>) modelClass.idClass();
        TypeUrl stateUrl = TypeUrl.of(stateClass);
        StorageSpec<I> result = StorageSpec.of(context.name(), stateUrl, idClass);
        return result;
    }

    @Override
    public void close() {
        // NOP
    }
}
