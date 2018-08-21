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
import io.spine.core.EventEnvelope;
import io.spine.server.delivery.DeliveryTag;
import io.spine.server.delivery.EventShardedStream;

/**
 * A strategy on delivering the events to the instances of a certain aggregate type.
 *
 * @param <I> the ID type of aggregate, to which events are being delivered
 * @param <A> the type of aggregate
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess") // is public for customizable delivery mechanisms
public class AggregateEventDelivery<I, A extends Aggregate<I, ?, ?>>
        extends AggregateDelivery<I,
                                  A,
                                  EventEnvelope,
                                  EventShardedStream<I>,
                                  EventShardedStream.Builder<I>> {

    protected AggregateEventDelivery(AggregateRepository<I, A> repository) {
        super(new AggregateEventConsumer<>(repository));
    }

    private static class AggregateEventConsumer<I, A extends Aggregate<I, ?, ?>>
            extends AggregateMessageConsumer<I,
                                             A,
                                             EventEnvelope,
                                             EventShardedStream<I>,
                                             EventShardedStream.Builder<I>> {

        protected AggregateEventConsumer(AggregateRepository<I, A> repository) {
            super(DeliveryTag.forEventsOf(repository), repository);
        }

        @Override
        protected EventShardedStream.Builder<I> newShardedStreamBuilder() {
            return EventShardedStream.newBuilder();
        }

        @Override
        protected AggregateEventEndpoint<I, A> getEndpoint(EventEnvelope envelope) {
            return new AggregateEventEndpoint<>(repository(), envelope);
        }
    }
}
