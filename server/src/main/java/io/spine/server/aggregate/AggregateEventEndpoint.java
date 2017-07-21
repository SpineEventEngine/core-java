/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.TenantAwareOperation;

import java.util.List;
import java.util.Set;

/**
 * Dispatches events to aggregates of the associated {@code AggregateRepository}.
 *
 * <p>Loading and storing an aggregate is a tenant-sensitive operation,
 * which depends on the tenant ID of the command we dispatch.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates
 *
 * @author Alexander Yevsyukov
 * @see React
 */
class AggregateEventEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends AggregateMessageEndpoint<I, A, EventEnvelope>{

    private AggregateEventEndpoint(AggregateRepository<I, A> repo, EventEnvelope envelope) {
        super(repo, envelope);
    }

    static <I, A extends Aggregate<I, ?, ?>>
    void handle(AggregateRepository<I, A> repository, EventEnvelope envelope) {
        final AggregateEventEndpoint<I, A> endpoint =
                new AggregateEventEndpoint<>(repository, envelope);

        final TenantAwareOperation operation = endpoint.createOperation();
        operation.execute();
    }

    @Override
    TenantAwareOperation createOperation() {
        return new Operation(envelope().getOuterObject());
    }

    @Override
    List<? extends Message> dispatchEnvelope(A aggregate, EventEnvelope envelope) {
        return aggregate.dispatchEvent(envelope);
    }

    /**
     * Does nothing since an aggregate is not required to produce events in reaction to an event.
     */
    @Override
    void onEmptyResult(A aggregate, EventEnvelope envelope) {
        // Do nothing.
    }

    /**
     * Obtains IDs of aggregates that react on the event processed by this endpoint.
     */
    @Override
    Set<I> getTargets() {
        final EventEnvelope env = envelope();
        return repository().getEventTargets(env.getMessage(), env.getEventContext());
    }

    /**
     * The operation executed under the event's tenant.
     */
    private class Operation extends EventOperation {

        private Operation(Event event) {
            super(event);
        }

        @Override
        public void run() {
            dispatch();
        }
    }
}
