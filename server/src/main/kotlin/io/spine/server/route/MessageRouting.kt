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

import com.google.common.annotations.VisibleForTesting
import com.google.errorprone.annotations.CanIgnoreReturnValue
import io.spine.base.Routable
import io.spine.core.SignalContext
import java.util.Collections.synchronizedMap

/**
 * A routing schema for a kind of messages such as commands, events, rejections, or entity states.
 *
 * A routing schema consists of a default route and custom routes per message class or
 * a grouping interface.
 *
 * @param I The type of identifiers used in the routing.
 * @param M The common supertype of messages to route.
 * @param C The type of message context.
 * @param R The type returned by a [routing function][RouteFn.apply].
 *   For unicast routing it would be the same as [I], and `Set<I>` for multicast routing.
 * @param S The type of message routing for covariance in return types.
 * @property defaultRoute The route to be used if there is no matching entry set in [routes].
 */
@Suppress("TooManyFunctions")
public sealed class MessageRouting<
        I : Any,
        M : Routable,
        C : SignalContext,
        R : Any,
        S : MessageRouting<I, M, C, R, S>
        >(
    private var defaultRoute: RouteFn<M, C, R>
) : RouteFn<M, C, R> {

    protected abstract fun self(): S

    /**
     * Maps a message class to the function for calculating identifier(s) of
     * entities to deliver a message of this class.
     *
     * ### Implementation note
     *
     * This collection is made `synchronized`, since in some cases it is
     * being simultaneously read and modified in different threads.
     * In particular, the modification at run-time is performed when storing
     * the routes discovered on a per-interface basis.
     * Therefore, if this collection is not `synchronized`,
     * a `ConcurrentModificationException` is sometimes thrown.
     */
    private val routes: MutableMap<Class<M>, RouteFn<M, C, R>> =
        synchronizedMap(LinkedHashMap())

    /**
     * Obtains the default route to be used when none of the added entries match
     * the argument passed to the [apply] function.
     */
    @VisibleForTesting
    public open fun defaultRoute(): RouteFn<M, C, R> = defaultRoute

    /**
     * Sets a new default route in the schema.
     *
     * @param newDefault The new route to be used as default.
     */
    @CanIgnoreReturnValue
    public fun replaceDefault(newDefault: RouteFn<M, C, out R>): S {
        @Suppress("UNCHECKED_CAST")
        defaultRoute = newDefault as RouteFn<M, C, R>
        return self()
    }

    @CanIgnoreReturnValue
    public fun <N: M> route(msgClass: Class<N>, via: RouteFn<N, C, R>): S {
        addRoute(msgClass, via)
        return self()
    }

    public inline fun <reified N: M> route(via: RouteFn<N, C, R>): S =
        route(N::class.java, via)

    public fun <N : M> unicast(
        msgType: Class<N>,
        via: (N) -> I
    ): S = route(msgType, createUnicastRoute(via))

    protected abstract fun <N : M> createUnicastRoute(via: (N) -> I): RouteFn<N, C, R>

    protected abstract fun <N : M> createUnicastRoute(via: (N, C) -> I): RouteFn<N, C, R>

    /**
     * Checks if the passed message type is supported by this instance of routing.
     */
    public open fun supports(messageType: Class<out M>): Boolean {
        val match = routeFor(messageType)
        val result = match.found
        return result
    }

    /**
     * Sets a custom route for the passed message type.
     *
     * The type can be either a class or interface. If the routing schema already contains an
     * entry with the same type or a super-interface of the passed type
     * an [IllegalStateException] will be thrown.
     *
     * In order to provide a mapping for a specific class *and* an interface common
     * to this and other message classes, please add the routing for the class *before*
     * the interface.
     *
     * @param N The type derived from common type M served by this routing.
     *   Could be a sub-interface or a class.
     * @param messageType The type of messages to route.
     * @param via The route function to be used for this type.
     * @throws IllegalStateException If the route for this message class is already set either
     *   directly or via a super-interface.
     */
    public fun <N : M> addRoute(messageType: Class<N>, via: RouteFn<N, C, R>) {
        val match = routeFor(messageType)
        if (match.found) {
            val requestedClass = messageType.name
            val entryClass: String = match.entryClass!!.name
            if (match.isDirect) {
                error(
                    "The route for the message class $requestedClass` already set." +
                            " Please remove the route (`$entryClass`) before setting a new one."
                )
            } else {
                error(
                    "The route for the message class `$requestedClass` already defined via" +
                            " the interface `$entryClass`. If you want to have specific" +
                            " routing for the class `$requestedClass`," +
                            " please put it before the routing for the super-interface.",
                )
            }
        }
        @Suppress("UNCHECKED_CAST")
        routes[messageType as Class<M>] = via as RouteFn<M, C, R>
    }

    /**
     * Obtains a route for the passed message class.
     *
     * @param msgCls the class of the messages.
     * @return optionally available route.
     */
    internal fun routeFor(msgCls: Class<out M>): Match {
        val direct = findDirect(msgCls)
        if (direct.found) {
            return direct
        }
        val viaInterface = findViaInterface(msgCls)
        return if (viaInterface.found) {
            // Store the found route for later direct use.
            @Suppress("UNCHECKED_CAST")
            routes[msgCls as Class<M>] = viaInterface.route!!
            viaInterface
        } else {
            notFound(msgCls)
        }
    }

    private fun findDirect(msgCls: Class<out M>): Match {
        val route = routes[msgCls]
        if (route != null) {
            return Match(msgCls, msgCls, route)
        }
        return notFound(msgCls)
    }

    private fun findViaInterface(msgCls: Class<out M>): Match {
        val result = routes.keys
            .filter { c -> c.isInterface }
            .find { iface -> iface.isAssignableFrom(msgCls) }
            ?.let { iface ->
                Match(msgCls, iface, routes[iface])
            }
            ?: (notFound(msgCls))
        return result
    }

    /**
     * Removes a route for the passed message class.
     *
     * @throws IllegalStateException if a custom route for this class was not previously set.
     */
    public fun remove(messageClass: Class<out M>) {
        if (!routes.containsKey(messageClass)) {
            error(
                "Cannot remove the route for the message class `${messageClass.name}`:" +
                        " a custom route was not previously set.",
            )
        }
        routes.remove(messageClass)
    }

    /**
     * Obtains IDs of entities to which the passed message should be delivered.
     *
     * If there is no function for the passed message, applies the default function.
     *
     * @param message The message to route.
     * @param context The context of the message.
     * @return the set of entity IDs to which the message should be delivered.
     */
    override fun apply(message: M, context: C): R {
        val cls = message.javaClass
        val match = routeFor(cls)
        if (match.found) {
            val func = match.route!!
            val result = func.apply(message, context)
            return result
        }
        val result = defaultRoute().apply(message, context)
        return result
    }

    /**
     * Provides the result of finding a route in the routing schema.
     *
     * @param requestedClass The class of the message which needs to be routed.
     * @param entryClass The type through which the route is found.
     *   Can be a class (for the [isDirect] match) or a super-interface
     *   of the requested class.
     *   Is `null` if there is no routing found for the [requestedClass].
     * @param route The routing function or `null` if there is no route defined neither
     *   for the class nor a super-interface of the class.
     */
    internal inner class Match(
        private val requestedClass: Class<out M>,
        val entryClass: Class<out M>?,
        val route: RouteFn<M, C, R>?
    ) {
        val found: Boolean = route != null

        /**
         * Returns `true` if the routing was defined directly for the requested class,
         * otherwise `false`.
         */
        val isDirect: Boolean = requestedClass == entryClass
    }

    private fun notFound(requestedClass: Class<out M>): Match = Match(requestedClass, null, null)
}
