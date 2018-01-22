/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import io.spine.core.CommandEnvelope;
import io.spine.core.Event;

import java.util.List;

/**
 * Dispatches command to process managers.
 *
 * @param <I> the type of process manager IDs
 * @param <P> the type of process managers
 * @author Alexander Yevsyukov
 */
class PmCommandEndpoint<I, P extends ProcessManager<I, ?, ?>>
        extends PmEndpoint<I, P, CommandEnvelope, I> {

    private PmCommandEndpoint(ProcessManagerRepository<I, P, ?> repository, CommandEnvelope cmd) {
        super(repository, cmd);
    }

    static <I, P extends ProcessManager<I, ?, ?>>
    PmCommandEndpoint<I, P> of(ProcessManagerRepository<I, P, ?> repository,
                               CommandEnvelope event) {
        return new PmCommandEndpoint<>(repository, event);
    }

    static <I, P extends ProcessManager<I, ?, ?>>
    I handle(ProcessManagerRepository<I, P, ?> repository, CommandEnvelope cmd) {
        final PmCommandEndpoint<I, P> endpoint = of(repository, cmd);
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
    protected PmCommandDelivery<I, P> getEndpointDelivery(CommandEnvelope envelope) {
        return repository().getCommandEndpointDelivery();
    }

    @Override
    protected List<Event> doDispatch(P processManager, CommandEnvelope command) {
        return processManager.dispatchCommand(command);
    }

    @Override
    protected void onError(CommandEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }

    /**
     * Does nothing since a state of a process manager should not be necessarily
     * updated during the command handling.
     */
    @Override
    protected void onEmptyResult(P processManager, CommandEnvelope cmd) {
        // Do nothing.
    }
}
