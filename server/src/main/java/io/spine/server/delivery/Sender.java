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

import io.spine.core.ActorMessageEnvelope;
import io.spine.server.ServerEnvironment;

import java.util.Set;

/**
 * The sending part of the {@linkplain Delivery}.
 *
 * <p>As long as the delivery process is sharded, the messages are posted to the assigned
 * {@linkplain ShardedStream sharded stream}.
 *
 * @author Alex Tymchenko
 */
public class Sender<I, M extends ActorMessageEnvelope<?, ?, ?>> {

    private final DeliveryTag<M> deliveryTag;

    public Sender(DeliveryTag<M> deliveryTag) {
        this.deliveryTag = deliveryTag;
    }

    /**
     * Send the given {@code message} to the consuming part of this delivery, where the target
     * with the specified {@code id} will be called to handle it.
     *
     * @param id      the identifier of the target entity
     * @param message the message to deliver to the target entity, packed into an envelope
     */
    public void send(I id, M message) {
        final Set<ShardedStream<I, ?, M>> streams = sharding().find(deliveryTag, id);

        for (ShardedStream<I, ?, M> shardedStream : streams) {
            shardedStream.post(id, message);
        }
    }

    /**
     * Obtains the sharding service instance for the current {@link ServerEnvironment server
     * environment}.
     *
     * <p>In order to allow switching to another sharding implementation at runtime and on-the-fly,
     * this API element is designed as method, not as a class-level field.
     *
     * @return the instance of sharding service
     */
    private static Sharding sharding() {
        final Sharding result = ServerEnvironment.getInstance()
                                                 .getSharding();
        return result;
    }
}
