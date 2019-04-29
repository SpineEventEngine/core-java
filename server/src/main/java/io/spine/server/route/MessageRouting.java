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

import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A routing schema for a kind of messages such as commands, events, rejections, or documents.
 *
 * <p>A routing schema consists of a default route and custom routes per message class.
 *
 * @param <M> the type of the message to route
 * @param <C> the type of message context objects
 * @param <R> the type returned by the {@linkplain Route#apply(Message, Message) routing function}
 */
abstract class MessageRouting<M extends Message, C extends Message, R> implements Route<M, C, R> {

    private static final long serialVersionUID = 0L;

    private final Map<Class<? extends M>, Route<M, C, R>> routes = new LinkedHashMap<>();

    /** The default route to be used if there is no matching entry set in {@link #routes}. */
    private Route<M, C, R> defaultRoute;

    MessageRouting(Route<M, C, R> defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    /**
     * Obtains the default route used by the schema.
     */
    protected Route<M, C, R> defaultRoute() {
        return defaultRoute;
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     */
    MessageRouting<M, C, R> replaceDefault(Route<M, C, R> newDefault) {
        checkNotNull(newDefault);
        defaultRoute = newDefault;
        return this;
    }

    /**
     * Sets a custom route for the passed message class.
     *
     * <p>Such a mapping may be required when...
     * <ul>
     *     <li>A message should be matched to more than one entity.
     *     <li>The type of an message producer ID (stored in the message context) differs from the
     *         type of entity identifiers.
     * </ul>
     *
     * <p>If there is no specific route for the class of the passed message, the routing will use
     * the {@linkplain #defaultRoute() default route}.
     *
     * @param messageClass the class of messages to route
     * @param via          the instance of the route to be used
     * @throws IllegalStateException if the route for this message class is already set
     */
    void doRoute(Class<? extends M> messageClass, Route<M, C, R> via) throws IllegalStateException {
        checkNotNull(messageClass);
        checkNotNull(via);
        RoutingMatch match = routeFor(messageClass);
        if (match.found()) {
            String requestedClass = messageClass.getName();
            String entryClass = match.entryClass()
                                     .getName();
            if (match.direct()) {
                throw newIllegalStateException(
                        "The route for the message class `%s` already set. " +
                                "Please remove the route (`%s`) before setting new route.",
                        requestedClass, entryClass);
            } else {
                throw newIllegalStateException(
                        "The route for the message class `%s` already defined via " +
                                "the interface `%s`. If you want to have specific routing for " +
                                "the class `%s` please put it before the routing for " +
                                "the superinterface.",
                        requestedClass, entryClass, requestedClass);
            }
        }
        routes.put(messageClass, via);
    }

    /**
     * Obtains a route for the passed message class.
     *
     * @param msgCls the class of the messages
     * @return optionally available route
     */
    RoutingMatch routeFor(Class<? extends M> msgCls) {
        checkNotNull(msgCls);
        // Iterate keys to match either by equality or superinterface.
        for (Map.Entry<Class<? extends M>, Route<M, C, R>> entry : routes.entrySet()) {
            Class<? extends M> key = entry.getKey();
            if (key.isAssignableFrom(msgCls)) {
                return new RoutingMatch(msgCls, key, entry.getValue());
            }
        }
        return new RoutingMatch(msgCls, null, null);
    }

    /**
     * Removes a route for the passed message class.
     *
     * @throws IllegalStateException if a custom route for this message class was not previously set
     */
    public void remove(Class<? extends M> messageClass) {
        checkNotNull(messageClass);
        if (!routes.containsKey(messageClass)) {
            throw newIllegalStateException(
                    "Cannot remove the route for the message class (`%s`): " +
                            "a custom route was not previously set.",
                    messageClass.getName());
        }
        routes.remove(messageClass);
    }

    /**
     * Obtains IDs of entities to which the passed message should be delivered.
     *
     * <p>If there is no function for the passed message applies the default function.
     *
     * @param  message the message
     * @param  context the message context
     * @return the set of entity IDs to which the message should be delivered
     */
    @Override
    public R apply(M message, C context) {
        checkNotNull(message);
        checkNotNull(context);
        @SuppressWarnings("unchecked") Class<? extends M>
        cls = (Class<? extends M>) message.getClass();
        RoutingMatch match = routeFor(cls);
        if (match.found()) {
            Route<M, C, R> func = match.route();
            R result = func.apply(message, context);
            return result;
        }
        R result = defaultRoute().apply(message, context);
        return result;
    }

    /**
     * Provides information on routing availability.
     */
    final class RoutingMatch {

        private final Class<? extends M> requestedClass;
        private final @Nullable Route<M, C, R> route;
        private final @Nullable Class<? extends M> entryClass;

        private RoutingMatch(Class<? extends M> requestedClass,
                             @Nullable Class<? extends M> entryClass,
                             @Nullable Route<M, C, R> route) {
            this.requestedClass = requestedClass;
            this.route = route;
            this.entryClass = entryClass;
        }

        boolean found() {
            return route != null;
        }

        boolean direct() {
            return requestedClass.equals(entryClass);
        }

        Class<? extends M> entryClass() {
            return checkNotNull(entryClass);
        }

        Route<M, C, R> route() {
            return checkNotNull(route);
        }
    }
}
