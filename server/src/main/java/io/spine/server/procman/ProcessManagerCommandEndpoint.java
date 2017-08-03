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

import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.entity.EntityMessageEndpoint;

import java.util.List;

class ProcessManagerCommandEndpoint<I, P extends ProcessManager<I, ?, ?>>
        extends EntityMessageEndpoint<I, P, CommandEnvelope, I> {

    private ProcessManagerCommandEndpoint(ProcessManagerRepository<I, P, ?> repository,
                                          CommandEnvelope envelope) {
        super(repository, envelope);
    }

    static <I, P extends ProcessManager<I, ?, ?>>
    I handle (ProcessManagerRepository<I, P, ?> repository, CommandEnvelope cmd) {
        final ProcessManagerCommandEndpoint<I, P> endpoint =
                new ProcessManagerCommandEndpoint<>(repository, cmd);
        final I result = endpoint.handle();
        return result;
    }

    @Override
    protected I getTargets() {
        final CommandEnvelope envelope = envelope();
        final I id = repository().getCommandRouting()
                                 .apply(envelope.getMessage(), envelope.getCommandContext());
        return id;
    }

    @Override
    protected void dispatchToOne(I id) {
        final ProcessManagerRepository<I, P, ?> repository = repository();
        final P manager = repository.findOrCreate(id);

        final ProcManTransaction<?, ?, ?> tx = repository.beginTransactionFor(manager);
        final List<Event> events = manager.dispatchCommand(envelope());
        tx.commit();
        store(manager);
        repository.postEvents(events);
    }

    @Override
    protected List<? extends Message> doDispatch(P processManager, CommandEnvelope command) {
        return processManager.dispatchCommand(command);
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
    protected void onError(CommandEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
        throw exception;
    }

    /**
     * Throws {@link IllegalStateException} with the message containing details of
     * the process manager and the command in response to which empty set of event messages
     * was generated.
     * @throws IllegalStateException always
     */
    @Override
    protected void onEmptyResult(P processManager, CommandEnvelope cmd)
            throws IllegalStateException {
        final String format =
                "The process manager (class: %s, id: %s) produced " +
                        "empty response for the command (class: %s, id: %s).";
        onUnhandledCommand(processManager, cmd, format);
    }

    @Override
    protected ProcessManagerRepository<I, P, ?> repository() {
        return (ProcessManagerRepository<I, P, ?>) super.repository();
    }
}
