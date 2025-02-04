/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.server.route

import com.google.common.base.Preconditions
import io.spine.base.EventMessage
import io.spine.core.EventContext
import io.spine.server.route.EventRoute.Companion.withId
import java.util.function.BiFunction
import java.util.function.Function

/**
 * An event route with one target entity ID of which is obtained as a function
 * on an event message or `BiFunction` on an event message and its context.
 *
 * @param <I>
 * the type of the target entity ID
 * @param <E>
 * the type of the event message
 */
internal class EventFnRoute<I : Any, E : EventMessage>(
    private val fn: BiFunction<E, EventContext, I>
) : EventRoute<I, E> {

    /**
     * Creates a new event route which obtains target entity ID from an event message.
     */
    constructor(fn: Function<E, I>) : this(BiFunction<E, EventContext, I> { e, _ ->
        fn.apply(e)
    })

    override fun apply(message: E, context: EventContext): Set<I> {
        val id = fn.apply(message, context)
        return withId(id)
    }
}
