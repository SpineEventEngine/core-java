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

package io.spine.server.delivery;

import io.spine.annotation.SPI;
import io.spine.server.NodeId;

import java.util.Optional;

/**
 * The registry of the shard indexes along with the identifiers of the nodes, which
 * process the messages corresponding to each index.
 *
 * @author Alex Tymchenko
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
     * @param nodeId
     *         the identifier of the node for which to pick the shard
     * @return the session of shard processing,
     *         or {@code Optional.empty()} if the shard is not available
     */
    Optional<ShardProcessingSession> pickUp(ShardIndex index, NodeId nodeId);
}
