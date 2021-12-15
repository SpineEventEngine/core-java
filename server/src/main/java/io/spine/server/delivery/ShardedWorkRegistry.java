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

import com.google.protobuf.Duration;
import io.spine.annotation.SPI;
import io.spine.server.NodeId;

import java.util.Optional;

/**
 * The registry of the shard indexes along with the identifiers of the nodes, which
 * process the messages corresponding to each index.
 */
@SPI
public interface ShardedWorkRegistry {

    /**
     * Picks up the shard at a given index to process.
     *
     * <p>This action is intended to be exclusive, i.e. a single shard may be served
     * by a single application node at a given moment of time.
     *
     * <p>In case of a successful operation, an instance of {@link ShardProcessingSession}
     * is returned. The node obtained the session should perform the desired actions with the
     * sharded messages and then {@link ShardProcessingSession#complete() complete} the session.
     *
     * <p>In case the shard at a given index is already picked up by some node,
     * an {@link Optional#empty() Optional.empty()} is returned.
     *
     * @param index
     *         the index of the shard to pick up for processing
     * @param node
     *         the identifier of the node for which to pick the shard
     * @return the session of shard processing,
     *         or {@code Optional.empty()} if the shard is not available
     */
    Optional<ShardProcessingSession> pickUp(ShardIndex index, NodeId node);

    /**
     * Clears up the recorded {@code NodeId}s from the session records if there was no activity
     * for longer than passed {@code inactivityPeriod}.
     *
     * <p>It may be handy if an application node hangs or gets killed — so that it is not able
     * to complete the session in a conventional way.
     *
     * @param inactivityPeriod
     *         the duration of the period after which the session is considered expired
     * @return the indexes of shards which sessions have been released
     */
    Iterable<ShardIndex> releaseExpiredSessions(Duration inactivityPeriod);
}
