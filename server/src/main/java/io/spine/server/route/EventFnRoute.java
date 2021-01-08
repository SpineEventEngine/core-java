/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.route;

import io.spine.base.EventMessage;
import io.spine.core.EventContext;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event route with one target entity ID of which is obtained as a function
 * on an event message or {@code BiFunction} on an event message and its context.
 *
 * @param <I>
 *         the type of the target entity ID
 * @param <E>
 *         the type of the event message
 */
final class EventFnRoute<I, E extends EventMessage> implements EventRoute<I, E> {

    private static final long serialVersionUID = 0L;
    private final BiFunction<E, EventContext, I> fn;

    /**
     * Creates a new event route which obtains target entity ID from an event message.
     */
    EventFnRoute(Function<E, I> fn) {
        this((e, ctx) -> fn.apply(e));
    }

    /**
     * Creates an event route which obtains target entity ID from an event message and
     * a its context.
     */
    EventFnRoute(BiFunction<E, EventContext, I> fn) {
        this.fn = checkNotNull(fn);
    }

    @Override
    public Set<I> apply(E message, EventContext context) {
        I id = fn.apply(message, context);
        return EventRoute.withId(id);
    }
}
