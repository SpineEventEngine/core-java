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

package io.spine.server.aggregate;

import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.entity.EntityRecord;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Optional;

/**
 * An {@link AggregateStorage} which purpose is to intercept and remember
 * the parameter values of executed read operations.
 */
final class TestAggregateStorage extends AggregateStorage<ProjectId, Project> {

    private final AggregateStorage<ProjectId, ?> delegate;
    private ProjectId memoizedId;
    private int memoizedBatchSize;

    TestAggregateStorage(AggregateStorage<ProjectId, Project> delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public Optional<AggregateHistory> read(ProjectId id, int batchSize) {
        memoizedId = id;
        memoizedBatchSize = batchSize;
        return Optional.empty();
    }

    @Override
    public void enableMirror() {
        delegate.enableMirror();
    }

    @Override
    public void checkNotClosed() throws IllegalStateException {
        delegate.checkNotClosed();
    }

    @Override
    public Iterator<ProjectId> index() {
        return delegate.index();
    }

    @Override
    public Optional<AggregateHistory> read(ProjectId id) {
        return delegate.read(id);
    }

    @Override
    public void write(ProjectId id, AggregateHistory events) {
        delegate.write(id, events);
    }

    @Override
    public void writeEvent(ProjectId id, Event event) {
        delegate.writeEvent(id, event);
    }

    @Override
    public void writeSnapshot(ProjectId aggregateId, Snapshot snapshot) {
        delegate.writeSnapshot(aggregateId, snapshot);
    }

    @Override
    public void writeEventRecord(ProjectId id, AggregateEventRecord record) {
        delegate.writeEventRecord(id, record);
    }

    @Override
    public Iterator<EntityRecord> readStates(TargetFilters filters, ResponseFormat format) {
        return delegate.readStates(filters, format);
    }

    @Override
    public Iterator<EntityRecord> readStates(ResponseFormat format) {
        return delegate.readStates(format);
    }

    @Override
    public void writeState(Aggregate<ProjectId, ?, ?> aggregate) {
        delegate.writeState(aggregate);
    }

    @Override
    public Iterator<AggregateEventRecord> historyBackward(ProjectId id, int batchSize) {
        return delegate.historyBackward(id, batchSize);
    }

    @Override
    public Iterator<AggregateEventRecord> historyBackward(ProjectId id, int batchSize,
                                                          @Nullable Version startingFrom) {
        return delegate.historyBackward(id, batchSize, startingFrom);
    }

    @Override
    @Internal
    public void truncateOlderThan(int snapshotIndex) {
        delegate.truncateOlderThan(snapshotIndex);
    }

    @Override
    @Internal
    public void truncateOlderThan(int snapshotIndex, Timestamp date) {
        delegate.truncateOlderThan(snapshotIndex, date);
    }

    @Override
    public void truncate(int snapshotIndex) {
        delegate.truncate(snapshotIndex);
    }

    @Override
    public void truncate(int snapshotIndex, Timestamp date) {
        delegate.truncate(snapshotIndex, date);
    }

    ProjectId memoizedId() {
        return memoizedId;
    }

    int memoizedBatchSize() {
        return memoizedBatchSize;
    }
}
