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

package io.spine.server.procman;

import io.spine.base.Error;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.dispatch.Success;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.type.SignalEnvelope;

/**
 * Common base message for endpoints of Process Managers.
 *
 * @param <I> the type of process manager IDs
 * @param <P> the type of process managers
 * @param <M> the type of message envelopes processed by the endpoint
 */
abstract class PmEndpoint<I,
                          P extends ProcessManager<I, ?, ?>,
                          M extends SignalEnvelope<?, ?, ?>>
        extends EntityMessageEndpoint<I, P, M> {

    PmEndpoint(ProcessManagerRepository<I, P, ?> repository, M envelope) {
        super(repository, envelope);
    }

    @Override
    protected boolean isModified(P processManager) {
        boolean result = processManager.changed();
        return result;
    }

    @Override
    protected void onModified(P processManager) {
        repository().store(processManager);
    }

    @Override
    public ProcessManagerRepository<I, P, ?> repository() {
        return (ProcessManagerRepository<I, P, ?>) super.repository();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This method works differently to its analogues as it saves the entity
     *          state even if a rejection is thrown. It is done so because the process manager
     *          {@linkplain ProcessManagerRepository#lifecycle() lifecycle rules} may demand that
     *          entity becomes archived/deleted upon emitting certain rejection types.
     */
    @SuppressWarnings("UnnecessaryInheritDoc") // IDEA bug.
    @Override
    public void dispatchTo(I id) {
        P manager = repository().findOrCreate(id);
        tryDispatchAndSave(manager);
    }

    /**
     * Dispatches the message to a process manager and saves the entity state regardless of
     * successful delivery.
     */
    private void tryDispatchAndSave(P manager) {
        try {
            DispatchOutcome outcome = runTransactionFor(manager);
            store(manager);
            if (outcome.hasSuccess()) {
                postMessages(outcome.getSuccess());
                afterDispatched(manager.id());
            } else if (outcome.hasError()) {
                Error error = outcome.getError();
                repository().lifecycleOf(manager.id())
                            .onDispatchingFailed(envelope().messageId(), error);
            }
        } catch (RuntimeException ex) {
            store(manager);
            throw ex;
        }
    }

    private void postMessages(Success successfulOutcome) {
        Success.ExhaustCase type = successfulOutcome.getExhaustCase();
        switch (type) {
            case PRODUCED_EVENTS:
                repository().postEvents(successfulOutcome.getProducedEvents()
                                                         .getEventList());
                break;
            case REJECTION:
                repository().postEvent(successfulOutcome.getRejection());
                break;
            case PRODUCED_COMMANDS:
                repository().postCommands(successfulOutcome.getProducedCommands()
                                                           .getCommandList());
                break;
            case EXHAUST_NOT_SET:
            default:

        }
    }

    protected DispatchOutcome runTransactionFor(P processManager) {
        PmTransaction<?, ?, ?> tx = repository().beginTransactionFor(processManager);
        DispatchOutcome outcome = invokeDispatcher(processManager, envelope());
        tx.commitIfActive();
        return outcome;
    }
}
