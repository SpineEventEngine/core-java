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

package io.spine.server.model;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.errorprone.annotations.Immutable;
import io.spine.logging.Logging;
import io.spine.server.type.EmptyClass;
import io.spine.type.MessageClass;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static io.spine.server.model.MethodScan.findMethodsBy;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Provides mapping from a class of messages to methods which handle such messages.
 *
 * @param <M>
 *         the type of messages
 * @param <R>
 *         the type of message classes produced by handler methods
 * @param <H>
 *         the type of handler methods
 */
@Immutable(containerOf = {"M", "H"})
public final class HandlerMap<M extends MessageClass<?>,
                              R extends MessageClass<?>,
                              H extends HandlerMethod<?, M, ?, R>>
        implements Serializable, Logging {

    private static final Joiner METHOD_LIST_JOINER = Joiner.on(System.lineSeparator() + ',');
    private static final long serialVersionUID = 0L;

    private final ImmutableSetMultimap<DispatchKey, H> map;
    private final ImmutableSet<M> messageClasses;

    /**
     * Creates a map of methods found in the passed class.
     *
     * @param declaringClass
     *         the class to inspect
     * @param signature
     *         the signature of methods
     * @return new {@code MessageHandlerMap} of methods of the given class matching the given
     *         signature
     */
    public static <M extends MessageClass<?>,
                   P extends MessageClass<?>,
                   H extends HandlerMethod<?, M, ?, P>>
    HandlerMap<M, P, H> create(Class<?> declaringClass, MethodSignature<H, ?> signature) {
        checkNotNull(declaringClass);
        checkNotNull(signature);
        ImmutableSetMultimap<DispatchKey, H> map = findMethodsBy(declaringClass, signature);
        ImmutableSet<M> messageClasses = messageClasses(map.values());
        return new HandlerMap<>(map, messageClasses);
    }

    private HandlerMap(ImmutableSetMultimap<DispatchKey, H> map,
                       ImmutableSet<M> messageClasses) {
        this.map = map;
        this.messageClasses = messageClasses;
    }

    /**
     * Obtains classes of messages for which handlers are stored in this map.
     */
    public Set<M> messageClasses() {
        return messageClasses;
    }

    /**
     * Returns {@code true} if the handler map contains a method that handles the passed class
     * of messages, {@code false} otherwise.
     */
    public boolean containsClass(M messageClass) {
        return messageClasses.contains(messageClass);
    }

    /**
     * Obtains classes of messages which handlers satisfy the passed {@code predicate}.
     *
     * @param predicate
     *         a predicate for handler methods to filter the corresponding message classes
     */
    public ImmutableSet<M> messageClasses(Predicate<? super H> predicate) {
        Multimap<DispatchKey, H> filtered = Multimaps.filterValues(map, predicate::test);
        return messageClasses(filtered.values());
    }

    /**
     * Obtains the classes of messages produced by the handler methods in this map.
     */
    public ImmutableSet<R> producedTypes() {
        ImmutableSet<R> result = map
                .values()
                .stream()
                .map(HandlerMethod::producedMessages)
                .flatMap(Set::stream)
                .collect(toImmutableSet());
        return result;
    }

    /**
     * Obtains the method for handling by the passed key.
     *
     * @param key
     *         the key of the handler to get
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no method found in the map
     */
    private ImmutableSet<H> handlersOf(DispatchKey key) {
        ImmutableSet<H> handlers = map.get(key);
        checkState(!handlers.isEmpty(),
                   "Unable to find handler with the key: %s.", key);
        return handlers;
    }

    /**
     * Obtains methods for handling messages of the given class and with the given origin.
     *
     * <p>If there is no handler matching both the message and origin class, handlers will be
     * searched by the message class only.
     *
     * @param messageClass
     *         the message class of the handled message
     * @param originClass
     *         the class of the message, from which the handled message is originate
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no method found in the map
     */
    public ImmutableSet<H> handlersOf(M messageClass, MessageClass<?> originClass) {
        DispatchKey key =
                originClass.equals(EmptyClass.instance())
                ? new DispatchKey(messageClass.value(), null, null)
                : new DispatchKey(messageClass.value(), null, originClass.value());
        // If we have a handler with origin type, use the key. Otherwise, find handlers only
        // by the first parameter.
        DispatchKey presentKey = map.containsKey(key)
                                 ? key
                                 : new DispatchKey(messageClass.value(), null, null);
        return handlersOf(presentKey);
    }

    /**
     * Obtains a single handler method for messages of the given class and with the given origin.
     *
     * <p>If there is no handler matching both the message and origin class, a handler will be
     * searched by the message class only.
     *
     * <p>If there is no such method or several such methods, an {@link IllegalStateException} is
     * thrown.
     *
     * @param messageClass
     *         the message class of the handled message
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no such method or several such methods found in the map
     */
    public H handlerOf(M messageClass, MessageClass originClass) {
        ImmutableSet<H> methods = handlersOf(messageClass, originClass);
        return singleMethod(methods, messageClass);
    }

    /**
     * Obtains methods for handling messages of the given class.
     *
     * @param messageClass
     *         the message class of the handled message
     * @return handlers method
     * @throws IllegalStateException
     *         if there is no method found in the map
     */
    public ImmutableSet<H> handlersOf(M messageClass) {
        return handlersOf(messageClass, EmptyClass.instance());
    }

    /**
     * Obtains a single handler method for messages of the given class.
     *
     * <p>If there is no such method or several such methods, an {@link IllegalStateException}
     * is thrown.
     *
     * @param messageClass
     *         the message class of the handled message
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no such method or several such methods found in the map
     */
    public H handlerOf(M messageClass) {
        ImmutableSet<H> methods = handlersOf(messageClass);
        return singleMethod(methods, messageClass);
    }

    private H singleMethod(Collection<H> handlers, M targetType) {
        checkSingle(handlers, targetType);
        H handler = getOnlyElement(handlers);
        return handler;
    }

    private void checkSingle(Collection<H> handlers, M targetType) {
        int count = handlers.size();
        if (count == 0) {
            _error().log("No handler method found for the type `%s`.", targetType);
            throw newIllegalStateException(
                    "Unexpected number of handlers for messages of class %s: %d.%n%s",
                    targetType, count, handlers
            );
        } else if (count > 1) {
            /*
              The map should have found all the duplicates during construction.
              This is a fail-safe execution branch which ensures that no changes in the `HandlerMap`
              implementation corrupt the model.
            */
            _error().log(
                    "There are %d handler methods found for the type `%s`." +
                            "Expected only one method, but got:%n%s",
                    count, targetType, METHOD_LIST_JOINER.join(handlers)
            );
            throw new DuplicateHandlerMethodError(handlers);
        }
    }

    private static <M extends MessageClass<?>, H extends HandlerMethod<?, M, ?, ?>>
    ImmutableSet<M> messageClasses(Collection<H> handlerMethods) {
        ImmutableSet<M> result =
                handlerMethods.stream()
                              .map(HandlerMethod::messageClass)
                              .collect(toImmutableSet());
        return result;
    }
}
