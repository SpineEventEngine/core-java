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
import io.spine.server.event.EventDispatch;

/**
 * A phase that dispatches an event to the entity in transaction.
 *
 * @param <I>
 *         the type of entity ID
 * @param <E>
 *         the type of the entity
 * @param <R>
 *         the type of the event dispatch result
 */
@Internal
public class EventDispatchingPhase<I, E extends TransactionalEntity<I, ?, ?>, R>
        extends Phase<I, R> {

    private final EventDispatch<I, E, R> dispatch;

    public EventDispatchingPhase(EventDispatch<I, E, R> dispatch,
                                 VersionIncrement versionIncrement) {
        super(versionIncrement);
        this.dispatch = dispatch;
    }

    @Override
    protected R performDispatch() {
        R result = dispatch.perform();
        return result;
    }

    @Override
    public I getEntityId() {
        return dispatch.entity()
                       .id();
    }

    @Override
    public MessageId getMessageId() {
        return dispatch.event()
                       .id();
    }
}
