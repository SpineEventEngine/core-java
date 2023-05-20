/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.Timestamp;
import io.spine.server.aggregate.AggregateEventRecord;
import io.spine.server.aggregate.AggregateReadRequest;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.entity.LifecycleFlags;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory storage for aggregate events and snapshots.
 *
 * @param <I>
 *         the type of IDs of aggregates managed by this storage
 *
 */
final class InMemoryAggregateStorage<I> extends AggregateStorage<I> {

    private final MultitenantStorage<TenantAggregateRecords<I>> multitenantStorage;

    InMemoryAggregateStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantAggregateRecords<I>>(multitenant) {
            @Override
            TenantAggregateRecords<I> createSlice() {
                return new TenantAggregateRecords<>();
            }
        };
    }

    /** Creates a new single-tenant storage instance. */
    static <I> InMemoryAggregateStorage<I> newInstance() {
        return new InMemoryAggregateStorage<>(false);
    }

    private TenantAggregateRecords<I> getStorage() {
        return multitenantStorage.currentSlice();
    }

    @Override
    protected Iterator<I> distinctAggregateIds() {
        return getStorage().index();
    }

    @Override
    public Optional<LifecycleFlags> readLifecycleFlags(I id) {
        checkNotClosed();
        Optional<LifecycleFlags> result = getStorage().getStatus(id);
        return result;
    }

    @Override
    public void writeLifecycleFlags(I id, LifecycleFlags status) {
        checkNotClosed();
        getStorage().putStatus(id, status);
    }

    @Override
    protected void writeRecord(I id, AggregateEventRecord record) {
        getStorage().put(id, record);
    }

    @Override
    protected Iterator<AggregateEventRecord> historyBackward(AggregateReadRequest<I> request) {
        checkNotNull(request);
        List<AggregateEventRecord> records = getStorage().historyBackward(request);
        return records.iterator();
    }

    @Override
    protected void truncate(int snapshotIndex) {
        getStorage().truncateOlderThan(snapshotIndex);
    }

    @Override
    protected void truncate(int snapshotIndex, Timestamp date) {
        getStorage().truncateOlderThan(snapshotIndex, date);
    }
}
