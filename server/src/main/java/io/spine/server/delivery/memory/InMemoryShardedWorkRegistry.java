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

package io.spine.server.delivery.memory;

import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.NodeId;
import io.spine.server.delivery.ShardIndex;
import io.spine.server.delivery.ShardProcessingSession;
import io.spine.server.delivery.ShardSessionRecord;
import io.spine.server.delivery.ShardedWorkRegistry;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * An in-memory implementation of {@link ShardedWorkRegistry ShardedWorkRegistry}.
 */
public class InMemoryShardedWorkRegistry implements ShardedWorkRegistry {

    private final Map<ShardIndex, ShardSessionRecord> workByNode =
            Maps.newConcurrentMap();

    @Override
    public synchronized Optional<ShardProcessingSession> pickUp(ShardIndex index, NodeId nodeId) {
        if (workByNode.containsKey(index)) {
            ShardSessionRecord existingRecord = workByNode.get(index);
            if (existingRecord.hasPickedBy()) {
                return Optional.empty();
            } else {
                ShardSessionRecord updatedRecord = updatePickedBy(existingRecord, nodeId);
                return Optional.of(asSession(updatedRecord));
            }
        }
        ShardSessionRecord record =
                ShardSessionRecord
                        .newBuilder()
                        .setIndex(index)
                        .setPickedBy(nodeId)
                        .vBuild();
        workByNode.put(index, record);
        return Optional.of(asSession(record));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")      // `Builder` methods called in `if-else`.
    @CanIgnoreReturnValue
    private ShardSessionRecord updatePickedBy(ShardSessionRecord record,
                                              @Nullable NodeId nodeId) {
        ShardSessionRecord.Builder builder = record.toBuilder();
        if(nodeId == null) {
            builder.clearPickedBy();
        } else {
            builder.setPickedBy(nodeId);
        }
        ShardSessionRecord updatedRecord = builder.vBuild();
        workByNode.put(record.getIndex(), updatedRecord);
        return updatedRecord;
    }

    private ShardProcessingSession asSession(ShardSessionRecord record) {
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
            updatePickedBy(record, null);
        }
    }
}
