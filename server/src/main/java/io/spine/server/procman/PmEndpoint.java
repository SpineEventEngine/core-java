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

import io.spine.core.ActorMessageEnvelope;
import io.spine.core.Event;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.Repository;

import java.util.List;

/**
 * Common base message for endpoints of Process Managers.
 *
 * @param <I> the type of process manager IDs
 * @param <P> the type of process managers
 * @param <M> the type of message envelopes processed by the endpoint
 * @param <R> the type of the dispatch result, can be {@code <I>} for unicast dispatching, or
 *            {@code Set<I>} for multicast
 * @author Alexander Yevsyukov
 */
abstract class PmEndpoint<I,
                          P extends ProcessManager<I, ?, ?>,
                          M extends ActorMessageEnvelope<?, ?>,
                          R>
        extends EntityMessageEndpoint<I, P, M, R> {

    PmEndpoint(Repository<I, P> repository, M envelope) {
        super(repository, envelope);
    }

    @Override
    protected boolean isModified(P processManager) {
        final boolean result = processManager.isChanged();
        return result;
    }

    @Override
    protected void onModified(P processManager) {
        repository().store(processManager);
    }

    @Override
    protected ProcessManagerRepository<I, P, ?> repository() {
        return (ProcessManagerRepository<I, P, ?>) super.repository();
    }

    @Override
    protected void dispatchToOne(I id) {
        final ProcessManagerRepository<I, P, ?> repository = repository();
        final P manager = repository.findOrCreate(id);

        final PmTransaction<?, ?, ?> tx = repository.beginTransactionFor(manager);
        @SuppressWarnings("unchecked") // all PM handlers return events.
        final List<Event> events = (List<Event>) doDispatch(manager, envelope());
        tx.commit();
        store(manager);
        repository.postEvents(events);
    }
}
