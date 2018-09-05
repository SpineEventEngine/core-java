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

import io.spine.core.ActorMessageEnvelope;
import io.spine.core.Event;
import io.spine.server.entity.EntityProxy;

import java.util.List;

/**
 * Implementation base for Process Managers proxies.
 *
 * @param <I> the type of process manager IDs
 * @param <P> the type of process managers
 * @param <M> the type of message envelopes processed by the proxy
 * @author Alexander Yevsyukov
 */
abstract class PmProxy<I,
                          P extends ProcessManager<I, ?, ?>,
                          M extends ActorMessageEnvelope<?, ?, ?>>
        extends EntityProxy<I, P, M> {

    PmProxy(ProcessManagerRepository<I, P, ?> repository, I procmanId) {
        super(repository, procmanId);
    }

    @Override
    protected boolean isModified(P processManager) {
        boolean result = processManager.isChanged();
        return result;
    }

    @Override
    protected void onModified(P processManager, M message) {
        repository().store(processManager);
    }

    @Override
    protected ProcessManagerRepository<I, P, ?> repository() {
        return (ProcessManagerRepository<I, P, ?>) super.repository();
    }

    @Override
    protected void deliverNow(M message) {
        ProcessManagerRepository<I, P, ?> repository = repository();
        P manager = repository.findOrCreate(entityId());
        List<Event> events = dispatchInTx(manager, message);
        store(manager, message);
        repository.postEvents(events);
    }

    protected List<Event> dispatchInTx(P processManager, M message) {
        PmTransaction<?, ?, ?> tx = repository().beginTransactionFor(processManager);
        List<Event> events = doDispatch(processManager, message);
        tx.commit();
        return events;
    }
}
