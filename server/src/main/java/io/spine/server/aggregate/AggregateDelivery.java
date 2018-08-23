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
package io.spine.server.aggregate;

import io.spine.annotation.SPI;
import io.spine.core.ActorMessageEnvelope;
import io.spine.server.delivery.Consumer;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.DeliveryTag;
import io.spine.server.delivery.ShardedStream;
import io.spine.server.entity.Repository;

/**
 * A strategy on delivering the messages to the instances of a certain aggregate type.
 *
 * <p>Spine users may want to extend this class or any of its framework-provided implementations
 * in order to switch to another delivery mechanism.
 *
 * @param <I> the ID type of aggregate, to which messages are being delivered
 * @param <A> the type of aggregate
 * @param <E> the type of message envelope, which is used for message delivery
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess") // is public for customizable delivery mechanisms
public abstract class AggregateDelivery<I,
                                        A extends Aggregate<I, ?, ?>,
                                        E extends ActorMessageEnvelope<?, ?, ?>,
                                        S extends ShardedStream<I, ?, E>,
                                        B extends ShardedStream.AbstractBuilder<I, E, B, S>>
        extends Delivery<I, A, E, S, B> {

    AggregateDelivery(AggregateMessageConsumer<I, A, E, S, B> consumer) {
        super(consumer);
    }

    protected abstract static
    class AggregateMessageConsumer<I,
                                   A extends Aggregate<I, ?, ?>,
                                   E extends ActorMessageEnvelope<?, ?, ?>,
                                   S extends ShardedStream<I, ?, E>,
                                   B extends ShardedStream.AbstractBuilder<I, E, B, S>>
            extends Consumer<I, A, E, S, B> {

        protected AggregateMessageConsumer(DeliveryTag<E> tag, Repository<I, A> repository) {
            super(tag, repository);
        }

        @Override
        protected abstract AggregateEndpoint<I, A, E, ?> getEndpoint(E messageEnvelope);

        @Override
        protected void passToEndpoint(I id, E envelopeMessage) {
            getEndpoint(envelopeMessage).deliverNowTo(id);
        }

        @Override
        protected AggregateRepository<I, A> repository() {
            return (AggregateRepository<I, A>) super.repository();
        }
    }
}
