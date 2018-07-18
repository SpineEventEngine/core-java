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
import io.spine.core.RejectionEnvelope;

import java.util.List;
import java.util.Set;

/**
 * Dispatches a rejection to a reacting process manager.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class PmRejectionEndpoint<I, P extends ProcessManager<I, ?, ?>>
    extends PmEndpoint<I, P, RejectionEnvelope, Set<I>> {

    protected PmRejectionEndpoint(ProcessManagerRepository<I, P, ?> repository,
                                  RejectionEnvelope envelope) {
        super(repository, envelope);
    }

    static <I, P extends ProcessManager<I, ?, ?>>
    PmRejectionEndpoint<I, P> of(ProcessManagerRepository<I, P, ?> repository,
                                 RejectionEnvelope event) {
        return new PmRejectionEndpoint<>(repository, event);
    }

    static <I, P extends ProcessManager<I, ?, ?>>
    Set<I> handle(ProcessManagerRepository<I, P, ?> repository, RejectionEnvelope rejection) {
        PmRejectionEndpoint<I, P> endpoint = of(repository, rejection);
        Set<I> result = endpoint.handle();
        return result;
    }

    @Override
    protected Set<I> getTargets() {
        RejectionEnvelope envelope = envelope();
        Set<I> ids =
                repository().getRejectionRouting()
                            .apply(envelope.getMessage(), envelope.getMessageContext());
        return ids;
    }

    @Override
    protected PmRejectionDelivery<I,P> getEndpointDelivery() {
        return repository().getRejectionEndpointDelivery();
    }

    @Override
    protected List<Event> doDispatch(P entity, RejectionEnvelope envelope) {
        List<Event> events = entity.dispatchRejection(envelope);
        return events;
    }

    /**
     * Does nothing since a state of a process manager should not be necessarily
     * updated upon reacting on a rejection.
     */
    @Override
    protected void onEmptyResult(P entity, RejectionEnvelope envelope) {
        // Do nothing. Reacting methods are allowed to return empty results.
    }

    @Override
    protected void onError(RejectionEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }
}
