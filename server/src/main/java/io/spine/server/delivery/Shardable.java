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

import io.spine.core.BoundedContextName;
import io.spine.server.ServerEnvironment;
import io.spine.server.entity.model.EntityClass;

/**
 * A contract for all message destinations, which require the messages sent to them
 * to be grouped and processed in shards.
 *
 * <p>Typically, the message destinations are repositories of entities, which dispatch the messages,
 * such as {@linkplain io.spine.core.Command Commands} and {@linkplain io.spine.core.Events Events},
 * to the entities.
 *
 * <p>In order to avoid concurrent modifications of the same entity in several processing nodes,
 * the entities are virtually grouped into shards by their identifiers. The items which belong
 * to the same shard, should be processed in a synchronous manner (e.g. a single thread)
 * to avoid such an issue.
 *
 * <p>Each {@code Shardable} may be serving as a destination for different kinds of messages.
 * A particular way to react to a message is {@linkplain #getMessageConsumers() defined} via
 * {@linkplain ShardedStreamConsumer consumers}, each consuming the messages of a particular kind.
 * I.e. {@linkplain io.spine.server.aggregate.AggregateRepository AggregateRepository} entities
 * handle {@code Commands} and may react to {@code Events} and {@code Rejections}. Therefore
 * the repository should define three consumers, one for each kind of messages.
 *
 * @author Alex Tymchenko
 */
public interface Shardable {

    /**
     * Defines the strategy of the sharding.
     *
     * <p>In particular, sets the rules on which message destination belongs to which shard.
     *
     * @return the strategy of sharding for this {@code Shardable}
     */
    ShardingStrategy getShardingStrategy();

    /**
     * Obtains all the message consumers for this {@code Shardable}.
     *
     * @return all the consumers
     */
    Iterable<ShardedStreamConsumer<?, ?>> getMessageConsumers();

    /**
     * Obtains the name of the {@linkplain io.spine.server.BoundedContext bounded context}, in scope
     * of which the current {@code Shardable} operates.
     *
     * @return the name of the bounded context
     */
    BoundedContextName getBoundedContextName();

    /**
     * Obtains the class of {@linkplain io.spine.server.entity.Entity Entity} which is served by
     * this shardable.
     *
     * @return the class of the {@code Entity}, which processing is being sharded
     * in this {@code Shardable}
     */
    EntityClass getShardedModelClass();

    /**
     * Registers this instance with {@link Sharding} obtained from the {@link ServerEnvironment}.
     */
    default void registerWithSharding() {
        ServerEnvironment.getInstance()
                         .getSharding()
                         .register(this);
    }

    /**
     * Unregisters this instance {@link Sharding} obtained from the {@link ServerEnvironment}.
     */
    default void unregisterWithSharding() {
        ServerEnvironment.getInstance()
                         .getSharding()
                         .unregister(this);
    }
}
