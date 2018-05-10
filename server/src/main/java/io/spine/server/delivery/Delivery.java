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

import io.spine.annotation.Internal;
import io.spine.core.ActorMessageEnvelope;
import io.spine.server.entity.Entity;

/**
 * A strategy on delivering the messages received from the respective bus futher through
 * the entity repository down to to the instances of a certain entity type.
 *
 * <p>The delivery process is split into {@linkplain Sender sending} and {@linkplain Consumer
 * consuming}.
 *
 * <p>The way of sending and consuming the messages is a joint point for re-routing the messages
 * so that the entity instances are only modified in a single computational node at any moment.
 * Such a processing is based upon a
 * {@linkplain Shardable#getShardingStrategy() sharding strategy},
 * set for the destination repository.
 *
 * @param <I> the ID type of entity, to which the messages are being delivered
 * @param <E> the type of entity
 * @param <M> the type of message envelope, which is used for message delivery
 * @param <S> the type of the sharded stream that is used for the delivery
 * @param <B> the type of the builder for the sharded stream
 * @author Alex Tymchenko
 */
@Internal
public abstract class Delivery<I,
                               E extends Entity<I, ?>,
                               M extends ActorMessageEnvelope<?, ?, ?>,
                               S extends ShardedStream<I, ?, M>,
                               B extends ShardedStream.AbstractBuilder<I, M, B, S>> {


    private final Sender<I, M> sender;
    private final Consumer<I, E, M, S, B> consumer;

    protected Delivery(Consumer<I, E, M, S, B> consumer) {
        this.consumer = consumer;
        this.sender = new Sender<>(consumer.getTag());
    }

    public Sender<I, M> getSender() {
        return sender;
    }

    public Consumer<I, E, M, S, B> getConsumer() {
        return consumer;
    }
}
