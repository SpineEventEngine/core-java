/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.core.Event;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.PropagationOutcome;
import io.spine.server.entity.Success;
import io.spine.server.entity.TransactionListener;
import io.spine.server.type.ActorMessageEnvelope;

import java.util.List;

/**
 * Abstract base for endpoints handling messages sent to aggregates.
 *
 * @param <I>
 *         the type of aggregate IDs
 * @param <A>
 *         the type of aggregates
 * @param <M>
 *         the type of message envelopes
 */
abstract class AggregateEndpoint<I,
                                 A extends Aggregate<I, ?, ?>,
                                 M extends ActorMessageEnvelope<?, ?, ?>>
        extends EntityMessageEndpoint<I, A, M> {

    AggregateEndpoint(AggregateRepository<I, A> repository, M envelope) {
        super(repository, envelope);
    }

    @Override
    protected final void dispatchInTx(I aggregateId) {
        A aggregate = loadOrCreate(aggregateId);
        LifecycleFlags flagsBefore = aggregate.lifecycleFlags();

        PropagationOutcome outcome = runTransactionWith(aggregate);
        if (outcome.hasSuccess()) {
            // Update lifecycle flags only if the message was handled successfully and flags changed.
            LifecycleFlags flagsAfter = aggregate.lifecycleFlags();
            if (flagsAfter != null && !flagsBefore.equals(flagsAfter)) {
                storage().writeLifecycleFlags(aggregateId, flagsAfter);
            }

            store(aggregate);
            Success success = outcome.getSuccess();
            if (success.hasProducedEvents()) {
                List<Event> events = success.getProducedEvents()
                                            .getEventList();
                repository().postEvents(events);
            }
        } else {
            // TODO:2019-07-01:dmytro.dashenkov: Post "errored" system event.
        }
    }

    private A loadOrCreate(I aggregateId) {
        return repository().loadOrCreate(aggregateId);
    }

    @CanIgnoreReturnValue
    final PropagationOutcome runTransactionWith(A aggregate) {
        PropagationOutcome outcome = invokeDispatcher(aggregate, envelope());
        AggregateTransaction tx = startTransaction(aggregate);
        Success successfulOutcome = outcome.getSuccess();
        if (successfulOutcome.hasProducedEvents()) {
            List<Event> eventList = successfulOutcome.getProducedEvents()
                                                     .getEventList();
            List<Event> producedEvents = aggregate.apply(eventList);
            tx.commit();
            PropagationOutcome.Builder result = outcome.toBuilder();
            result.getSuccessBuilder()
                  .getProducedEventsBuilder()
                  .clearEvent()
                  .addAllEvent(producedEvents);
            return result.vBuild();
        } else {
            tx.commit();
            return outcome;
        }
    }

    @SuppressWarnings("unchecked") // to avoid massive generic-related issues.
    private AggregateTransaction startTransaction(A aggregate) {
        AggregateTransaction tx = AggregateTransaction.start(aggregate);
        TransactionListener listener =
                EntityLifecycleMonitor.newInstance(repository(), aggregate.id());
        tx.setListener(listener);
        return tx;
    }

    @Override
    protected final void onModified(A entity) {
        repository().store(entity);
    }

    @Override
    protected final boolean isModified(A aggregate) {
        UncommittedEvents events = aggregate.getUncommittedEvents();
        return events.nonEmpty();
    }

    @Override
    protected final AggregateRepository<I, A> repository() {
        return (AggregateRepository<I, A>) super.repository();
    }

    private AggregateStorage<I> storage() {
        return repository().aggregateStorage();
    }
}
