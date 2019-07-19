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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import io.spine.base.Error;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.Version;
import io.spine.logging.Logging;
import io.spine.server.dispatch.BatchDispatchOutcome;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.dispatch.ProducedEvents;
import io.spine.server.dispatch.Success;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.TransactionListener;
import io.spine.server.type.SignalEnvelope;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.spine.protobuf.AnyPacker.unpack;

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
                                 M extends SignalEnvelope<?, ?, ?>>
        extends EntityMessageEndpoint<I, A, M>
        implements Logging {

    AggregateEndpoint(AggregateRepository<I, A> repository, M envelope) {
        super(repository, envelope);
    }

    @Override
    public final void dispatchTo(I aggregateId) {
        A aggregate = loadOrCreate(aggregateId);
        LifecycleFlags flagsBefore = aggregate.lifecycleFlags();
        DispatchOutcome outcome = handleAndApplyEvents(aggregate);
        if (outcome.hasSuccess()) {
            updateLifecycle(aggregate, flagsBefore);
            storeAndPost(aggregate, outcome);
        } else if (outcome.hasError()) {
            Error error = outcome.getError();
            repository().lifecycleOf(aggregateId)
                        .onDispatchingFailed(envelope().messageId(), error);
        }
    }

    private void storeAndPost(A aggregate, DispatchOutcome outcome) {
        Success success = outcome.getSuccess();
        if (success.hasProducedEvents()) {
            store(aggregate);
            List<Event> events = success.getProducedEvents()
                                        .getEventList();
            post(events);
        } else if (success.hasRejection()) {
            post(success.getRejection());
        } else {
            onEmptyResult(aggregate, envelope());
        }
        afterDispatched(aggregate.id());
    }

    private void updateLifecycle(A aggregate, LifecycleFlags flagsBefore) {
        LifecycleFlags flagsAfter = aggregate.lifecycleFlags();
        if (flagsAfter != null && !flagsBefore.equals(flagsAfter)) {
            storage().writeLifecycleFlags(aggregate.id(), flagsAfter);
        }
    }

    private void post(Collection<Event> events) {
        repository().postEvents(events);
    }

    private void post(Event event) {
        post(ImmutableList.of(event));
    }

    private A loadOrCreate(I aggregateId) {
        return repository().loadOrCreate(aggregateId);
    }

    @CanIgnoreReturnValue
    final DispatchOutcome handleAndApplyEvents(A aggregate) {
        DispatchOutcome outcome = invokeDispatcher(aggregate, envelope());
        Success successfulOutcome = outcome.getSuccess();
        return successfulOutcome.hasProducedEvents()
               ? applyProducedEvents(aggregate, outcome)
               : outcome;
    }

    /**
     * Applies the produced by the Aggregate events to the Aggregate.
     *
     * @param aggregate
     *         the target Aggregate
     * @param commandOutcome
     *         the successful command propagation outcome
     * @return the outcome of the command propagation with the events with the correct versions or
     *         the erroneous outcome of applying an event
     */
    private DispatchOutcome applyProducedEvents(A aggregate, DispatchOutcome commandOutcome) {
        List<Event> events = commandOutcome.getSuccess()
                                           .getProducedEvents()
                                           .getEventList();
        AggregateTransaction tx = startTransaction(aggregate);
        BatchDispatchOutcome batchDispatchOutcome = aggregate.apply(events);
        if (batchDispatchOutcome.getSuccessful()) {
            tx.commitIfActive();
            return correctProducedEvents(commandOutcome, batchDispatchOutcome);
        } else {
            return firstErroneousOutcome(batchDispatchOutcome);
        }
    }

    /**
     * Corrects the versions of the produced events in the command outcome.
     *
     * @param commandOutcome
     *         the successful command outcome
     * @param eventDispatch
     *         the result of event applying
     * @return the same command outcome but with the events of the correct versions
     */
    private static DispatchOutcome
    correctProducedEvents(DispatchOutcome commandOutcome, BatchDispatchOutcome eventDispatch) {
        DispatchOutcome.Builder correctedCommandOutcome = commandOutcome.toBuilder();
        ProducedEvents.Builder eventsBuilder = correctedCommandOutcome.getSuccessBuilder()
                                                                      .getProducedEventsBuilder();
        Map<EventId, Event.Builder> correctedEvents = eventsBuilder
                .getEventBuilderList()
                .stream()
                .collect(toImmutableMap(Event.Builder::getId, identity()));
        for (DispatchOutcome outcome : eventDispatch.getOutcomeList()) {
            Any signalId = outcome.getPropagatedSignal()
                                  .getId();
            EventId eventId = unpack(signalId, EventId.class);
            Event.Builder event = checkNotNull(correctedEvents.get(eventId));
            Version signalVersion = outcome.getPropagatedSignal()
                                           .getVersion();
            event.getContextBuilder()
                 .setVersion(signalVersion);
        }
        // The validation is not triggered, as the previously valid message is only updated.
        return correctedCommandOutcome.build();
    }

    /**
     * Finds the first erroneous outcome in the given batchDispatchOutcome report.
     *
     * @param batchDispatchOutcome
     *         the non-successful dispatch
     * @return the first found outcome with an error
     */
    private static DispatchOutcome firstErroneousOutcome(BatchDispatchOutcome batchDispatchOutcome) {
        DispatchOutcome erroneous = batchDispatchOutcome
                .getOutcomeList()
                .stream()
                .filter(DispatchOutcome::hasError)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Dispatch was marked failed but no error occurred."
                ));
        return erroneous;
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
    public final AggregateRepository<I, A> repository() {
        return (AggregateRepository<I, A>) super.repository();
    }

    private AggregateStorage<I> storage() {
        return repository().aggregateStorage();
    }
}
