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

package io.spine.server.route;

import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.model.StateClass;
import io.spine.system.server.event.EntityStateChanged;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.route.EventRoute.noTargets;

/**
 * A routing schema used to deliver entity state updates.
 *
 * <p>A routing schema consists of a default route and custom routes per entity state class.
 * When calculating targets to be notified on the updated state, {@code StateUpdateRouting} would
 * see if there is a custom route set for the type of the entity state.
 * If not found, the default route will be
 * {@linkplain StateUpdateRouting#apply(Message, Message) applied}.
 *
 * @param <I>
 *         the type of the entity IDs to which the updates are routed
 */
public class StateUpdateRouting<I>
        extends MessageRouting<Message, EventContext, Set<I>> {

    private static final long serialVersionUID = 0L;

    private StateUpdateRouting() {
        super(defaultStateRoute());
    }

    /**
     * Creates a new {@code StateUpdateRouting}.
     *
     * <p>The resulting routing schema has the ignoring default route, i.e. if a custom route is not
     * set, the entity state update is ignored.
     *
     * @param <I>
     *         the type of the entity IDs to which the updates are routed
     * @return new {@code StateUpdateRouting}
     */
    public static <I> StateUpdateRouting<I> newInstance() {
        return new StateUpdateRouting<>();
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
    public <S extends Message>
    StateUpdateRouting<I> route(Class<S> stateClass, StateUpdateRoute<I, S> via)
            throws IllegalStateException {
        @SuppressWarnings("unchecked") // Logically valid.
        Route<Message, EventContext, Set<I>> route = (Route<Message, EventContext, Set<I>>) via;
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
            Message state = AnyPacker.unpack(event.getNewState());
            return apply(state, context);
        };
    }

    private static <I> Route<Message, EventContext, Set<I>> defaultStateRoute() {
        return (message, context) -> noTargets();
    }

    /**
     * Validates routing schema for types of state messages.
     *
     * @throws IllegalStateException
     *          if one of the state type cannot be dispatched by the current schema configuration
     */
    public void validate(Sets.SetView<StateClass> stateClasses) throws IllegalStateException {
        checkNotNull(stateClasses);
        //TODO:2019-05-15:alexander.yevsyukov: Implement
    }
}
