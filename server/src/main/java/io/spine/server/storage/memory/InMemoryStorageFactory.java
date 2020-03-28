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

import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.server.ContextSpec;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.Columns;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;

import static io.spine.server.entity.model.EntityClass.asEntityClass;

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

    @Override
    public <I> RecordStorage<I>
    createRecordStorage(ContextSpec context, Class<? extends Entity<I, ?>> entityClass) {
        EntityClass<?> modelClass = asEntityClass(entityClass);
        StorageSpec<I> storageSpec = toStorageSpec(context, modelClass);
        return new InMemoryRecordStorage<>(storageSpec, entityClass, context.isMultitenant());
    }

    @Override
    public <I, M extends Message> MessageStorage<I, M>
    createMessageStorage(Columns<M> columns, boolean multitenant) {
        return new InMemoryMessageStorage<>(columns, multitenant);
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
