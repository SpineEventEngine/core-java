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

package io.spine.server.event;

import io.spine.annotation.Internal;
import io.spine.core.EventEnvelope;
import io.spine.server.entity.TransactionalEntity;

import java.util.function.BiFunction;

/**
 * A dispatch of the event to the appropriate handler of an entity.
 *
 * <p>Unlike the {@link io.spine.server.command.DispatchCommand}, this class is a simple wrapper
 * around the {@link BiFunction} which performs an actual event dispatch.
 *
 * @param <I>
 *         the type of entity ID
 * @param <E>
 *         the type of entity
 * @param <R>
 *         the type of the dispatch result
 */
@Internal
public final class EventDispatch<I, E extends TransactionalEntity<I, ?, ?>, R> {

    private final BiFunction<E, EventEnvelope, R> dispatchFunction;
    private final E entity;
    private final EventEnvelope event;

    /**
     * Creates a new {@code EventDispatch} from the given dispatch function.
     *
     * @param dispatchFunction
     *         the {@code BiFunction} that performs a dispatch operation
     * @param entity
     *         the entity to which the event is dispatched
     * @param event
     *         the dispatched event
     */
    public EventDispatch(BiFunction<E, EventEnvelope, R> dispatchFunction,
                         E entity,
                         EventEnvelope event) {
        this.dispatchFunction = dispatchFunction;
        this.entity = entity;
        this.event = event;
    }

    /**
     * Executes the dispatch operation, returning its result.
     */
    public R perform() {
        return dispatchFunction.apply(entity, event);
    }

    /**
     * Returns the entity to which the event is dispatched.
     */
    public E entity() {
        return entity;
    }

    /**
     * Returns the dispatched event.
     */
    public EventEnvelope event() {
        return event;
    }
}
