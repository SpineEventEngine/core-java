/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.base.Optional;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A routing schema used by an {@link io.spine.server.event.EventDispatcher EventDispatcher} for
 * delivering events.
 *
 * <p>A routing schema consists of a default route and custom routes per event class.
 * When calculating a set of event targets, {@code EventRouting} would see if there is
 * a custom route set for the type of the event. If not found, the default route will be
 * {@linkplain EventRoute#apply(Message, Message) applied}.
 *
 * @param <I> the type of the entity IDs to which events are routed
 * @author Alexander Yevsyukov
 */
public final class EventRouting<I> extends MessageRouting<EventContext, EventClass, Set<I>>
        implements EventRoute<I, Message> {

    private static final long serialVersionUID = 0L;

    /**
     * Creates new instance with the passed default route.
     *
     * @param defaultRoute
     *        the route to use if a custom one is not {@linkplain #route(Class, EventRoute) set}
     */
    private EventRouting(EventRoute<I, Message> defaultRoute) {
        super(defaultRoute);
    }

    /**
     * Creates a new event routing with the passed default route.
     *
     * @param defaultRoute the default route
     * @param <I>          the type of entity identifiers returned by new routing
     * @return new routing instance
     */
    @CanIgnoreReturnValue
    public static <I> EventRouting<I> withDefault(EventRoute<I, Message> defaultRoute) {
        checkNotNull(defaultRoute);
        return new EventRouting<>(defaultRoute);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    public final EventRoute<I, Message> getDefault() {
        return (EventRoute<I, Message>) super.getDefault();
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     * @return {@code this} to allow chained calls when configuring the routing
     */
    @CanIgnoreReturnValue
    public EventRouting<I> replaceDefault(EventRoute<I, Message> newDefault) {
        return (EventRouting<I>) super.replaceDefault(newDefault);
    }

    /**
     * Sets a custom route for the passed event class.
     *
     * <p>Such a mapping may be required when...
     * <ul>
     * <li>An an event message should be matched to more than one entity (e.g. several projections
     * updated in response to one event).
     * <li>The type of an event producer ID (stored in the event context) differs from the type
     * of entity identifiers ({@code <I>}.
     * </ul>
     *
     * <p>If there is no specific route for the class of the passed event, the routing will use
     * the {@linkplain #getDefault() default route}.
     *
     * @param eventClass the class of events to route
     * @param via        the instance of the route to be used
     * @param <E>        the type of the event message
     * @return {@code this} to allow chained calls when configuring the routing
     * @throws IllegalStateException if the route for this event class is already set
     */
    @CanIgnoreReturnValue
    public <E extends Message> EventRouting<I> route(Class<E> eventClass, EventRoute<I, E> via)
            throws IllegalStateException {
        @SuppressWarnings("unchecked") // The cast is required to adapt the type to internal API.
        Route<Message, EventContext, Set<I>> casted =
                (Route<Message, EventContext, Set<I>>) via;
        return (EventRouting<I>) doRoute(eventClass, casted);
    }

    /**
     * Obtains a route for the passed event class.
     *
     * @param eventClass the class of the event messages
     * @param <M>        the type of the event message
     * @return optionally available route
     */
    public <M extends Message> Optional<EventRoute<I, M>> get(Class<M> eventClass) {
        Optional<? extends Route<Message, EventContext, Set<I>>> optional = doGet(eventClass);
        if (optional.isPresent()) {
            EventRoute<I, M> route = (EventRoute<I, M>) optional.get();
            return Optional.of(route);
        }
        return Optional.absent();
    }

    /**
     * Creates {@link EventClass} by the passed class value.
     */
    @Override
    EventClass toMessageClass(Class<? extends Message> classOfEvents) {
        return EventClass.of(classOfEvents);
    }

    /**
     * Obtains the {@link EventClass} for the passed event or event message.
     */
    @Override
    EventClass toMessageClass(Message eventOrMessage) {
        return EventClass.of(eventOrMessage);
    }
}
