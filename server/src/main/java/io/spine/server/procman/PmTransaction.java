/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.core.Version;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.command.DispatchCommand;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.entity.CommandDispatchingPhase;
import io.spine.server.entity.EventDispatchingPhase;
import io.spine.server.entity.Phase;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.VersionIncrement;
import io.spine.server.event.EventDispatch;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;

/**
 * A transaction, within which {@linkplain ProcessManager ProcessManager instances} are modified.
 *
 * @param <I>
 *         the type of process manager IDs
 * @param <S>
 *         the type of process manager state
 * @param <B>
 *         the type of a {@code ValidatingBuilder} for the process manager state
 */
@Internal
public class PmTransaction<I,
                           S extends EntityState,
                           B extends ValidatingBuilder<S>>
        extends Transaction<I, ProcessManager<I, S, B>, S, B> {

    @VisibleForTesting
    protected PmTransaction(ProcessManager<I, S, B> processManager) {
        super(processManager);
    }

    @VisibleForTesting
    protected PmTransaction(ProcessManager<I, S, B> processManager, S state, Version version) {
        super(processManager, state, version);
    }

    /**
     * Executes the given command dispatch for the current entity in transaction.
     *
     * @param dispatch
     *         the {@code DispatchCommand} task
     * @return the events generated from the command dispatch
     * @see ProcessManager#dispatchCommand(CommandEnvelope)
     */
    final DispatchOutcome perform(DispatchCommand<I> dispatch) {
        VersionIncrement vi = createVersionIncrement();
        Phase<I> phase = new CommandDispatchingPhase<>(this, dispatch, vi);
        return propagate(phase);
    }

    /**
     * Dispatches the given event to the current entity in transaction.
     *
     * @param event
     *         the event to dispatch
     * @return the events generated from the event dispatch
     * @see ProcessManager#dispatchEvent(EventEnvelope)
     */
    final DispatchOutcome dispatchEvent(EventEnvelope event) {
        Phase<I> phase = new EventDispatchingPhase<>(
                this,
                createDispatch(event),
                createVersionIncrement()
        );
        return propagate(phase);
    }

    private EventDispatch<I, ProcessManager<I, S, B>>
    createDispatch(EventEnvelope event) {
        return new EventDispatch<>(this::dispatch, entity(), event);
    }

    private DispatchOutcome dispatch(ProcessManager<I, S, B> pm, EventEnvelope event) {
        return pm.dispatchEvent(event);
    }

    private VersionIncrement createVersionIncrement() {
        return VersionIncrement.sequentially(this);
    }
}
