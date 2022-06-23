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

package io.spine.server.entity;

import io.spine.annotation.Internal;
import io.spine.core.Signal;
import io.spine.core.SignalId;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.dispatch.DispatchOutcomeHandler;
import io.spine.server.dispatch.Success;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An atomic entity state change which advances the entity version.
 *
 * <p>The {@code Phase} is a part of {@linkplain Transaction transaction} mechanism and should only
 * be propagated during the active transaction.
 *
 * <p>Typically, to perform the entity state change, a {@code Phase} executes some event/command
 * dispatch task provided by the caller from the outside.
 *
 * <p>The result of the dispatch task execution could be the list of
 * {@linkplain io.spine.core.Event events} if the applier method generated events or the
 * list of {@linkplain io.spine.core.Command commands} if the dispatch was performed to the
 * commanding method.
 *
 * @param <I>
 *         the type of entity ID
 * @see Transaction
 */
@Internal
public abstract class Phase<I> {

    private final Transaction<I, ?, ?, ?> transaction;
    private final VersionIncrement versionIncrement;

    Phase(Transaction<I, ?, ?, ?> transaction, VersionIncrement increment) {
        this.transaction = checkNotNull(transaction);
        this.versionIncrement = checkNotNull(increment);
    }

    /**
     * Executes the task of the phase and updates the version of the entity in transaction.
     *
     * @return the result of the task execution
     */
    final DispatchOutcome propagate() {
        DispatchOutcome outcome = performDispatch();
        return DispatchOutcomeHandler
                .from(outcome)
                .onSuccess(this::incrementTransaction)
                .handle();
    }

    private void incrementTransaction(Success success) {
        if (!success.hasRejection()) {
            transaction.incrementStateAndVersion(versionIncrement);
        }
    }

    /**
     * Executes the dispatch task and returns the result.
     */
    protected abstract DispatchOutcome performDispatch();

    /**
     * Returns the ID of the entity to which the {@code Message} is dispatched.
     */
    protected abstract I entityId();

    /**
     * Returns the dispatched {@code Message} ID.
     */
    protected abstract SignalId messageId();

    /**
     * Obtains the signal message applied by this phase.
     */
    protected abstract Signal<?, ?, ?> signal();
}
