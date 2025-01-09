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

import io.spine.base.CommandMessage
import io.spine.base.EventMessage
import io.spine.base.MessageContext
import io.spine.base.SignalMessage
import io.spine.core.CommandContext
import io.spine.core.EventContext
import io.spine.logging.WithLogging
import io.spine.reflect.GenericTypeIndex
import io.spine.server.entity.Entity
import io.spine.string.simply
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * The abstract base for classes for scanning routing methods defined in an entity class.
 *
 * An entity class can declare static methods annotated with the [Route] annotation for
 * calculating IDs of the entities for which a signal should be dispatched.
 *
 * @param entityClass The class of the entity which may declare routing methods.
 * @param messageType The super interface for the routed signal messages, such as
 *   [CommandMessage] or [EventMessage].
 * @param contextType The super interface for the signal context messages, such as
 *   [CommandContext] or [EventContext].
 * @see Route
 */
public sealed class RoutingMethodMap<I: Any>(
    entityClass: Class<out Entity<I, *>>,
    private val messageType: Class<out SignalMessage>,
    private val contextType: Class<out MessageContext>,
) : WithLogging {

    protected val idClass: Class<*> = GenericParameter.ID.argumentIn(this::class.java)

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
        //TODO:2025-01-09:alexander.yevsyukov: Add sorting by interfaces.
        methods = collecting.toMap()
    }

    @Suppress("ReturnCount")
    private fun parameterTypesMatch(method: Method): Boolean {
        val methodName = "${method.declaringClass.canonicalName}.${method.name}"
        val errorProlog =
            "The method `$methodName` annotated with `@${simply<Route>()}` must accept"
        val nl = System.lineSeparator()
        val parameterTypes = method.parameterTypes
        if (parameterTypes.isEmpty() || parameterTypes.size > 2) {
            logger.atError().log {
                "$errorProlog one or two parameters.${nl}Encountered: `$method`."
            }
            return false
        }
        val firstParamType = parameterTypes[0]
        if (!messageType.isAssignableFrom(firstParamType)) {
            return false
        }
        if (parameterTypes.size == 2) {
            val secondParamType = parameterTypes[1]
            val match = contextType.isAssignableFrom(secondParamType)
            return match
        }
        return true
    }

    /**
     * The filter for a raw method for checking
     */
    protected abstract fun acceptReturnType(method: Method): Boolean

    /**
     * The factory method for creating an instance of [RoutingMethod] for the given raw method.
     */
    internal abstract fun createMethod(method: Method): RoutingMethod<I, *, *, *>

    private enum class GenericParameter(
        private val index: Int
    ) : GenericTypeIndex<RoutingMethodMap<*>> {

        ID(0);

        override fun index(): Int = index
    }
}

/**
 * Collects routing methods for commands.
 */
public class CommandRoutingMethodMap<I : Any>(
    entityClass: Class<out Entity<I, *>>,
) : RoutingMethodMap<I>(
    entityClass,
    CommandMessage::class.java,
    CommandContext::class.java,
) {
    override fun acceptReturnType(method: Method): Boolean {
        val returnType = method.returnType
        val returnsSingleId = idClass.isAssignableFrom(returnType)
        return returnsSingleId
    }

    override fun createMethod(method: Method): RoutingMethod<I, *, *, *> =
        CommandRoutingMethod(method)

    /**
     * Adds the collected methods as entries to the given command routing.
     */
    @Suppress("UNCHECKED_CAST") /* The casts are ensured by:
      1) The value of the `messageType` parameter passed to the `super` constructor.
      2) The result type of the `createMethod()`.
    */
    public fun addTo(routing: CommandRouting<I>) {
        methods.forEach { (messageClass, method) ->
            val commandClass = messageClass as Class<CommandMessage>
            val fn = method as CommandRoute<I, CommandMessage>
            routing.route(commandClass, fn)
        }
    }
}

/**
 * Collects routing method for events.
 */
public class EventRoutingMethodMap<I: Any>(
    entityClass: Class<out Entity<I, *>>,
) : RoutingMethodMap<I>(
    entityClass,
    EventMessage::class.java,
    EventContext::class.java,
) {
    override fun acceptReturnType(method: Method): Boolean {
        val returnType = method.returnType
        val returnsSet = Set::class.java.isAssignableFrom(returnType)
        val returnsSingleId =  idClass.isAssignableFrom(returnType)
        return returnsSingleId || returnsSet
    }

    override fun createMethod(method: Method): RoutingMethod<I, *, *, *> =
        EventRoutingMethod(method)

    /**
     * Adds the collected methods as entries to the given event routing.
     */
    @Suppress("UNCHECKED_CAST") /* The casts are ensured by:
      1) The value of the `messageType` parameter passed to the `super` constructor.
      2) The result type of the `createMethod()`.
    */
    public fun addTo(routing: EventRouting<I>) {
        methods.forEach { (messageClass, method) ->
            val eventClass = messageClass as Class<EventMessage>
            val fn = method as EventRoute<I, EventMessage>
            routing.route(eventClass, fn)
        }
    }
}
