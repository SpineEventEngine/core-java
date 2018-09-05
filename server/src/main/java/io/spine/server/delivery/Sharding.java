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

import io.spine.annotation.SPI;
import io.spine.core.MessageEnvelope;

import java.util.Set;

/**
 * The service responsible for orchestrating the shardables and their inbound streams of messages.
 *
 * <p>Typically is a JVM-wide singleton.
 *
 * @author Alex Tymchenko
 */
public interface Sharding {

    /**
     * Registers a shardable instance.
     *
     * <p>After the registration the shardable starts to be served with the messages targeting the
     * instances which belong to its shard.
     *
     * @param shardable a shardable to register
     */
    void register(Shardable shardable);

    /**
     * Unregister a shardable instance.
     *
     * <p>After this operation, the messages are no longer dispatched to the shards of this
     * shardable.
     *
     * <p>Has no effect if there is no such a shardable registered.
     *
     * @param shardable a shardable instance to unregister
     */
    void unregister(Shardable shardable);

    /**
     * Defines the sharding keys for the given shardable to be served within this
     * {@code Sharding instance}.
     *
     * @param shardable the shardable to define the keys for
     * @param keys all the sharding keys to pick those to handle within this instance
     * @return the set of keys to serve within this sharding instance
     */
    @SPI
    Set<ShardingKey> pickKeysForNode(Shardable shardable, Set<ShardingKey> keys);

    /**
     * Finds the message streams which delivery tag and target entity identifier
     * match the specified values.
     *
     * @param tag the delivery tag
     * @param targetId the identifier of the target entity
     * @param <I> the type of target entity identifiers
     * @param <E> the type of message envelope to find the message stream for
     * @return the message streams matching the search criteria
     */
    <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
    find(DeliveryTag tag, I targetId);
}
