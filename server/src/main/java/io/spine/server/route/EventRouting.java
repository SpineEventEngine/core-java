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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.EventContext;
import io.spine.system.server.event.EntityStateChanged;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A routing schema used to deliver events.
 *
 * <p>A routing schema consists of a default route and custom routes per event class.
 * When calculating a set of event targets, {@code EventRouting} will see if there is
 * a custom route set for the type of the event. If not found, the default route will be
 * {@linkplain EventRoute#apply(Message, Message) applied}.
 *
 * @param <I> the type of the entity IDs to which events are routed
 */
public final class EventRouting<I>
        extends MessageRouting<EventMessage, EventContext, Set<I>>
        implements EventRoute<I, EventMessage> {

    private static final long serialVersionUID = 0L;

    /**
     * Creates a new instance with the passed default route.
     *
     * @param defaultRoute
     *        the route to use if a custom one is not {@linkplain #route(Class, EventRoute) set}
     */
    private EventRouting(EventRoute<I, EventMessage> defaultRoute) {
        super(defaultRoute);
    }

    /**
     * Creates a new event routing with the passed default route.
     *
     * @param defaultRoute
     *         the default route
     * @param <I>
     *         the type of entity identifiers returned by new routing
     * @return new routing instance
     */
    @CanIgnoreReturnValue
    public static <I> EventRouting<I> withDefault(EventRoute<I, EventMessage> defaultRoute) {
        checkNotNull(defaultRoute);
        return new EventRouting<>(defaultRoute);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    public final EventRoute<I, EventMessage> defaultRoute() {
        return (EventRoute<I, EventMessage>) super.defaultRoute();
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     * @return {@code this} to allow chained calls when configuring the routing
     */
    @CanIgnoreReturnValue
    public EventRouting<I> replaceDefault(EventRoute<I, EventMessage> newDefault) {
        return (EventRouting<I>) super.replaceDefault(newDefault);
    }

    /**
     * Sets a custom route for the passed event type.
     *
     * <p>Such mapping may be required for the following cases:
     * <ul>
     *   <li>An event message should be matched to more than one entity, for example, several
     *   projections updated in response to one event.
     *   <li>The type of an event producer ID (stored in the event context) differs from the type
     *   of entity identifiers ({@code <I>}.
     * </ul>
     *
     * <p>The type of the event can be a class or an interface. If a routing schema needs to contain
     * entries for specific classes and an interface that these classes implement, routes for
     * interfaces should be defined <em>after</em> entries for the classes:
     *
     * <pre>{@code
     * customRouting.route(MyEventClass.class, (event, context) -> { ... })
     *              .route(MyEventInterface.class, (event, context) -> { ... });
     * }</pre>
     *
     * Defining an entry for an interface and then for the class which implements the interface will
     * result in {@code IllegalStateException}.
     *
     * <p>If there is no specific route for an event type, the {@linkplain #defaultRoute()
     * default route} will be used.
     *
     * @param eventType
     *         the type of events to route
     * @param via
     *         the instance of the route to be used
     * @param <E>
     *         the type of the event message
     * @return {@code this} to allow chained calls when configuring the routing
     * @throws IllegalStateException
     *         if the route for this event type is already set either directly or
     *         via a super-interface
     */
    @CanIgnoreReturnValue
    public <E extends EventMessage>
    EventRouting<I> route(Class<E> eventType, EventRoute<I, ? super E> via)
            throws IllegalStateException {
        @SuppressWarnings("unchecked") // The cast is required to adapt the type to internal API.
        Route<EventMessage, EventContext, Set<I>> casted =
                (Route<EventMessage, EventContext, Set<I>>) via;
        addRoute(eventType, casted);
        return this;
    }

    /**
     * Sets a custom routing schema for entity state updates.
     *
     * @param routing
     *         the routing schema for entity state updates
     * @return {@code this} to allow chained calls when configuring the routing
     * @throws IllegalStateException
     *         if a route for {@link EntityStateChanged} is already set
     */
    @CanIgnoreReturnValue
    public EventRouting<I> routeStateUpdates(StateUpdateRouting<I> routing) {
        checkNotNull(routing);
        return route(EntityStateChanged.class, routing.eventRoute());
    }

    /**
     * Obtains a route for the passed event class.
     *
     * @param eventClass the class of the event messages
     * @param <M>        the type of the event message
     * @return optionally available route
     */
    public <M extends EventMessage> Optional<EventRoute<I, M>> get(Class<M> eventClass) {
        Match match = routeFor(eventClass);
        if (match.found()) {
            @SuppressWarnings({"unchecked", "RedundantSuppression"})
            // protected by generic params of this class
            Optional<EventRoute<I, M>> result = Optional.of((EventRoute<I, M>) match.route());
            return result;
        }
        return Optional.empty();
    }
}
