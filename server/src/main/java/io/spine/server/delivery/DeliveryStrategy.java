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

/**
 * Determines the {@linkplain ShardIndex index of a shard} for the given identifier of an entity.
 *
 * <p>The idea is to avoid the concurrent modification of the same {@code Entity} instance
 * on several app nodes. Therefore an entity is put into a shard, which in turn is designed to
 * process all the shard-incoming messages on a single application node at a time.
 */
public interface DeliveryStrategy {

    /**
     * Determines the shard index for the messages heading to the entity with the specified target
     * identifier.
     *
     * @param entityId
     *         the identifier of the entity, to which the messages are dispatched.
     * @return the shard index
     */
    ShardIndex getIndexFor(Object entityId);

    /**
     * Tells how many shards there are according to this strategy.
     *
     * @return total count of shards
     */
    int getShardCount();
}
