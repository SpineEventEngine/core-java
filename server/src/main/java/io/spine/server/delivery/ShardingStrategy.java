/*
 * Copyright 2018, TeamDev. All rights reserved.
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


import java.io.Serializable;
import java.util.Set;

/**
 * The strategy of splitting the message targets into shards.
 */
public interface ShardingStrategy extends Serializable {

    /**
     * Obtains the total number of shards.
     *
     * <p>The returned value is always greater than zero.
     *
     * @return the total number of shards.
     */
    int getNumberOfShards();

    /**
     * By the target identifier, obtains the index of the shard to which the target belongs.
     *
     * @param targetId the identifier of the target
     * @return the index of the shard, to which the specified target belongs
     */
    ShardIndex indexForTarget(Object targetId);

    /**
     * Obtains all possible indexes of existing shards.
     *
     * <p>The cardinality of the returned collection equals to the {@linkplain #getNumberOfShards()
     * total number of shards}.
     *
     * @return the set of all possible shard indexes
     */
    Set<ShardIndex> allIndexes();
}
