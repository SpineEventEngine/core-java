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

package io.spine.server.storage.nop;

import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.delivery.InboxStorage;
import io.spine.server.entity.Entity;
import io.spine.server.event.EventStore;
import io.spine.server.projection.Projection;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryInboxStorage;

import static java.lang.String.format;

final class EmptyStorageFactory implements StorageFactory {

    @Override
    public <I> EmptyAggregateStorage<I>
    createAggregateStorage(ContextSpec context,
                           Class<? extends Aggregate<I, ?, ?>> aggregateClass) {
        return new EmptyAggregateStorage<>(context.isMultitenant());
    }

    @Override
    public <I> EmptyRecordStorage<I>
    createRecordStorage(ContextSpec context, Class<? extends Entity<I, ?>> entityClass) {
        return new EmptyRecordStorage<>(context.isMultitenant());
    }

    @Override
    public <I> EmptyProjectionStorage<I>
    createProjectionStorage(ContextSpec context,
                            Class<? extends Projection<I, ?, ?>> projectionClass) {
        EmptyRecordStorage<I> recordStorage = createRecordStorage(context, projectionClass);
        return new EmptyProjectionStorage<>(recordStorage);
    }

    @Override
    public InboxStorage createInboxStorage(boolean multitenant) {
        String message = format("Attempting to create an empty Inbox storage. Use `%s` instead.",
                                InMemoryInboxStorage.class.getName());
        throw new UnsupportedOperationException(message);
    }

    @Override
    public EventStore createEventStore(ContextSpec context) {
        return new EmptyEventStore();
    }

    @Override
    public void close() {
        // NOP.
    }
}
