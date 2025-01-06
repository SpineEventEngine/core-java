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

package io.spine.server.route;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.MessageContext;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;

/**
 * A routing schema for a kind of messages such as commands, events, rejections, or documents.
 *
 * <p>A routing schema consists of a default route and custom routes per message class.
 *
 * @param <M>
 *         the type of the message to route
 * @param <C>
 *         the type of message context objects
 * @param <R>
 *         the type returned by the {@linkplain RouteFn#apply(Message, Message) routing function}
 */
public abstract class MessageRouting<M extends Message, C extends MessageContext, R>
        implements RouteFn<M, C, R> {

    private static final long serialVersionUID = 0L;

    /**
     * Map of currently known routes.
     *
     * @implNote This collection is made {@code synchronized}, since in some cases it is
     *         being simultaneously read and modified in different threads.
     *         In particular, the modification at run-time is performed when storing
     *         the routes discovered on a per-interface basis.
     *         Therefore, if this collection is not {@code synchronized},
     *         a {@code ConcurrentModificationException} is sometimes thrown.
     */
    private final Map<Class<? extends M>, RouteFn<M, C, R>> routes =
            synchronizedMap(new LinkedHashMap<>());

    /** The default route to be used if there is no matching entry set in {@link #routes}. */
    private RouteFn<M, C, R> defaultRoute;

    MessageRouting(RouteFn<M, C, R> defaultRoute) {
        this.defaultRoute = checkNotNull(defaultRoute);
    }

    /**
     * Obtains the default route used by the schema.
     */
    protected RouteFn<M, C, R> defaultRoute() {
        return defaultRoute;
    }

    /**
     * Sets a new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     */
    @CanIgnoreReturnValue
    MessageRouting<M, C, R> replaceDefault(RouteFn<M, C, R> newDefault) {
        checkNotNull(newDefault);
        defaultRoute = newDefault;
        return this;
    }

    /**
     * Checks if the passed message type is supported by this instance of routing.
     */
    public boolean supports(Class<? extends M> messageType) {
        checkNotNull(messageType);
        var match = routeFor(messageType);
        var result = match.found();
        return result;
    }

    /**
     * Sets a custom route for the passed message type.
     *
     * <p>The type can be either a class or interface. If the routing schema already contains an
     * entry with the same type or a super-interface of the passed type
     * an {@link IllegalStateException} will be thrown.
     *
     * <p>In order to provide a mapping for a specific class <em>and</em> an interface common
     * to this and other message classes, please add the routing for the class <em>before</em>
     * the interface.
     *
     * @param messageType
     *         the type of messages to route
     * @param via
     *         the instance of the route to be used
     * @throws IllegalStateException
     *         if the route for this message class is already set either directly or
     *         via a super-interface
     */
    void addRoute(Class<? extends M> messageType, RouteFn<M, C, R> via)
            throws IllegalStateException {
        checkNotNull(messageType);
        checkNotNull(via);
        var match = routeFor(messageType);
        if (match.found()) {
            var requestedClass = messageType.getName();
            var entryClass = match.entryClass()
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
                                "the class `%s`, please put it before the routing for " +
                                "the super-interface.",
                        requestedClass, entryClass, requestedClass);
            }
        }
        routes.put(messageType, via);
    }

    /**
     * Obtains a route for the passed message class.
     *
     * @param msgCls the class of the messages
     * @return optionally available route
     */
    Match routeFor(Class<? extends M> msgCls) {
        checkNotNull(msgCls);
        var direct = findDirect(msgCls);
        if (direct.found()) {
            return direct;
        }

        var viaInterface = findViaInterface(msgCls);
        if (viaInterface.found()) {
            // Store the found route for later direct use.
            routes.put(msgCls, viaInterface.route());
            return viaInterface;
        }

        return new Match(msgCls, null, null);
    }

    private Match findDirect(Class<? extends M> msgCls) {
        var route = routes.get(msgCls);
        if (route != null) {
            return new Match(msgCls, msgCls, route);
        }
        return new Match(msgCls, null, null);
    }

    private Match findViaInterface(Class<? extends M> msgCls) {
        var result = routes.keySet()
                .stream()
                .filter(Class::isInterface)
                .filter(iface -> iface.isAssignableFrom(msgCls))
                .findFirst()
                .map(iface -> new Match(msgCls, iface, routes.get(iface)))
                .orElse(new Match(msgCls, null, null));
        return result;
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
                    "Cannot remove the route for the message class (`%s`):" +
                            " a custom route was not previously set.",
                    messageClass.getName());
        }
        routes.remove(messageClass);
    }

    /**
     * Obtains IDs of entities to which the passed message should be delivered.
     *
     * <p>If there is no function for the passed message applies the default function.
     *
     * @param message the message
     * @param context the message context
     * @return the set of entity IDs to which the message should be delivered
     */
    @Override
    public R apply(M message, C context) {
        checkNotNull(message);
        checkNotNull(context);
        @SuppressWarnings("unchecked") var
        cls = (Class<? extends M>) message.getClass();
        var match = routeFor(cls);
        if (match.found()) {
            var func = match.route();
            var result = func.apply(message, context);
            return result;
        }
        var result = defaultRoute().apply(message, context);
        return result;
    }

    /**
     * Provides information on routing availability.
     */
    final class Match {

        private final Class<? extends M> requestedClass;
        private final @Nullable RouteFn<M, C, R> route;
        private final @Nullable Class<? extends M> entryClass;

        /**
         * Creates new instance.
         *
         * @param requestedClass
         *         the class of the message which needs to be routed
         * @param entryType
         *         the type through which the route is found.
         *         Can be a class (for the {@link #direct()} match) or a super-interface
         *         of the requested class.
         *         Is {@code null} if there is no routing found for the {@code requestedClass}.
         * @param route
         *         the routing function or {@code null} if there is no route defined neither
         *         for the class or a super-interface of the class
         */
        private Match(Class<? extends M> requestedClass,
                      @Nullable Class<? extends M> entryType,
                      @Nullable RouteFn<M, C, R> route) {
            this.requestedClass = requestedClass;
            this.route = route;
            this.entryClass = entryType;
        }

        boolean found() {
            return route != null;
        }

        /**
         * Returns {@code true} if the routing was defined directly for the requested class,
         * otherwise {@code false}.
         */
        boolean direct() {
            return requestedClass.equals(entryClass);
        }

        Class<? extends M> entryClass() {
            return requireNonNull(entryClass);
        }

        RouteFn<M, C, R> route() {
            return requireNonNull(route);
        }
    }
}
