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
import io.spine.core.Event;
import io.spine.logging.Logging;
import io.spine.server.type.EventEnvelope;

import java.util.List;

/**
 * The endpoint for importing events into aggregates.
 *
 * <p>Importing events one by one uses the same delivery mechanism as in event reaction of
 * aggregates. But unlike for event reaction, only one aggregate can be a target for event
 * being imported.
 *
 * @see io.spine.server.aggregate.Apply#allowImport()
 */
class EventImportEndpoint<I, A extends Aggregate<I, ?, ?>>
    extends AggregateEventEndpoint<I, A> implements Logging {

    EventImportEndpoint(AggregateRepository<I, A> repository, EventEnvelope event) {
        super(repository, event);
    }

    /**
     * Notifies the repository about the event being imported and returns the enclosed
     * {@link Event} instance.
     *
     * @return the list with one {@code Event} which is being imported
     * @implNote We do not need to perform anything with the aggregate and the passed event.
     * The aggregate would consume the passed event when dispatching result is
     * {@link io.spine.server.aggregate.AggregateEndpoint#runTransactionWith(Aggregate) applied}.
     */
    @Override
    protected List<Event> invokeDispatcher(A aggregate, EventEnvelope event) {
        return ImmutableList.of(event.outerObject());
    }

    @Override
    protected void afterDispatched(I entityId) {
        repository().lifecycleOf(entityId)
                    .onEventImported(envelope().outerObject());
    }

    @Override
    protected void onEmptyResult(A aggregate, EventEnvelope event) {
        _error("The aggregate `{}` was not modified during the import of the event `{}`.",
               aggregate, event);
    }

    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        _error(exception,
               "Error importing event of class `{}` into repository `{}`. " +
                       "Event message: `{}` context: `{}` id: `{}`",
               event.messageClass(),
               repository(),
               event.message(),
               event.messageClass(),
               event.idAsString());
    }
}
