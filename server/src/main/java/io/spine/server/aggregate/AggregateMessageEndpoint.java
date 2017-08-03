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
import io.spine.core.ActorMessageEnvelope;
import io.spine.core.Event;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.LifecycleFlags;

import java.util.List;

/**
 * Abstract base for endpoints handling messages sent to aggregates.
 *
 * @param <I> the type of aggregate IDs
 * @param <A> the type of aggregates
 * @param <M> the type of message envelopes
 * @param <R> the type of the dispatch result, can be {@code <I>} for unicast dispatching, or
 *            {@code Set<I>} for multicast
 * @author Alexander Yevsyukov
 */
abstract class AggregateMessageEndpoint<I,
                                        A extends Aggregate<I, ?, ?>,
                                        M extends ActorMessageEnvelope<?, ?>, R>
        extends EntityMessageEndpoint<I, A, M, R> {
    
    private final AggregateRepository<I, A> repository;

    AggregateMessageEndpoint(AggregateRepository<I, A> repository, M envelope) {
        super(repository, envelope);
        this.repository = repository;
    }

    /**
     * Dispatched the message to the aggregate with the passed ID.
     *
     * @param aggregateId the ID of the aggreagate to which dispatch the message
     */
    @Override
    protected void dispatchToOne(I aggregateId) {
        final A aggregate = repository().loadOrCreate(aggregateId);

        final LifecycleFlags statusBefore = aggregate.getLifecycleFlags();

        final List<? extends Message> eventMessages = dispatchEnvelope(aggregate, envelope());

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);

        aggregate.apply(eventMessages, envelope());

        tx.commit();

        // Update status only if the message was handled successfully.
        final LifecycleFlags statusAfter = aggregate.getLifecycleFlags();
        if (statusAfter != null && !statusBefore.equals(statusAfter)) {
            storage().writeLifecycleFlags(aggregateId, statusAfter);
        }

        store(aggregate);
    }

    /**
     * Stores the aggregate if it has uncommitted events.
     *
     * @param aggregate the aggregate to store
     */
    private void store(A aggregate) {
        final List<Event> events = aggregate.getUncommittedEvents();
        if (!events.isEmpty()) {
            repository.onModifiedAggregate(envelope().getTenantId(), aggregate);
        } else {
            onEmptyResult(aggregate, envelope());
        }
    }

    @Override
    protected AggregateRepository<I, A> repository() {
        return (AggregateRepository<I, A>)super.repository();
    }

    private AggregateStorage<I> storage() {
        return repository.aggregateStorage();
    }
}
