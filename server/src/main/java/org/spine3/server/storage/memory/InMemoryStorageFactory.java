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

import org.spine3.server.Entity;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.storage.*;

/**
 * A factory for in-memory storages.
 *
 * @author Alexander Yevsyukov
 */
public class InMemoryStorageFactory implements StorageFactory {

    @Override
    public CommandStorage createCommandStorage() {
        return new InMemoryCommandStorage();
    }

    @Override
    public EventStorage createEventStorage() {
        return new InMemoryEventStorage();
    }

    /**
     * NOTE: the parameter is unused.
     */
    @Override
    public <I> AggregateStorage<I> createAggregateStorage(Class<? extends Aggregate<I, ?>> unused) {
        return new InMemoryAggregateStorage<>();
    }

    /**
     * {@inheritDoc}
     *
     * NOTE: the parameter is unused.
     */
    @Override
    public <I> EntityStorage<I> createEntityStorage(Class<? extends Entity<I, ?>> unused) {
        return InMemoryEntityStorage.newInstance();
    }

    @Override
    public <I> ProjectionStorage<I> createProjectionStorage() {
        final InMemoryEntityStorage<I> entityStorage = InMemoryEntityStorage.<I>newInstance();
        return InMemoryProjectionStorage.newInstance(entityStorage);
    }

    @Override
    public void close() {
        // NOP
    }

    public static InMemoryStorageFactory getInstance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final InMemoryStorageFactory value = new InMemoryStorageFactory();
    }

    private InMemoryStorageFactory() {}
}
