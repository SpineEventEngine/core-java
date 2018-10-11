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

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import io.spine.type.MessageClass;

import java.util.HashMap;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A routing schema for a kind of messages such as commands, events, rejections, or documents.
 *
 * <p>A routing schema consists of a default route and custom routes per message class.
 *
 * @param <M> the type of the message to route
 * @param <C> the type of message context objects
 * @param <K> the type of message class objects such as {@link io.spine.core.EventClass EventClass}
 *            or {@link io.spine.core.CommandClass CommandClass}
 * @param <R> the type returned by the {@linkplain Route#apply(Message, Message) routing function}
 * @author Alexander Yevsyukov
 */
abstract class MessageRouting<M extends Message, C extends Message, K extends MessageClass, R>
        implements Route<M, C, R> {

    private static final long serialVersionUID = 0L;

    @SuppressWarnings("CollectionDeclaredAsConcreteClass") // We need a serializable field.
    private final HashMap<K, Route<M, C, R>> routes = Maps.newHashMap();

    /** The default route to be used if there is no matching entry set in {@link #routes}. */
    private Route<M, C, R> defaultRoute;

    MessageRouting(Route<M, C, R> defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    /**
     * Obtains the default route used by the schema.
     */
    protected Route<M, C, R> getDefault() {
        return defaultRoute;
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     */
    MessageRouting<M, C, K, R> replaceDefault(Route<M, C, R> newDefault) {
        checkNotNull(newDefault);
        defaultRoute = newDefault;
        return this;
    }

    /**
     * Creates an instance of {@link MessageClass} by the passed class of messages.
     */
    abstract K toMessageClass(Class<? extends M> classOfMessages);

    /**
     * Creates an instance of {@link MessageClass} by the passed outer message object.
     */
    abstract K toMessageClass(Message outerOrMessage);

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
     * the {@linkplain #getDefault() default route}.
     *
     * @param messageClass the class of messages to route
     * @param via          the instance of the route to be used
     * @throws IllegalStateException if the route for this message class is already set
     */
    void doRoute(Class<? extends M> messageClass, Route<M, C, R> via)
            throws IllegalStateException {
        checkNotNull(messageClass);
        checkNotNull(via);
        Optional route = doGet(messageClass);
        if (route.isPresent()) {
            throw newIllegalStateException(
                    "The route for the message class %s already set. " +
                            "Please remove the route (%s) before setting new route.",
                    messageClass.getName(), route.get());
        }
        K cls = toMessageClass(messageClass);
        routes.put(cls, via);
    }

    /**
     * Obtains a route for the passed message class.
     *
     * @param msgCls the class of the messages
     * @return optionally available route
     */
    Optional<? extends Route<M, C, R>> doGet(Class<? extends M> msgCls) {
        checkNotNull(msgCls);
        K cls = toMessageClass(msgCls);
        Route<M, C, R> route = routes.get(cls);
        return Optional.ofNullable(route);
    }

    /**
     * Removes a route for the passed message class.
     *
     * @throws IllegalStateException if a custom route for this message class was not previously set
     */
    public void remove(Class<? extends M> messageClass) {
        checkNotNull(messageClass);
        K cls = toMessageClass(messageClass);
        if (!routes.containsKey(cls)) {
            throw newIllegalStateException("Cannot remove the route for the message class (%s):" +
                                                   " a custom route was not previously set.",
                                           messageClass.getName());
        }
        routes.remove(cls);
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
        K messageClass = toMessageClass(message);
        Route<M, C, R> func = routes.get(messageClass);
        if (func != null) {
            R result = func.apply(message, context);
            return result;
        }
        R result = getDefault().apply(message, context);
        return result;
    }
}
