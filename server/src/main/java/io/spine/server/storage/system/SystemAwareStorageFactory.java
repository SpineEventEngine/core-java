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

package io.spine.server.storage.system;

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.delivery.InboxStorage;
import io.spine.server.entity.Entity;
import io.spine.server.event.EmptyEventStore;
import io.spine.server.event.EventStore;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Internal
public final class SystemAwareStorageFactory implements StorageFactory {

    private final StorageFactory delegate;

    private SystemAwareStorageFactory(StorageFactory delegate) {
        this.delegate = delegate;
    }

    public static SystemAwareStorageFactory wrap(StorageFactory factory) {
        checkNotNull(factory);
        return factory instanceof SystemAwareStorageFactory
               ? (SystemAwareStorageFactory) factory
               : new SystemAwareStorageFactory(factory);
    }

    @VisibleForTesting
    public StorageFactory delegate() {
        return delegate;
    }

    @Override
    public <I> AggregateStorage<I>
    createAggregateStorage(ContextSpec context,
                           Class<? extends Aggregate<I, ?, ?>> aggregateClass) {
        return delegate.createAggregateStorage(context, aggregateClass);
    }

    @Override
    public <I> RecordStorage<I>
    createRecordStorage(ContextSpec context, Class<? extends Entity<I, ?>> entityClass) {
        return delegate.createRecordStorage(context, entityClass);
    }

    @Override
    public <I> ProjectionStorage<I>
    createProjectionStorage(ContextSpec context,
                            Class<? extends Projection<I, ?, ?>> projectionClass) {
        return delegate.createProjectionStorage(context, projectionClass);
    }

    @Override
    public InboxStorage createInboxStorage(boolean multitenant) {
        return delegate.createInboxStorage(multitenant);
    }

    @Override
    public EventStore createEventStore(ContextSpec context) {
        return context.storesEvents()
               ? delegate.createEventStore(context)
               : new EmptyEventStore();
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
