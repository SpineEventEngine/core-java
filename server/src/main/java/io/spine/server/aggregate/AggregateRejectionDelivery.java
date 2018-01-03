/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

/**
 * A strategy on delivering the rejections to the instances of a certain aggregate type.
 *
 * @param <I> the ID type of aggregate, to which rejections are being delivered
 * @param <A> the type of aggregate
 * @author Alex Tymchenko
 */
public abstract class AggregateRejectionDelivery<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEndpointDelivery<I, A, RejectionEnvelope> {

    protected AggregateRejectionDelivery(AggregateRepository<I, A> repository) {
        super(repository);
    }

    @Override
    protected AggregateMessageEndpoint<I, A, RejectionEnvelope, ?> getEndpoint(
            RejectionEnvelope envelope) {
        return AggregateRejectionEndpoint.of(repository(), envelope);
    }

    public static <I, A extends Aggregate<I, ?, ?>>
    AggregateRejectionDelivery<I, A> directDelivery(AggregateRepository<I, A> repository) {
        return new Direct<>(repository);
    }

    /**
     * Direct delivery which does not postpone dispatching.
     *
     * @param <I> the type of aggregate IDs
     * @param <A> the type of aggregate
     */
    public static class Direct<I, A extends Aggregate<I, ?, ?>>
            extends AggregateRejectionDelivery<I, A> {

        private Direct(AggregateRepository<I, A> repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(I id, RejectionEnvelope envelope) {
            return false;
        }
    }
}
