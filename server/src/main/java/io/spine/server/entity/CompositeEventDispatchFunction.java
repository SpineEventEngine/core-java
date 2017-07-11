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

package io.spine.server.entity;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.server.entity.idfunc.EventDispatchFunction;

import java.util.HashMap;
import java.util.Set;

/**
 * Helper class for managing {@link EventDispatchFunction}s associated with
 * a repository that dispatches events to its entities.
 *
 * @param <I> the type of the entity IDs of this repository
 * @author Alexander Yevsyukov
 */
public final class CompositeEventDispatchFunction<I> implements EventDispatchFunction<I, Message> {

    private static final long serialVersionUID = 0L;

    /**
     * The map from event class to a function that generates a set of project IDs
     * for the corresponding event.
     */
    @SuppressWarnings("CollectionDeclaredAsConcreteClass") // need a serializable field.
    private final HashMap<EventClass, EventDispatchFunction<I, Message>> map = Maps.newHashMap();

    /** The function used when there's no matching entry in the map. */
    private final EventDispatchFunction<I, Message> defaultFn;

    public static <I>
    CompositeEventDispatchFunction<I> withDefault(EventDispatchFunction<I, Message> defaultFn) {
        return new CompositeEventDispatchFunction<>(defaultFn);
    }

    /**
     * Creates new instance with the passed default function.
     *
     * @param defaultFn the function which used when there is no matching entry in the map
     */
    private CompositeEventDispatchFunction(EventDispatchFunction<I, Message> defaultFn) {
        this.defaultFn = defaultFn;
    }

    /**
     * Removes a function for the passed event class.
     */
    <E extends Message> void remove(Class<E> eventClass) {
        final EventClass clazz = EventClass.of(eventClass);
        map.remove(clazz);
    }

    /**
     * Finds a function for the passed event and applies it.
     *
     * <p>If there is no function for the passed event applies the default function.
     *
     * @param event   the event message
     * @param context the event context
     * @return the set of entity IDs
     */
    @Override
    public Set<I> apply(Message event, EventContext context) {
        final EventClass eventClass = EventClass.of(event);
        final EventDispatchFunction<I, Message> func = map.get(eventClass);
        if (func != null) {
            final Set<I> result = func.apply(event, context);
            return result;
        }

        final Set<I> result = defaultFn.apply(event, context);
        return result;
    }

    /**
     * Puts a function into the map.
     *
     * @param eventClass the class of the event handled by the function
     * @param func       the function instance
     * @param <E>        the type of the event message
     */
    <E extends Message> void put(Class<E> eventClass, EventDispatchFunction<I, E> func) {
        final EventClass clazz = EventClass.of(eventClass);

        @SuppressWarnings("unchecked")
        // since we want to store {@code IdSetFunction}s for various event types.
        final EventDispatchFunction<I, Message> casted = (EventDispatchFunction<I, Message>) func;
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
    <E extends Message> Optional<EventDispatchFunction<I, E>> get(Class<E> eventClass) {
        final EventClass clazz = EventClass.of(eventClass);
        final EventDispatchFunction<I, Message> func = map.get(clazz);

        @SuppressWarnings("unchecked")  // we ensure the type when we put into the map.
        final EventDispatchFunction<I, E> result = (EventDispatchFunction<I, E>) func;
        return Optional.fromNullable(result);
    }
}
