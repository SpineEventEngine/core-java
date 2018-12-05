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

package io.spine.server.entity;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import io.spine.annotation.Internal;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.server.event.EventDispatch;
import io.spine.server.model.Nothing;
import io.spine.validate.ValidatingBuilder;

@Internal
public abstract class EventPlayingTransaction<I,
                                              E extends TransactionalEntity<I, S, B>,
                                              S extends Message,
                                              B extends ValidatingBuilder<S, ? extends Builder>>
        extends Transaction<I, E, S, B> {

    protected EventPlayingTransaction(E entity) {
        super(entity);
    }

    protected EventPlayingTransaction(E entity, S state, Version version) {
        super(entity, state, version);
    }

    @SuppressWarnings("OverlyBroadCatchBlock")  /* to `rollback(..)` in case of any exception. */
    void play(EventEnvelope event) {
        VersionIncrement increment = createVersionIncrement(event);
        Phase<I, Nothing> phase = new EventDispatchingPhase<>(
                new EventDispatch<>(this::dispatch, getEntity(), event),
                increment
        );
        propagate(phase);
    }

    private Nothing dispatch(E entity, EventEnvelope event) {
        doDispatch(entity, event);
        return Nothing.getDefaultInstance();
    }

    /**
     * Dispatches the event message and its context to the current entity-in-transaction.
     *
     * <p>This operation is always performed in scope of an active transaction.
     *
     * @param entity
     *         the entity to which the envelope is dispatched
     * @param event
     *         the event to dispatch
     */
    protected abstract void doDispatch(E entity, EventEnvelope event);

    protected abstract VersionIncrement createVersionIncrement(EventEnvelope event);
}
