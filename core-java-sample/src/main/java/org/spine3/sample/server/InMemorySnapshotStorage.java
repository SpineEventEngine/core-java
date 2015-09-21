/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.sample.server;

import org.spine3.server.aggregate.Snapshot;
import org.spine3.server.aggregate.SnapshotStorage;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

/**
 * In-memory-based implementation of the {@link Snapshot} repository.
 */
public class InMemorySnapshotStorage<I> implements SnapshotStorage<I> {

    private final Map<I, Snapshot> snapshots = newHashMap();


    @Override
    public void store(I aggregateId, Snapshot snapshot) {
        checkNotNull(aggregateId);
        checkNotNull(snapshot);
        snapshots.put(aggregateId, snapshot);
    }

    @Nullable
    @Override
    public Snapshot load(I aggregateId) {
        checkNotNull(aggregateId);
        final Snapshot snapshot = snapshots.get(aggregateId);
        return snapshot;
    }
}
