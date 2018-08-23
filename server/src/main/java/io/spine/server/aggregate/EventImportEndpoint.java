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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.logging.Logging;
import io.spine.server.delivery.Delivery;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * The endpoint for importing events into aggregates.
 *
 * <p>Importing events one by one uses the same delivery mechanism as in event reaction of
 * aggregates. But unlike for event reaction, only one aggregate can be a target for event
 * being imported.
 *
 * @author Alexander Yevsyukov
 * @see io.spine.server.aggregate.Apply#allowImport()
 */
class EventImportEndpoint<I, A extends Aggregate<I, ?, ?>>
    extends AggregateEventEndpoint<I, A> implements Logging {

    EventImportEndpoint(AggregateRepository<I, A> repository, EventEnvelope envelope) {
        super(repository, envelope);
    }

    /**
     * Returns a set with one element of the target aggregate.
     */
    @Override
    protected Set<I> getTargets() {
        EventEnvelope envelope = envelope();
        Set<I> ids = repository().getEventImportRouting()
                                 .apply(envelope.getMessage(), envelope.getEventContext());
        int numberOfTargets = ids.size();
        checkState(
                numberOfTargets > 0,
                "Could not get aggregate ID from the event context: `%s`. Event class: `%s`.",
                envelope.getEventContext(),
                envelope.getMessageClass()
        );

        checkState(
                numberOfTargets == 1,
                "Expected one aggregate ID, but got %s (`%s`). Event class: `%s`, context: `%s`.",
                String.valueOf(numberOfTargets),
                ids,
                envelope.getMessageClass(),
                envelope.getEventContext()
        );

        I id = ids.stream()
                  .findFirst()
                  .get();
        repository().onImportTargetSet(id, envelope.getId());
        return ImmutableSet.of(id);
    }


    @Override
    protected Delivery<I, A, EventEnvelope, ?, ?> getEndpointDelivery() {
        return repository().getEventEndpointDelivery();
    }

    /**
     * Notifies the repository about the event being imported and returns the enclosed
     * {@link Event} instance.
     *
     * @return the list with one {@code Event} which is being imported
     */
    @Override
    protected List<Event> doDispatch(A aggregate, EventEnvelope envelope) {
        I id = aggregate.getId();
        Event event = envelope.getOuterObject();
        repository().onImportEvent(id, event);
        // We do not need to perform anything with the aggregate. It would consume the passed
        // event when `AggregateEndpoint` would `apply()` the returned event on the aggregate.
        // Just return the event to be imported.
        return ImmutableList.of(event);
    }

    @Override
    protected void onEmptyResult(A aggregate, EventEnvelope envelope) {
        _error("The aggregate `{}` was not modified during the import of the event `{}`.",
               aggregate, envelope);
    }

    @Override
    protected void onError(EventEnvelope envelope, RuntimeException exception) {
        _error(exception,
               "Error importing event of class `{}` into repository `{}`. " +
                       "Event message: `{}` context: `{}` id: `{}`",
               envelope.getMessageClass(),
               repository(),
               envelope.getMessage(),
               envelope.getMessageClass(),
               envelope.idAsString());
    }
}
