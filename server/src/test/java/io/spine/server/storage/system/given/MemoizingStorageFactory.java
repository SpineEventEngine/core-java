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

package io.spine.server.storage.system.given;

import com.google.common.collect.ImmutableList;
import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.delivery.InboxStorage;
import io.spine.server.entity.Entity;
import io.spine.server.event.EventStore;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;

import java.util.ArrayList;
import java.util.List;

import static io.spine.testing.Tests.nullRef;

/**
 * A test-only {@link StorageFactory} which always returns {@code null}s instead of storages and
 * memoizes the requested storage types.
 */
public final class MemoizingStorageFactory implements StorageFactory {

    private final List<Class<?>> requestedStorages = new ArrayList<>();
    private boolean requestedInbox = false;
    private boolean requestedEventStore = false;
    private boolean closed = false;

    @Override
    public <I> AggregateStorage<I>
    createAggregateStorage(ContextSpec context,
                           Class<? extends Aggregate<I, ?, ?>> aggregateClass) {
        requestedStorages.add(aggregateClass);
        return nullRef();
    }

    @Override
    public <I> RecordStorage<I>
    createRecordStorage(ContextSpec context, Class<? extends Entity<I, ?>> entityClass) {
        requestedStorages.add(entityClass);
        return nullRef();
    }

    @Override
    public <I> ProjectionStorage<I>
    createProjectionStorage(ContextSpec context,
                            Class<? extends Projection<I, ?, ?>> projectionClass) {
        requestedStorages.add(projectionClass);
        return nullRef();
    }

    @Override
    public InboxStorage createInboxStorage(boolean multitenant) {
        requestedInbox = true;
        return nullRef();
    }

    @Override
    public EventStore createEventStore(ContextSpec context) {
        requestedEventStore = true;
        return nullRef();
    }

    @Override
    public void close() {
        closed = true;
    }

    public ImmutableList<Class<?>> requestedStorages() {
        return ImmutableList.copyOf(requestedStorages);
    }

    public boolean requestedInbox() {
        return requestedInbox;
    }

    public boolean requestedEventStore() {
        return requestedEventStore;
    }

    public boolean isClosed() {
        return closed;
    }
}
