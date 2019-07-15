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

import com.google.protobuf.Timestamp;
import io.spine.server.aggregate.AggregateEventRecord;
import io.spine.server.aggregate.AggregateHistory;
import io.spine.server.aggregate.AggregateReadRequest;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.entity.LifecycleFlags;

import java.util.Iterator;
import java.util.Optional;

final class EmptyAggregateStorage<I>
        extends AggregateStorage<I>
        implements EmptyStorageWithLifecycleFlags<I, AggregateHistory, AggregateReadRequest<I>> {

    EmptyAggregateStorage(boolean multitenant) {
        super(multitenant);
    }

    @Override
    public Optional<AggregateHistory> read(AggregateReadRequest<I> request) {
        return Optional.empty();
    }

    @Override
    public void write(I id, AggregateHistory events) {
        // NOP.
    }

    @Override
    public void truncateOlderThan(int snapshotIndex) {
        // NOP.
    }

    @Override
    public void truncateOlderThan(int snapshotIndex, Timestamp date) {
        // NOP.
    }

    @Override
    protected void writeRecord(I id, AggregateEventRecord record) {
        // NOP.
    }

    @Override
    protected Iterator<AggregateEventRecord> historyBackward(AggregateReadRequest<I> request) {
        return iterator();
    }

    @Override
    protected void truncate(int snapshotIndex) {
        // NOP.
    }

    @Override
    protected void truncate(int snapshotIndex, Timestamp date) {
        // NOP.
    }

    @Override
    public Optional<LifecycleFlags> readLifecycleFlags(I id) {
        return Optional.empty();
    }

    @Override
    public void writeLifecycleFlags(I id, LifecycleFlags flags) {
        // NOP.
    }
}
