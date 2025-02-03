/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.base.MessageContext;
import io.spine.core.EventContext;
import io.spine.protobuf.AnyPacker;
import io.spine.system.server.event.EntityStateChanged;

import java.util.Set;

/**
 * A routing schema used to deliver entity state updates.
 *
 * <p>A routing schema consists of a default route and custom routes per entity state class.
 * When calculating targets to be notified on the updated state, {@code StateUpdateRouting} would
 * see if there is a custom route set for the type of the entity state.
 * If not found, the default route will be
 * {@linkplain MessageRouting#apply(Message, MessageContext) applied}.
 *
 * @param <I>
 *         the type of the entity IDs to which the updates are routed
 */
public final class StateUpdateRouting<I>
        extends MessageRouting<EntityState<?>, EventContext, Set<I>> {

    private static final long serialVersionUID = 0L;

    /**
     * Creates a new {@code StateUpdateRouting}.
     *
     * <p>The resulting routing schema has the ignoring default route, i.e. if a custom route is not
     * set, the entity state update is ignored.
     *
     * @param <I>
     *         the type of the entity IDs to which the updates are routed
     * @param idClass
     *         the class of identifiers served by this routing
     * @return new {@code StateUpdateRouting}
     */
    public static <I> StateUpdateRouting<I> newInstance(Class<I> idClass) {
        return new StateUpdateRouting<>(idClass);
    }

    private StateUpdateRouting(Class<I> idClass) {
        super(DefaultStateRoute.newInstance(idClass));
    }

    /**
     * Verifies if the passed state type can be routed by a custom route, or
     * the message has a field matching the type of identifiers served by this routing.
     */
    @Override
    public boolean supports(Class<? extends EntityState<?>> stateType) {
        var customRouteSet = super.supports(stateType);
        @SuppressWarnings({"unchecked", "RedundantSuppression"}) // cast to the type used in ctor.
        var defaultRoute = (DefaultStateRoute<I>) defaultRoute();
        var defaultRouteAvailable = defaultRoute.supports(stateType);
        return customRouteSet || defaultRouteAvailable;
    }

    /**
     * Sets a custom route for the passed entity state class.
     *
     * <p>If there is no specific route for the class of the passed entity state, the routing will
     * use the {@linkplain #defaultRoute() default route}.
     *
     * @param stateClass
     *         the class of entity states to route
     * @param via
     *         the instance of the route to be used
     * @param <S>
     *         the type of the entity state message
     * @return {@code this} to allow chained calls when configuring the routing
     * @throws IllegalStateException
     *         if the route for this class is already set
     */
    @CanIgnoreReturnValue
    public <S extends EntityState<?>>
    StateUpdateRouting<I> route(Class<S> stateClass, StateUpdateRoute<I, S> via)
            throws IllegalStateException {
        @SuppressWarnings("unchecked") // Logically valid.
        var route = (RouteFn<EntityState<?>, EventContext, Set<I>>) via;
        addRoute(stateClass, route);
        return this;
    }

    /**
     * Creates an {@link EventRoute} for {@link EntityStateChanged} events based on this routing.
     *
     * <p>The entity state is extracted from the event and passed to this routing schema.
     *
     * @return event route for {@link EntityStateChanged} events
     */
    EventRoute<I, EntityStateChanged> eventRoute() {
        return (event, context) -> {
            @SuppressWarnings("unchecked")      // `EntityStateChanged` passes the `EntityState`s.
            var state = (EntityState<I>) AnyPacker.unpack(event.getNewState());
            return apply(state, context);
        };
    }
}
