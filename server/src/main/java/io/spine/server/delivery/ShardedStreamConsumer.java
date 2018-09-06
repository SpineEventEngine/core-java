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
import io.spine.core.BoundedContextName;
import io.spine.core.MessageEnvelope;
import io.spine.server.transport.TransportFactory;

import java.util.Set;

/**
 * The consumer of the sharded messages of a certain type.
 *
 * @param <I> the type of identifiers of the message targets
 * @param <E> the type of the envelope, in which the sharded messages are packed
 * @author Alex Tymchenko
 */
public interface ShardedStreamConsumer<I, E extends MessageEnvelope<?, ?, ?>> {

    /**
     * Defines the consumer tag.
     *
     * @return the tag value
     */
    DeliveryTag getTag();

    /**
     * Defines the processing of the message, sent to a particular destination (with the target ID
     * specified).
     *
     * @param targetId the ID of the target to handle the message
     * @param envelope the message to process packed into an envelope of a certain type
     */
    void onNext(I targetId, E envelope);

    /**
     * Binds the consumer to the transport and returns the created sharded stream with this consumer
     * as a subscriber.
     *
     * <p>Used internally in order to {@linkplain ShardingRegistry#register(ShardingStrategy, Set)
     * register} the consumer in the system.
     *
     * @param name             the name of the bounded context in scope of which the resulting
     *                         stream should act
     * @param key              the key of the shard for this consumer
     * @param transportFactory the transport factory which is used to create a low-level transport
     *                         for the stream
     * @return the sharded stream, down which the messages will travel to this consumer.
     */
    @Internal
    ShardedStream<I, ?, E> bindToTransport(BoundedContextName name,
                                           ShardingKey key,
                                           TransportFactory transportFactory);
}
