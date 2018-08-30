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

import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.event.React;

import java.util.List;
import java.util.Set;

/**
 * Dispatches an event to aggregates of the associated {@code AggregateRepository}.
 *
 * @author Alexander Yevsyukov
 * @see React
 */
class AggregateEventReactionEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEventEndpoint<I, A> {

    AggregateEventReactionEndpoint(AggregateRepository<I, A> repo, EventEnvelope event) {
        super(repo, event);
    }

    @Override
    protected AggregateDelivery<I, A, EventEnvelope, ?, ?> getEndpointDelivery() {
        return repository().getEventEndpointDelivery();
    }

    @Override
    protected List<Event> doDispatch(A aggregate, EventEnvelope envelope) {
        repository().onDispatchEvent(aggregate.getId(), envelope.getOuterObject());
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
        EventEnvelope envelope = envelope();
        Set<I> ids = repository().getEventRouting()
                                 .apply(envelope.getMessage(), envelope.getEventContext());
        return ids;
    }
}
