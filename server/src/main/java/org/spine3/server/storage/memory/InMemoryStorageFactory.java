/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.entity.Entity;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.CommandStorage;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.ProjectionStorage;
import org.spine3.server.storage.StorageFactory;

/**
 * A factory for in-memory storages.
 *
 * @author Alexander Yevsyukov
 */
public class InMemoryStorageFactory implements StorageFactory {

    private final boolean multitenant;

    private InMemoryStorageFactory(boolean multitenant) {
        this.multitenant = multitenant;
    }

    @Override
    public boolean isMultitenant() {
        return this.multitenant;
    }

    @Override
    public CommandStorage createCommandStorage() {
        return new InMemoryCommandStorage(isMultitenant());
    }

    @Override
    public EventStorage createEventStorage() {
        return new InMemoryEventStorage(isMultitenant());
    }

    /**
     * NOTE: the parameter is unused.
     */
    @Override
    public <I> AggregateStorage<I> createAggregateStorage(Class<? extends Aggregate<I, ?, ?>> unused) {
        return new InMemoryAggregateStorage<>(isMultitenant());
    }

    /**
     * {@inheritDoc}
     *
     * NOTE: the parameter is unused.
     */
    @Override
    public <I> EntityStorage<I> createEntityStorage(Class<? extends Entity<I, ?>> unused) {
        return InMemoryEntityStorage.newInstance(isMultitenant());
    }

    @Override
    public <I> ProjectionStorage<I> createProjectionStorage(Class<? extends Entity<I, ?>> unused) {
        final boolean multitenant = isMultitenant();
        final InMemoryEntityStorage<I> entityStorage = InMemoryEntityStorage.newInstance(multitenant);
        return InMemoryProjectionStorage.newInstance(entityStorage, multitenant);
    }

    @Override
    public void close() {
        // NOP
    }

    public static InMemoryStorageFactory getInstance() {
        return Singleton.INSTANCE.singleTenantInstance;
    }

    public static InMemoryStorageFactory getMultitenantInstance() {
        return Singleton.INSTANCE.multitenantInstance;
    }

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private enum Singleton {
        INSTANCE;
        private final InMemoryStorageFactory singleTenantInstance = new InMemoryStorageFactory(false);
        private final InMemoryStorageFactory multitenantInstance = new InMemoryStorageFactory(true);
    }
}
