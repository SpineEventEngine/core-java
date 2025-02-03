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

import com.google.errorprone.annotations.CanIgnoreReturnValue
import io.spine.base.EntityState
import io.spine.core.EventContext
import io.spine.protobuf.AnyPacker
import io.spine.system.server.event.EntityStateChanged

/**
 * A routing schema used to deliver entity state updates.
 *
 * A routing schema consists of a default route and custom routes per entity state class.
 * When calculating targets to be notified on the updated state, `StateUpdateRouting` would
 * see if there is a custom route set for the type of the entity state.
 * If not found, the default route will be [applied][MessageRouting.apply].
 *
 * @param I The type of the entity IDs to which the updates are routed.
 * @param idClass The class of entity identifiers to which entity states are routed.
 */
public class StateUpdateRouting<I> private constructor(
    idClass: Class<I>
) : MessageRouting<EntityState<*>, EventContext, Set<I>>(
    DefaultStateRoute.newInstance(idClass)
) {
    /**
     * Verifies if the passed state type can be routed by a custom route, or
     * the message has a field matching the type of identifiers served by this routing.
     */
    override fun supports(stateType: Class<out EntityState<*>>): Boolean {
        val customRouteSet = super.supports(stateType)
        @Suppress("UNCHECKED_CAST") // cast to the type used in ctor.
        val defaultRoute = defaultRoute() as DefaultStateRoute<I>
        val defaultRouteAvailable = defaultRoute.supports(stateType)
        return customRouteSet || defaultRouteAvailable
    }

    /**
     * Sets a custom route for the passed entity state class.
     *
     * If there is no specific route for the class of the passed entity state,
     * the routing will use the [defaultRoute].
     *
     * @param S The type of the entity state message
     *
     * @param stateClass The class of entity states to route.
     * @param via The instance of the route to be used.
     * @return `this` to allow chained calls when configuring the routing.
     * @throws IllegalStateException if the route for this class is already set.
     */
    @CanIgnoreReturnValue
    public fun <S : EntityState<*>> route(
        stateClass: Class<S>,
        via: StateUpdateRoute<I, S>
    ): StateUpdateRouting<I> {
        @Suppress("UNCHECKED_CAST") // Logically valid.
        val route = via as RouteFn<EntityState<*>, EventContext, Set<I>>
        addRoute(stateClass, route)
        return this
    }

    /**
     * Creates an [EventRoute] for [EntityStateChanged] events based on this routing.
     *
     * The entity state is extracted from the event and passed to this routing schema.
     *
     * @return event route for [EntityStateChanged] events.
     */
    public fun eventRoute(): EventRoute<I, EntityStateChanged> {
        return EventRoute { event: EntityStateChanged, context: EventContext ->
            @Suppress("UNCHECKED_CAST") // `EntityStateChanged` passes the `EntityState`s.
            val state = AnyPacker.unpack(event.newState) as EntityState<I>
            apply(state, context)
        }
    }

    public companion object {

        /**
         * Creates a new `StateUpdateRouting`.
         *
         * The resulting routing schema has the ignoring default route,
         * i.e., if a custom route is not set, the entity state update is ignored.
         *
         * @param I The type of the entity IDs to which the updates are routed.
         * @param idClass The class of identifiers served by this routing.
         * @return new `StateUpdateRouting`.
         */
        @JvmStatic
        public fun <I> newInstance(idClass: Class<I>): StateUpdateRouting<I> =
            StateUpdateRouting(idClass)
    }
}
