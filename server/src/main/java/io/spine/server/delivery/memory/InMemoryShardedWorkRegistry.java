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

package io.spine.server.delivery.memory;

import com.google.protobuf.Duration;
import io.spine.server.NodeId;
import io.spine.server.delivery.AbstractWorkRegistry;
import io.spine.server.delivery.ShardIndex;
import io.spine.server.delivery.ShardProcessingSession;
import io.spine.server.delivery.ShardSessionRecord;
import io.spine.server.delivery.ShardedWorkRegistry;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Iterators.unmodifiableIterator;
import static com.google.common.collect.Maps.newConcurrentMap;

/**
 * An in-memory implementation of {@link ShardedWorkRegistry ShardedWorkRegistry}.
 *
 * @implNote This implementation synchronizes methods of {@code AbstractWorkRegistry} and
 *         uses a concurrent collection in order to guarantee thread safety.
 */
public final class InMemoryShardedWorkRegistry extends AbstractWorkRegistry {

    private final Map<ShardIndex, ShardSessionRecord> workByNode = newConcurrentMap();

    @Override
    public synchronized Optional<ShardProcessingSession> pickUp(ShardIndex index, NodeId nodeId) {
        return super.pickUp(index, nodeId);
    }

    @Override
    public synchronized Iterable<ShardIndex> releaseExpiredSessions(Duration inactivityPeriod) {
        return super.releaseExpiredSessions(inactivityPeriod);
    }

    @Override
    protected synchronized void clearNode(ShardSessionRecord session) {
        super.clearNode(session);
    }

    @Override
    protected Iterator<ShardSessionRecord> allRecords() {
        return unmodifiableIterator(workByNode.values().iterator());
    }

    @Override
    protected void write(ShardSessionRecord session) {
        workByNode.put(session.getIndex(), session);
    }

    @Override
    protected Optional<ShardSessionRecord> find(ShardIndex index) {
        return Optional.ofNullable(workByNode.get(index));
    }

    @Override
    protected ShardProcessingSession asSession(ShardSessionRecord record) {
        return new InMemoryShardSession(record);
    }

    /**
     * Implementation of shard processing session, based on in-memory storage mechanism.
     */
    public class InMemoryShardSession extends ShardProcessingSession {

        private InMemoryShardSession(ShardSessionRecord record) {
            super(record);
        }

        @Override
        protected void complete() {
            ShardSessionRecord record = workByNode.get(shardIndex());
            // Clear the node ID value and release the session.
            clearNode(record);
        }
    }
}
