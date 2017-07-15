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
import io.spine.core.MessageEnvelope;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.tenant.TenantAwareOperation;

import java.util.List;
import java.util.Set;

/**
 * Abstract base for endpoints handling messages sent to aggregates.
 *
 * @param <I> the type of aggregate IDs
 * @param <A> the type of aggregates
 * @param <E> the type of message envelopes
 * @author Alexander Yevsyukov
 */
abstract class AggregateMessageEndpoint<I,
                                        A extends Aggregate<I, ?, ?>,
                                        E extends MessageEnvelope<?, ?>> {
    private final AggregateRepository<I, A> repository;
    private final E envelope;

    AggregateMessageEndpoint(AggregateRepository<I, A> repository, E envelope) {
        this.repository = repository;
        this.envelope = envelope;
    }

    /**
     * Creates a tenant-aware operation based on the message this endpoint processes.
     */
    abstract TenantAwareOperation createOperation();

    /**
     * {@linkplain #getTargets() Selects} one or more message targets and {@linkplain #dispatchTo(I)
     * dispatches} the message to them.
     */
    void dispatch() {
        final Set<I> targets = getTargets();
        for (I target : targets) {
            dispatchTo(target);
        }
    }

    /**
     * Dispatched the message to the aggregate with the passed ID.
     *
     * @param aggregateId the ID of the aggreagate to which dispatch the message
     */
    private void dispatchTo(I aggregateId) {
        final A aggregate = repository().loadOrCreate(aggregateId);

        final LifecycleFlags statusBefore = aggregate.getLifecycleFlags();

        final List<? extends Message> eventMessages = dispatchEnvelope(aggregate, envelope);

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);

        aggregate.apply(eventMessages, envelope);

        tx.commit();

        // Update status only if the message was handled successfully.
        final LifecycleFlags statusAfter = aggregate.getLifecycleFlags();
        if (statusAfter != null && !statusBefore.equals(statusAfter)) {
            storage().writeLifecycleFlags(aggregateId, statusAfter);
        }

        store(aggregate);
    }

    abstract List<? extends Message> dispatchEnvelope(A aggregate, E envelope);

    /**
     * Stores the aggregate if it has uncommitted events.
     *
     * @param aggregate the aggregate to store
     */
    private void store(A aggregate) {
        final List<Event> events = aggregate.getUncommittedEvents();
        if (!events.isEmpty()) {
            repository.onModifiedAggregate(envelope.getActorContext()
                                                   .getTenantId(), aggregate);
        } else {
            onEmptyResult(aggregate, envelope);
        }
    }

    /**
     * Allows derived classes to handle empty list of uncommitted events returned by the aggregate
     * in response to the message.
     */
    abstract void onEmptyResult(A aggregate, E envelope);

    /**
     * Obtains IDs of aggregates to which the endpoint delivers the message.
     */
    abstract Set<I> getTargets();

    /**
     * Obtains the envelope of the message processed by this endpoint.
     */
    E envelope() {
        return envelope;
    }

    AggregateRepository<I, A> repository() {
        return repository;
    }

    private AggregateStorage<I> storage() {
        return repository.aggregateStorage();
    }
}
