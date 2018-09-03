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

package io.spine.server.procman;

import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;

import java.util.List;

/**
 * Dispatches event to reacting process managers.
 *
 * @param <I> the type of process manager IDs
 * @param <P> the type of process managers
 * @author Alexander Yevsyukov
 */
@Internal
public class PmEventProxy<I, P extends ProcessManager<I, ?, ?>>
        extends PmProxy<I, P, EventEnvelope> {

    protected PmEventProxy(ProcessManagerRepository<I, P, ?> repository, I procmanId) {
        super(repository, procmanId);
    }

    @Override
    protected PmEventDelivery<I, P> getEndpointDelivery() {
        return repository().getEventEndpointDelivery();
    }

    @Override
    protected List<Event> doDispatch(P processManager, EventEnvelope envelope) {
        List<Event> events = processManager.dispatchEvent(envelope);
        return events;
    }

    /**
     * Does nothing since a state of a process manager should not be necessarily
     * updated upon reacting on an event.
     */
    @Override
    protected void onEmptyResult(P pm, EventEnvelope envelope) {
        // Do nothing.
    }

    @Override
    protected void onError(EventEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }
}
