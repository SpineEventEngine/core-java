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

package io.spine.server.aggregate;

import com.google.protobuf.Timestamp;
import io.spine.server.entity.LifecycleFlags;
import io.spine.test.aggregate.ProjectId;

import java.util.Iterator;
import java.util.Optional;

/**
 * An {@link AggregateStorage} whose purpose is to intercept the incoming
 * {@linkplain AggregateReadRequest read request}.
 */
final class TestAggregateStorage extends AggregateStorage<ProjectId> {

    private final AggregateStorage<ProjectId> delegate;
    private AggregateReadRequest<ProjectId> memoizedRequest;

    TestAggregateStorage(AggregateStorage<ProjectId> delegate) {
        super(delegate.isMultitenant());
        this.delegate = delegate;
    }

    @Override
    public Optional<AggregateHistory> read(AggregateReadRequest<ProjectId> request) {
        memoizedRequest = request;
        return Optional.empty();
    }

    @Override
    protected void writeRecord(ProjectId id, AggregateEventRecord record) {
        delegate.writeRecord(id, record);
    }

    @Override
    protected Iterator<AggregateEventRecord>
    historyBackward(AggregateReadRequest<ProjectId> request) {
        return delegate.historyBackward(request);
    }

    @Override
    protected void truncate(int snapshotIndex) {
        delegate.truncate(snapshotIndex);
    }

    @Override
    protected void truncate(int snapshotIndex, Timestamp date) {
        delegate.truncate(snapshotIndex, date);
    }

    @Override
    protected Iterator<ProjectId> distinctAggregateIds() {
        return delegate.distinctAggregateIds();
    }

    @Override
    public Optional<LifecycleFlags> readLifecycleFlags(ProjectId id) {
        return delegate.readLifecycleFlags(id);
    }

    @Override
    public void writeLifecycleFlags(ProjectId id, LifecycleFlags flags) {
        delegate.writeLifecycleFlags(id, flags);
    }

    AggregateReadRequest<ProjectId> memoizedRequest() {
        return memoizedRequest;
    }
}
