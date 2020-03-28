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

import com.google.common.annotations.VisibleForTesting;
import io.spine.server.ContextSpec;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.storage.StorageFactory;

/**
 * In-memory storage for aggregate events and snapshots.
 *
 * @param <I>
 *         the type of IDs of aggregates managed by this storage
 */
final class InMemoryAggregateStorage<I> extends AggregateStorage<I> {

    public InMemoryAggregateStorage(ContextSpec spec,
                                    Class<? extends Aggregate<I, ?, ?>> aggregateClass,
                                    StorageFactory factory) {
        super(spec, aggregateClass, factory);
    }
//
//    private final MultitenantStorage<TenantAggregateRecords<I>> multitenantStorage;
//
//    InMemoryAggregateStorage(ContextSpec spec,
//                             Class<? extends Aggregate<I, ?, ?>> aggregateClass,
//                             StorageFactory factory) {
//        super(spec, aggregateClass, factory);
//        this.multitenantStorage =
//                new MultitenantStorage<TenantAggregateRecords<I>>(spec.isMultitenant()) {
//                    @Override
//                    TenantAggregateRecords<I> createSlice() {
//                        return new TenantAggregateRecords<>();
//                    }
//                };
//    }
//
    /** Creates a new single-tenant storage instance. */
    @VisibleForTesting
    static <I> InMemoryAggregateStorage<I> newInstance(Class<? extends Aggregate<I, ?, ?>> cls) {
        ContextSpec spec = ContextSpec.singleTenant("VisibleForTesting");
        StorageFactory factory = ServerEnvironment.instance()
                                                  .storageFactory();
        return new InMemoryAggregateStorage<>(spec, cls, factory);
    }
//
//    private TenantAggregateRecords<I> getStorage() {
//        return multitenantStorage.currentSlice();
//    }
//
//    @Override
//    protected Iterator<I> distinctAggregateIds() {
//        return getStorage().index();
//    }
//
//    @Override
//    public Optional<LifecycleFlags> readLifecycleFlags(I id) {
//        checkNotClosed();
//        Optional<LifecycleFlags> result = getStorage().getStatus(id);
//        return result;
//    }
//
//    @Override
//    public void writeLifecycleFlags(I id, LifecycleFlags status) {
//        checkNotClosed();
//        getStorage().putStatus(id, status);
//    }
//
//    @Override
//    protected void writeRecord(I id, AggregateEventRecord record) {
//        getStorage().put(id, record);
//    }
//
//    @Override
//    protected void truncate(int snapshotIndex) {
//        getStorage().truncateOlderThan(snapshotIndex);
//    }
//
//    @Override
//    protected void truncate(int snapshotIndex, Timestamp date) {
//        getStorage().truncateOlderThan(snapshotIndex, date);
//    }
}
