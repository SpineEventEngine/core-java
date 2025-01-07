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

import io.spine.base.MessageContext
import io.spine.base.SignalMessage
import io.spine.logging.WithLogging
import io.spine.server.entity.Entity
import io.spine.string.simply
import java.lang.reflect.Method
import java.lang.reflect.Modifier

public sealed class RoutingMethodMap<I: Any>(
    protected val entityClass: Class<out Entity<I, *>>,
    protected val messageType: Class<out SignalMessage>,
    protected val contextType: Class<out MessageContext>
) : WithLogging {
    internal val methods: Map<Class<out SignalMessage>, RoutingMethod<I, *, *, *>>
    
    init {
        val collecting = mutableMapOf<Class<out SignalMessage>, RoutingMethod<I, *, *, *>>()
        entityClass.declaredMethods
            .filter { Modifier.isStatic(it.modifiers) }
            .filter { it.isAnnotationPresent(Route::class.java) }
            .filter { parameterTypesMatch(it) }
            .filter { acceptReturnType(it) }
            .forEach { method ->
                @Suppress("UNCHECKED_CAST") // protected by checking parameters before.
                val firstParam = method.parameters[0].type as Class<out SignalMessage>
                collecting[firstParam] = createMethod(method)
            }
        methods = collecting.toMap()
    }

    @Suppress("ReturnCount")
    private fun parameterTypesMatch(method: Method): Boolean {
        val methodName = "${method.declaringClass.canonicalName}.${method.name}"
        val errorProlog = "The method `$methodName` annotated with ${simply<Route>()} must accept"

        val parameterTypes = method.parameterTypes
        if (parameterTypes.isEmpty() || parameterTypes.size > 2) {
            logger.atError().log {
                "$errorProlog one or two parameters. Encountered: `$method`."
            }
            return false
        }
        val firstParamType = parameterTypes[0]
        if (!messageType.isAssignableFrom(firstParamType)) {
            logger.atError().log {
                "$errorProlog a parameter implementing `${messageType.canonicalName}`." +
                        " Encountered: `${firstParamType.canonicalName}`."
            }
            return false
        }
        if (parameterTypes.size == 2) {
            val secondParamType = parameterTypes[1]
            val match = contextType.isAssignableFrom(secondParamType)
            if (!match) {
                logger.atError().log {
                    "$errorProlog a second parameter implementing `${contextType.canonicalName}`." +
                            " Encountered: `${firstParamType.canonicalName}`."
                }
            }
            return match
        }
        return true
    }

    protected abstract fun acceptReturnType(method: Method): Boolean

    protected abstract fun createMethod(method: Method): RoutingMethod<I, *, *, *>
}

