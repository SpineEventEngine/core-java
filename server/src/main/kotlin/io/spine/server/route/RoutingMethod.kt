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

import com.google.protobuf.Message
import io.spine.base.CommandMessage
import io.spine.base.EventMessage
import io.spine.core.CommandContext
import io.spine.core.EventContext
import java.lang.reflect.Method

public sealed class RoutingMethod<I: Any, M: Message, C: Message, R: Any>(
    protected val rawMethod: Method
) : RouteFn<M, C, R> {

    private val acceptsContext: Boolean = rawMethod.parameterTypes.size == 2

    protected fun invoke(message: M, context: C): Any? {
        val result = if (acceptsContext) {
            rawMethod.invoke(null, message, context)
        } else {
            rawMethod.invoke(null, message)
        }
        return result
    }
}

public class CommandRoutingMethod<I : Any>(
    rawMethod: Method
) : RoutingMethod<I, CommandMessage, CommandContext, I>(rawMethod) {

    public override fun apply(message: CommandMessage, context: CommandContext): I {
        val result = invoke(message, context)
        @Suppress("UNCHECKED_CAST") // protected by the scanning.
        return result as I
    }
}

public class EventRoutingMethod<I: Any>(
    rawMethod: Method
) : RoutingMethod<I, EventMessage, EventContext, Set<I>>(rawMethod) {

    private val returnsSet: Boolean = rawMethod.returnType == Set::class.java

    @Suppress("UNCHECKED_CAST")
    override fun apply(message: EventMessage, context: EventContext): Set<I> {
        val result = invoke(message, context)
        return if (returnsSet) {
            result as Set<I>
        } else {
            setOf(result as I)
        }
    }
}
