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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.ActorMessageEnvelope;
import io.spine.core.Event;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.TransactionListener;

import java.util.List;

/**
 * Abstract base for endpoints handling messages sent to aggregates.
 *
 * @param <I> the type of aggregate IDs
 * @param <A> the type of aggregates
 * @param <M> the type of message envelopes
 * @param <R> the type of the dispatch result, which is {@code <I>} for unicast dispatching, and
 *            {@code Set<I>} for multicast
 * @author Alexander Yevsyukov
 */
abstract class AggregateEndpoint<I,
                                 A extends Aggregate<I, ?, ?>,
                                 M extends ActorMessageEnvelope<?, ?, ?>,
                                 R>
        extends EntityMessageEndpoint<I, A, M, R> {

    AggregateEndpoint(AggregateRepository<I, A> repository, M envelope) {
        super(repository, envelope);
    }

    @Override
    protected void deliverNowTo(I aggregateId) {
        A aggregate = instanceFor(aggregateId);
        LifecycleFlags flagsBefore = aggregate.getLifecycleFlags();

        List<Event> produced = dispatchInTx(aggregate);

        // Update lifecycle flags only if the message was handled successfully and flags changed.
        LifecycleFlags flagsAfter = aggregate.getLifecycleFlags();
        if (flagsAfter != null && !flagsBefore.equals(flagsAfter)) {
            storage().writeLifecycleFlags(aggregateId, flagsAfter);
        }

        store(aggregate);
        repository().postEvents(produced);
    }

    @CanIgnoreReturnValue
    protected List<Event> dispatchInTx(A aggregate) {
        List<Event> events = doDispatch(aggregate, envelope());
        AggregateTransaction tx = startTransaction(aggregate);
        List<Event> producedEvents = aggregate.apply(events, envelope());
        tx.commit();
        return producedEvents;
    }

    protected A instanceFor(I aggregateId) {
        return repository().loadOrCreate(aggregateId);
    }

    @SuppressWarnings("unchecked") // to avoid massive generic-related issues.
    protected AggregateTransaction startTransaction(A aggregate) {
        AggregateTransaction tx = AggregateTransaction.start(aggregate);
        TransactionListener listener = EntityLifecycleMonitor.newInstance(repository());
        tx.setListener(listener);
        return tx;
    }

    @Override
    protected void onModified(A entity) {
        repository().onModifiedAggregate(envelope().getTenantId(), entity);
    }

    @Override
    protected boolean isModified(A aggregate) {
        UncommittedEvents events = aggregate.getUncommittedEvents();
        return events.nonEmpty();
    }

    @Override
    protected AggregateRepository<I, A> repository() {
        return (AggregateRepository<I, A>)super.repository();
    }

    private AggregateStorage<I> storage() {
        return repository().aggregateStorage();
    }
}
