/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;

import java.util.HashMap;
import java.util.Set;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A routing schema used by an {@link io.spine.server.event.EventDispatcher EventDispatcher} for
 * delivering events.
 *
 * <p>A routing schema consists of a default route and custom routes per event class. When
 * calculating a set of event targets, {@code EventRouting} would see if there is a custom route
 * set for the type of the event. If not found, the default route will be
 * {@linkplain EventRoute#apply(Message, Message) applied}.
 *
 * @param <I> the type of the entity IDs of this repository
 * @author Alexander Yevsyukov
 */
public final class EventRouting<I> implements EventRoute<I, Message> {

    private static final long serialVersionUID = 0L;

    /**
     * The map from event class to a function that generates a set of project IDs
     * for the corresponding event.
     */
    @SuppressWarnings("CollectionDeclaredAsConcreteClass") // need a serializable field.
    private final HashMap<EventClass, EventRoute<I, Message>> map = Maps.newHashMap();

    /** The function used when there's no matching entry in the map. */
    private final EventRoute<I, Message> defaultRoute;

    public static <I> EventRouting<I> withDefault(EventRoute<I, Message> defaultRoute) {
        return new EventRouting<>(defaultRoute);
    }

    /**
     * Creates new instance with the passed default function.
     *
     * @param defaultRoute the function which used when there is no matching entry in the map
     */
    private EventRouting(EventRoute<I, Message> defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    /**
     * Obtains the default route used by the schema.
     */
    public EventRoute<I, Message> getDefault() {
        return defaultRoute;
    }

    /**
     * Sets a custom route for the passed event class.
     *
     * <p>Typical usage for this method would be in a constructor of a {@code ProjectionRepository}
     * to provide mapping between events to projection identifiers.
     *
     * <p>Such a mapping may be required when...
     * <ul>
     * <li>An event should be matched to more than one projection.
     * <li>The type of an event producer ID (stored in {@code EventContext})
     * differs from {@code <I>}.
     * </ul>
     *
     * <p>If there is no function for the class of the passed event message,
     * the repository will use the event producer ID from an {@code EventContext} passed
     * with the event message.
     *
     * @param eventClass the class of the event handled by the function
     * @param route      the function instance
     * @param <E>        the type of the event message
     * @throws IllegalStateException if the route for this event class is already set
     */
    public <E extends Message> void set(Class<E> eventClass, EventRoute<I, E> route)
            throws IllegalStateException {
        final EventClass clazz = EventClass.of(eventClass);

        final Optional<EventRoute<I, E>> alreadySet = get(eventClass);
        if (alreadySet.isPresent()) {
            throw newIllegalStateException(
                    "The route for event class %s already set. " +
                    "Please remove the route (%s) before setting new route.",
                    eventClass.getName(), alreadySet.get());
        }

        @SuppressWarnings("unchecked")
        // since we want to store {@code IdSetFunction}s for various event types.
        final EventRoute<I, Message> casted = (EventRoute<I, Message>) route;
        map.put(clazz, casted);
    }

    /**
     * Obtains a function for the passed event class.
     *
     * @param eventClass the class of the event message
     * @param <E>        the type of the event message
     * @return the function wrapped into {@code Optional} or empty {@code Optional}
     * if there is no matching function
     */
    public <E extends Message> Optional<EventRoute<I, E>> get(Class<E> eventClass) {
        final EventClass clazz = EventClass.of(eventClass);
        final EventRoute<I, Message> route = map.get(clazz);

        @SuppressWarnings("unchecked")  // we ensure the type when we put into the map.
        final EventRoute<I, E> result = (EventRoute<I, E>) route;
        return Optional.fromNullable(result);
    }

    /**
     * Removes a function for the passed event class.
     *
     * @throws IllegalStateException if a custom route for this event class was not previously
     * {@linkplain #set(Class, EventRoute) set}.
     */
    public <E extends Message> void remove(Class<E> eventClass) {
        final EventClass cls = EventClass.of(eventClass);
        if (!map.containsKey(cls)) {
            throw newIllegalStateException("Cannot remove the route for the event class (%s):" +
                                                   " a custom route was not previously set.",
                                           eventClass.getName());
        }
        map.remove(cls);
    }

    /**
     * Finds a function for the passed event and applies it.
     *
     * <p>If there is no function for the passed event applies the default function.
     *
     * @param event   the event message
     * @param context the event context
     * @return the set of entity IDs
     * @throws IllegalStateException if the route for this event class is already set
     */
    @Override
    public Set<I> apply(Message event, EventContext context) throws IllegalStateException {
        final EventClass eventClass = EventClass.of(event);
        final EventRoute<I, Message> func = map.get(eventClass);
        if (func != null) {
            final Set<I> result = func.apply(event, context);
            return result;
        }

        final Set<I> result = defaultRoute.apply(event, context);
        return result;
    }
}
