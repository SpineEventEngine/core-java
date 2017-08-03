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

package io.spine.server.procman;

import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.entity.Repository;

import java.util.List;
import java.util.Set;

/**
 * Dispatches event to reacting process managers.
 *
 * @param <I> the type of process manager IDs
 * @param <P> the type of process managers
 * @author Alexander Yevsyukov
 */
class PmEventEndpoint<I, P extends ProcessManager<I, ?, ?>>
        extends PmEndpoint<I, P, EventEnvelope, Set<I>> {

    private PmEventEndpoint(Repository<I, P> repository, EventEnvelope envelope) {
        super(repository, envelope);
    }

    static <I, P extends ProcessManager<I, ?, ?>>
    Set<I> handle (ProcessManagerRepository<I, P, ?> repository, EventEnvelope event) {
        final PmEventEndpoint<I, P> endpoint = new PmEventEndpoint<>(repository, event);
        final Set<I> result = endpoint.handle();
        return result;
    }

    @Override
    protected Set<I> getTargets() {
        final EventEnvelope event = envelope();
        final Set<I> ids = repository().eventRouting()
                                       .apply(event.getMessage(), event.getEventContext());
        return ids;
    }

    @Override
    protected List<Event> doDispatch(P processManager, EventEnvelope envelope) {
        final List<Event> events = processManager.dispatchEvent(envelope);
        return events;
    }

    @Override
    protected void onEmptyResult(P pm, EventEnvelope envelope) {
        // Do nothing. Reacting methods are allowed to return empty results.
    }

    @Override
    protected void onError(EventEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
        throw exception;
    }
}
