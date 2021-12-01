/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import io.spine.annotation.SPI;
import io.spine.server.NodeId;

import java.util.Iterator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.between;
import static io.spine.base.Time.currentTime;

/**
 * An implementation base for {@link ShardedWorkRegistry ShardedWorkRegistries} based on a specific
 * persistence mechanism.
 *
 * @implNote This class is NOT thread safe. Synchronize the atomic persistence operations
 *         as well as the methods implemented in this class make an implementation thread safe.
 */
@SPI
public abstract class AbstractWorkRegistry implements ShardedWorkRegistry {

    @Override
    public Optional<ShardProcessingSession> pickUp(ShardIndex index, NodeId nodeId) {
        checkNotNull(index);
        checkNotNull(nodeId);

        var record = find(index);
        if (record.isPresent()) {
            var existingRecord = record.get();
            if (hasPickedBy(existingRecord)) {
                return Optional.empty();
            } else {
                var updatedRecord = updateNode(existingRecord, nodeId);
                return Optional.of(asSession(updatedRecord));
            }
        } else {
            var newRecord = createRecord(index, nodeId);
            return Optional.of(asSession(newRecord));
        }
    }

    private static boolean hasPickedBy(ShardSessionRecord record) {
        return !NodeId.getDefaultInstance().equals(record.getPickedBy());
    }

    private ShardSessionRecord createRecord(ShardIndex index, NodeId nodeId) {
        var newRecord = ShardSessionRecord.newBuilder()
                .setIndex(index)
                .setPickedBy(nodeId)
                .setWhenLastPicked(currentTime())
                .vBuild();
        write(newRecord);
        return newRecord;
    }

    private ShardSessionRecord updateNode(ShardSessionRecord record, NodeId nodeId) {
        var updatedRecord = record.toBuilder()
                .setPickedBy(nodeId)
                .setWhenLastPicked(currentTime())
                .build();
        write(updatedRecord);
        return updatedRecord;
    }

    @Override
    public Iterable<ShardIndex> releaseExpiredSessions(Duration inactivityPeriod) {
        checkNotNull(inactivityPeriod);
        ImmutableSet.Builder<ShardIndex> resultBuilder = ImmutableSet.builder();
        allRecords().forEachRemaining(record -> {
            if (record.hasPickedBy()) {
                var whenPicked = record.getWhenLastPicked();
                var elapsed = between(whenPicked, currentTime());

                var comparison = Durations.compare(elapsed, inactivityPeriod);
                if (comparison >= 0) {
                    clearNode(record);
                    resultBuilder.add(record.getIndex());
                }
            }
        });
        return resultBuilder.build();
    }

    /**
     * Clears the value of {@code ShardSessionRecord.when_last_picked} and stores the session.
     */
    protected void clearNode(ShardSessionRecord session) {
        var record = session.toBuilder()
                .clearPickedBy()
                .build();
        write(record);
    }

    /**
     * Obtains all the session records associated with this registry.
     */
    protected abstract Iterator<ShardSessionRecord> allRecords();

    /**
     * Stores the given session.
     *
     * <p>The session may or may be not present in the registry already. After calling this method,
     * the given session must be reachable via {@link #find(ShardIndex)} and {@link #allRecords()}.
     */
    protected abstract void write(ShardSessionRecord session);

    /**
     * Looks for the session record by the given shard index.
     *
     * @param index
     *         shard index to find a session for
     * @return a session record or {@code Optional.empty()} if the record is not present in
     *         the registry
     */
    protected abstract Optional<ShardSessionRecord> find(ShardIndex index);

    /**
     * Restores a {@link ShardProcessingSession} from the given session record.
     */
    protected abstract ShardProcessingSession asSession(ShardSessionRecord record);
}
