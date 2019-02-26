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

package io.spine.server.storage.memory;

import com.google.protobuf.Message;
import io.spine.core.BoundedContextName;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.entity.storage.ColumnTypeRegistry;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;

/**
 * A factory for in-memory storages.
 */
public class InMemoryStorageFactory implements StorageFactory {

    private final BoundedContextName boundedContextName;
    private final boolean multitenant;

    public static InMemoryStorageFactory newInstance(BoundedContextName boundedContextName,
                                                     boolean multitenant) {
        return new InMemoryStorageFactory(boundedContextName, multitenant);
    }

    private InMemoryStorageFactory(BoundedContextName boundedContextName, boolean multitenant) {
        this.boundedContextName = boundedContextName;
        this.multitenant = multitenant;
    }

    @Override
    public boolean isMultitenant() {
        return this.multitenant;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In-memory implementation stores no values separately
     * ({@link io.spine.server.entity.storage.EntityColumn entity columns}),
     * therefore returns an empty {@code ColumnTypeRegistry}.
     */
    @Override
    public ColumnTypeRegistry getTypeRegistry() {
        return ColumnTypeRegistry.newBuilder()
                                 .build();
    }

    /** NOTE: the parameter is unused. */
    @Override
    public <I> AggregateStorage<I> createAggregateStorage(
            Class<? extends Aggregate<I, ?, ?>> unused) {
        return new InMemoryAggregateStorage<>(isMultitenant());
    }

    @Override
    public <I> RecordStorage<I>
    createRecordStorage(Class<? extends Entity<I, ?>> entityClass) {
        EntityClass<?> modelClass = asEntityClass(entityClass);
        StorageSpec<I> spec = toStorageSpec(modelClass);
        return new InMemoryRecordStorage<>(spec, isMultitenant(), entityClass);
    }

    @Override
    public <I> ProjectionStorage<I> createProjectionStorage(
            Class<? extends Projection<I, ?, ?>> projectionClass) {
        EntityClass<?> modelClass = asProjectionClass(projectionClass);
        StorageSpec<I> spec = toStorageSpec(modelClass);
        InMemoryRecordStorage<I> recordStorage =
                new InMemoryRecordStorage<>(spec, isMultitenant(), projectionClass);
        return new InMemoryProjectionStorage<>(recordStorage);
    }

    /**
     * Obtains storage specification for the passed entity class.
     */
    private <I> StorageSpec<I> toStorageSpec(EntityClass<?> modelClass) {
        Class<? extends Message> stateClass = modelClass.stateClass();
        @SuppressWarnings("unchecked") // The cast is protected by generic parameters of the method.
        Class<I> idClass = (Class<I>) modelClass.idClass();
        TypeUrl stateUrl = TypeUrl.of(stateClass);
        StorageSpec<I> result = StorageSpec.of(boundedContextName, stateUrl, idClass);
        return result;
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public StorageFactory toSingleTenant() {
        if (!isMultitenant()) {
            return this;
        }
        return newInstance(boundedContextName, false);
    }

    @Override
    public StorageFactory copyFor(BoundedContextName name, boolean multitenant) {
        checkNotNull(name);
        return newInstance(name, multitenant);
    }
}
