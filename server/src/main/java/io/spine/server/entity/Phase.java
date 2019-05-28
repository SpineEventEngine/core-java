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

package io.spine.server.entity;

import io.spine.annotation.Internal;
import io.spine.core.MessageId;
import io.spine.system.server.MessageDiagInfo;

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
 * @param <R>
 *         the type of the {@code Phase} propagation result
 * @see Transaction
 */
@Internal
public abstract class Phase<I, R> {

    private final VersionIncrement versionIncrement;

    private boolean successful = false;

    Phase(VersionIncrement versionIncrement) {
        this.versionIncrement = versionIncrement;
    }

    /**
     * Executes the task of the phase and updates the version of the entity in transaction.
     *
     * @return the result of the task execution
     */
    R propagate() {
        R result = performDispatch();
        versionIncrement.apply();
        markSuccessful();
        return result;
    }

    boolean isSuccessful() {
        return successful;
    }

    private void markSuccessful() {
        this.successful = true;
    }

    /**
     * Executes the dispatch task and returns the result.
     */
    protected abstract R performDispatch();

    /**
     * Returns the ID of the entity to which the {@code Message} is dispatched.
     */
    protected abstract I entityId();

    /**
     * Returns the dispatched {@code Message} ID.
     */
    protected abstract MessageId messageId();

    protected abstract MessageDiagInfo diagnostics();
}
