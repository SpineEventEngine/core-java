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

import io.spine.core.RejectionEnvelope;
import io.spine.server.delivery.DeliveryTag;
import io.spine.server.delivery.RejectionShardedStream;

/**
 * A strategy on delivering the rejections to the instances of a certain aggregate type.
 *
 * @param <I> the ID type of aggregate, to which rejections are being delivered
 * @param <A> the type of aggregate
 * @author Alex Tymchenko
 */
public class AggregateRejectionDelivery<I, A extends Aggregate<I, ?, ?>>
        extends AggregateDelivery<I,
                                  A,
                                  RejectionEnvelope,
                                  RejectionShardedStream<I>,
                                  RejectionShardedStream.Builder<I>> {

    protected AggregateRejectionDelivery(AggregateRepository<I, A> repository) {
        super(new AggregateRejectionConsumer<>(repository));
    }

    private static class AggregateRejectionConsumer<I, A extends Aggregate<I, ?, ?>>
            extends AggregateMessageConsumer<I,
                                             A,
                                             RejectionEnvelope,
                                             RejectionShardedStream<I>,
                                             RejectionShardedStream.Builder<I>> {

        private AggregateRejectionConsumer(AggregateRepository<I, A> repository) {
            super(DeliveryTag.forRejectionsOf(repository), repository);
        }

        @Override
        protected RejectionShardedStream.Builder<I> newShardedStreamBuilder() {
            return RejectionShardedStream.newBuilder();
        }

        @Override
        protected AggregateRejectionEndpoint<I, A> getEndpoint(RejectionEnvelope envelope) {
            return new AggregateRejectionEndpoint<>(repository(), envelope);
        }
    }
}
