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

import com.google.protobuf.Message;
import io.spine.core.RejectionEnvelope;

import java.util.List;
import java.util.Set;

/**
 * Dispatches rejections to aggregates of the associated {@link AggregateRepository}.
 *
 * @param <I> the type of aggregate IDs
 * @param <A> the type of aggregates
 * @author Alexander Yevsyukov
 */
class AggregateRejectionEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEndpoint<I, A, RejectionEnvelope, Set<I>> {

    private AggregateRejectionEndpoint(AggregateRepository<I, A> repository,
                                       RejectionEnvelope envelope) {
        super(repository, envelope);
    }

    static <I, A extends Aggregate<I, ?, ?>>
    Set<I> handle(AggregateRepository<I, A> repository, RejectionEnvelope rejection) {
        AggregateRejectionEndpoint<I, A> endpoint = of(repository, rejection);

        return endpoint.handle();
    }

    static <I, A extends Aggregate<I, ?, ?>>
    AggregateRejectionEndpoint<I, A>
    of(AggregateRepository<I, A> repository, RejectionEnvelope rejection) {
        return new AggregateRejectionEndpoint<>(repository, rejection);
    }

    @Override
    protected AggregateDelivery<I, A, RejectionEnvelope, ?, ?> getEndpointDelivery() {
        return repository().getRejectionEndpointDelivery();
    }

    @Override
    protected Set<I> getTargets() {
        RejectionEnvelope envelope = envelope();
        Set<I> ids =
                repository().getRejectionRouting()
                            .apply(envelope.getMessage(), envelope.getMessageContext());
        return ids;
    }

    @Override
    protected List<? extends Message> doDispatch(A aggregate, RejectionEnvelope envelope) {
        return aggregate.reactOn(envelope);
    }

    /**
     * Does nothing since a state of an aggregate should not be necessarily
     * updated upon reacting on a rejection.
     */
    @Override
    protected void onEmptyResult(A aggregate, RejectionEnvelope envelope) {
        // Do nothing.
    }

    @Override
    protected void onError(RejectionEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }
}
