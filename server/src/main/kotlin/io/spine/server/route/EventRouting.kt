/*
 * Copyright 2023, TeamDev. All rights reserved.
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
package io.spine.server.route

import com.google.common.base.Preconditions
import com.google.errorprone.annotations.CanIgnoreReturnValue
import io.spine.base.EventMessage
import io.spine.core.EventContext
import io.spine.system.server.event.EntityStateChanged
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function

/**
 * A routing schema used to deliver events.
 *
 * A routing schema consists of a default route and custom routes per event class.
 * When calculating a set of event targets, `EventRouting` will see if there is
 * a custom route set for the type of the event. If not found, the default route will be
 * [applied][EventRoute.apply].
 *
 * @param I the type of the entity IDs to which events are routed.
 */
@Suppress("TooManyFunctions") // We want both inline and Java versions of the methods.
public class EventRouting<I: Any>
/**
 * Creates a new instance with the passed default route.
 *
 * @param defaultRoute
 *         the route to use if a custom one is not [set][.route].
 */
private constructor(defaultRoute: EventRoute<I, EventMessage>) :
    MessageRouting<EventMessage, EventContext, Set<I>>(defaultRoute),
    EventRoute<I, EventMessage> {

    /**
     * Overrides for return type covariance.
     */
    public override fun defaultRoute(): EventRoute<I, EventMessage> {
        @Suppress("UNCHECKED_CAST")
        return super.defaultRoute() as EventRoute<I, EventMessage>
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     * @return `this` to allow chained calls when configuring the routing
     */
    @CanIgnoreReturnValue
    public fun replaceDefault(newDefault: EventRoute<I, EventMessage>): EventRouting<I> {
        return super.replaceDefault(newDefault) as EventRouting<I>
    }

    /**
     * Sets a custom route for the passed event type.
     *
     * Such mapping may be required for the following cases:
     *
     *  * An event message should be matched to more than one entity, for example, several
     * projections updated in response to one event.
     *  * The type of event producer ID (stored in the event context) differs from the type
     * of entity identifiers (`<I>`.
     *
     * The type of the event can be a class or an interface. If a routing schema needs to contain
     * entries for specific classes and an interface that these classes implement, routes for
     * interfaces should be defined *after* entries for the classes:
     *
     * ```kotlin
     * customRouting.route<MyEventClass> { event, context -> ... }
     *              .route<MyEventInterface> { event, context -> ... }
     * ```
     * Defining an entry for an interface and then for the class which implements the interface will
     * result in `IllegalStateException`.
     *
     * If there is no specific route for an event type, the [default route][defaultRoute]
     * will be used.
     *
     * @param via
     *         the instance of the route to be used.
     * @param E the type of the event message.
     * @return `this` to allow chained calls when configuring the routing.
     * @throws IllegalStateException
     *          if the route for this event type is already set either directly or
     *          via a super-interface.
     */
    public inline fun <reified E : EventMessage> route(via: EventRoute<I, in E>): EventRouting<I> =
        route(E::class.java, via)

    /**
     * Sets a custom route for the passed event type.
     *
     * This is a Java version of `public inline fun` [route].
     *
     * @param eventType
     *         the type of events to route.
     * @param via
     *         the instance of the route to be used.
     * @param E the type of the event message.
     * @return `this` to allow chained calls when configuring the routing.
     * @throws IllegalStateException
     *          if the route for this event type is already set either directly or
     *          via a super-interface.
     */
    @CanIgnoreReturnValue
    public fun <E : EventMessage> route(
        eventType: Class<E>,
        via: EventRoute<I, in E>
    ): EventRouting<I> {
        @Suppress("UNCHECKED_CAST") // The cast is required to adapt the type to internal API.
        val casted = via as RouteFn<EventMessage, EventContext, Set<I>>
        addRoute(eventType, casted)
        return this
    }

    /**
     * Sets a custom route for the passed event type by obtaining the target entity
     * ID from the passed function.
     *
     * This is a convenience method for configuring routing when an event is to be delivered
     * to only one entity which ID is calculated from the event message. The simplest case of that
     * would be passing a method reference for an accessor of a field of the event message
     * which contains the ID of interest.
     *
     * @param via
     *         the function for obtaining the target entity ID.
     * @param E the type of the event message.
     * @return `this` to allow chained calls when configuring routing
     *
     * @see route
     */
    public inline fun <reified E : EventMessage> unicast(
        noinline via: (E) -> I
    ): EventRouting<I> = unicast(E::class.java, via)

    /**
     * Sets a custom route for the passed event type by obtaining the target entity
     * ID from the passed function.
     *
     * This is a Java version of `public inline fun` [unicast].
     *
     * @param eventType
     *         the type of the event to route.
     * @param via
     *         the function for obtaining the target entity ID.
     * @param E the type of the event message.
     * @return `this` to allow chained calls when configuring routing.
     *
     * @see route
     * @see unicast
     */
    @CanIgnoreReturnValue
    public fun <E : EventMessage> unicast(
        eventType: Class<E>,
        via: (E) -> I
    ): EventRouting<I> = route(eventType, EventFnRoute(via))


    /**
     * Sets a custom route for the passed event type by obtaining the target entity
     * ID from the passed function over event message and its context.
     *
     * This is a convenience method for configuring routing when an event is to be delivered
     * to only one entity which ID is calculated from the event message and its context.
     *
     * @param via
     *         the supplier of the target entity ID.
     * @param E the type of the event message.
     * @return `this` to allow chained calls when configuring routing.
     *
     * @see route
     */
    public inline fun <reified E : EventMessage> unicast(
        noinline via: (E, EventContext) -> I
    ): EventRouting<I> = unicast(E::class.java, via)

    /**
     * Sets a custom route for the passed event type by obtaining the target entity
     * ID from the passed function over event message and its context.
     *
     * This is a Java version of `public inline fun` [unicast].
     *
     * @param eventType
     *         the type of the event to route.
     * @param via
     *         the supplier of the target entity ID.
     * @param E the type of the event message.
     * @return `this` to allow chained calls when configuring routing.
     *
     * @see route
     * @see unicast
     */
    @CanIgnoreReturnValue
    public fun <E : EventMessage> unicast(
        eventType: Class<E>,
        via: BiFunction<E, EventContext, I>
    ): EventRouting<I> = route(eventType, EventFnRoute(via))

    /**
     * Sets a custom routing schema for entity state updates.
     *
     * @param routing
     *         the routing schema for entity state updates.
     * @return `this` to allow chained calls when configuring the routing.
     * @throws IllegalStateException
     *          if a route for [EntityStateChanged] is already set.
     */
    @CanIgnoreReturnValue
    public fun routeStateUpdates(routing: StateUpdateRouting<I>): EventRouting<I> =
        route(EntityStateChanged::class.java, routing.eventRoute())

    /**
     * Obtains a route for the passed event class.
     *
     * @param E the type of the event message.
     * @return optionally available route.
     */
    public inline fun <reified E: EventMessage> find(): EventRoute<I, E>? =
        find(E::class.java)

    /**
     * Obtains a route for the passed event class.
     *
     * @param eventClass
     *         the class of the event messages.
     * @param E the type of the event message.
     * @return optionally available route.
     */
    public fun <E : EventMessage> find(eventClass: Class<E>): EventRoute<I, E>? {
        val match: Match = routeFor(eventClass)
        return if (match.found()) {
            @Suppress("UNCHECKED_CAST") // protected by generic params of this class
            return match.route() as EventRoute<I, E>
        } else {
            null
        }
    }

    /**
     * Obtains a route for the passed event class.
     */
    @Deprecated("Use `find` instead.", ReplaceWith("find(eventClass)"))
    public fun <E : EventMessage> get(eventClass: Class<E>): Optional<EventRoute<I, E>> =
        Optional.ofNullable(find(eventClass))

    /**
     * Removes a route for the given event message class.
     *
     * @throws IllegalStateException
     *          if a custom route for this message class was not previously set, or already removed.
     */
    public inline fun <reified E : EventMessage> remove(): Unit =
        remove(E::class.java)

    public companion object {

        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 0L

        /**
         * Creates a new event routing with the passed default route.
         *
         * @param defaultRoute
         *         the default route.
         * @param I the type of entity identifiers returned by new routing.
         * @return new routing instance.
         */
        @JvmStatic
        @CanIgnoreReturnValue
        public fun <I : Any> withDefault(
            defaultRoute: EventRoute<I, EventMessage>
        ): EventRouting<I> = EventRouting(defaultRoute)

        /**
         * Creates a new event routing with the default one by event producer ID.
         *
         * @see withDefault
         * @see EventRoute.byProducerId
         */
        @JvmStatic
        public fun <I: Any> withDefaultByProducerId(): EventRouting<I> =
            withDefault(EventRoute.byProducerId())
    }
}
