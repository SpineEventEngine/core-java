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

package io.spine.server.event;

import io.spine.annotation.Internal;
import io.spine.core.EventEnvelope;
import io.spine.server.entity.TransactionalEntity;

import java.util.function.BiFunction;

@Internal
public final class EventDispatch<I, E extends TransactionalEntity<I, ?, ?>, R> {

    private final BiFunction<E, EventEnvelope, R> dispatchFunction;
    private final E entity;
    private final EventEnvelope event;

    public EventDispatch(BiFunction<E, EventEnvelope, R> dispatchFunction,
                         E entity,
                         EventEnvelope event) {
        this.dispatchFunction = dispatchFunction;
        this.entity = entity;
        this.event = event;
    }

    public R perform() {
        return dispatchFunction.apply(entity, event);
    }

    public I entityId() {
        return entity.getId();
    }

    public EventEnvelope event() {
        return event;
    }
}
