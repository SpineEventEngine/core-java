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

import com.google.protobuf.Message;
import io.spine.core.EventEnvelope;
import io.spine.core.React;

import java.util.List;
import java.util.Set;

/**
 * Dispatches events to aggregates of the associated {@code AggregateRepository}.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates
 *
 * @author Alexander Yevsyukov
 * @see React
 */
class AggregateEventEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends AggregateMessageEndpoint<I, A, EventEnvelope, Set<I>> {

    private AggregateEventEndpoint(AggregateRepository<I, A> repo, EventEnvelope event) {
        super(repo, event);
    }

    static <I, A extends Aggregate<I, ?, ?>>
    AggregateEventEndpoint<I, A> of(AggregateRepository<I, A> repository, EventEnvelope event) {
        return new AggregateEventEndpoint<>(repository, event);
    }

    static <I, A extends Aggregate<I, ?, ?>>
    Set<I> handle(AggregateRepository<I, A> repository, EventEnvelope event) {
        final AggregateEventEndpoint<I, A> endpoint = of(repository, event);

        return endpoint.handle();
    }

    @Override
    protected AggregateEndpointDelivery<I, A, EventEnvelope>
    getEndpointDelivery(EventEnvelope envelope) {
        return repository().getEventEndpointDelivery();
    }

    @Override
    protected List<? extends Message> doDispatch(A aggregate, EventEnvelope envelope) {
        return aggregate.reactOn(envelope);
    }

    @Override
    protected void onError(EventEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }

    /**
     * Does nothing since a state of an aggregate should not be necessarily
     * updated upon reacting on an event.
     */
    @Override
    protected void onEmptyResult(A aggregate, EventEnvelope envelope) {
        // Do nothing.
    }

    /**
     * Obtains IDs of aggregates that react on the event processed by this endpoint.
     */
    @Override
    protected Set<I> getTargets() {
        final EventEnvelope envelope = envelope();
        final Set<I> ids = repository().getEventRouting()
                                       .apply(envelope.getMessage(), envelope.getEventContext());
        return ids;
    }
}
