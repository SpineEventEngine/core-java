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

    protected AggregateMessageEndpoint(AggregateRepository<I, A> repository, E envelope) {
        this.repository = repository;
        this.envelope = envelope;
    }

    protected abstract TenantAwareOperation createOperation();

    /**
     * Dispatches the command to an aggregate.
     */
    protected void dispatch() {
        final Set<I> targets = getTargets();
        for (I target : targets) {
            dispatchTo(target);
        }
    }

    protected void dispatchTo(I aggregateId) {
        final E envelope = envelope();
        final A aggregate = repository().loadOrCreate(aggregateId);

        final LifecycleFlags statusBefore = aggregate.getLifecycleFlags();

        final List<? extends Message> eventMessages = dispatchEnvelope(aggregate, envelope);

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);

        aggregate.apply(eventMessages, envelope);

        tx.commit();

        // Update status only if the command was handled successfully.
        final LifecycleFlags statusAfter = aggregate.getLifecycleFlags();
        if (statusAfter != null && !statusBefore.equals(statusAfter)) {
            storage().writeLifecycleFlags(aggregateId, statusAfter);
        }

        store(aggregate);
    }

    protected abstract List<? extends Message> dispatchEnvelope(A aggregate, E envelope);

    protected void store(A aggregate) {
        final List<Event> events = aggregate.getUncommittedEvents();
        if (!events.isEmpty()) {
            repository.store(aggregate);
            repository.updateStand(envelope.getActorContext()
                                           .getTenantId(), aggregate);
            repository.postEvents(events);
        }
        //TODO:2017-07-11:alexander.yevsyukov: For command handling complain if the list of events is empty.
        // An aggregate cannot silently accept commands without either producing events or rejecting commands.
    }

    protected abstract Set<I> getTargets();

    protected E envelope() {
        return envelope;
    }

    protected AggregateRepository<I, A> repository() {
        return repository;
    }

    private AggregateStorage<I> storage() {
        return repository.aggregateStorage();
    }
}
