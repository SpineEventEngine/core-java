/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.errorprone.annotations.Immutable;
import io.spine.logging.Logging;
import io.spine.server.type.EmptyClass;
import io.spine.server.type.EnvelopeWithOrigin;
import io.spine.server.type.SignalEnvelope;
import io.spine.type.MessageClass;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.server.model.MethodScan.findMethodsBy;
import static java.lang.String.format;
import static java.util.Comparator.comparing;

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
        var map = findMethodsBy(declaringClass, signature);
        var messageClasses = messageClasses(map.values());
        return new HandlerMap<>(map, messageClasses);
    }

    private HandlerMap(ImmutableSetMultimap<DispatchKey, H> map, ImmutableSet<M> messageClasses) {
        this.map = map;
        this.messageClasses = messageClasses;
    }

    /**
     * Obtains classes of messages for which handlers are stored in this map.
     */
    public ImmutableSet<M> messageClasses() {
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
        var result = map.values()
                .stream()
                .map(HandlerMethod::producedMessages)
                .flatMap(Set::stream)
                .collect(toImmutableSet());
        return result;
    }

    /**
     * Obtains methods for handling messages of the given class and with the given origin.
     *
     * <p>If there is no handler matching both the message and origin class, handlers will be
     * searched by the message class only.
     *
     * <p>If no handlers for a specified criteria is found, returns an empty set.
     *
     * @param messageClass
     *         the message class of the handled message
     * @param originClass
     *         the class of the message, from which the handled message is originate
     */
    private ImmutableSet<H> handlersOf(M messageClass, MessageClass<?> originClass) {
        var key =
                originClass.equals(EmptyClass.instance())
                ? new DispatchKey(messageClass.value(), null)
                : new DispatchKey(messageClass.value(), originClass.value());
        // If we have a handler with origin type, use the key. Otherwise, find handlers only
        // by the first parameter.
        var presentKey = map.containsKey(key)
                         ? key
                         : new DispatchKey(messageClass.value(), null);
        var handlers = map.get(presentKey);
        return handlers;
    }

    /**
     * Looks up a method for handling the given message.
     *
     * @param message
     *         a signal message which must be handled
     * @return a handler method for the given signal or {@code Optional.empty()}
     */
    public Optional<H> findHandlerFor(SignalEnvelope<?, ?, ?> message) {
        var messageClass = message.messageClass();
        MessageClass<?> originClass;
        if (message instanceof EnvelopeWithOrigin) {
            originClass = ((EnvelopeWithOrigin<?, ?, ?>) message).originClass();
        } else {
            originClass = EmptyClass.instance();
        }
        @SuppressWarnings("unchecked")
        var handlers = handlersOf(((M) messageClass), originClass);
        return handlers.stream()
                       .sorted(comparing((H h) -> h.filter().pathLength()).reversed())
                       .filter(h -> h.filter().test(message.message()))
                       .findFirst();
    }

    /**
     * Obtains a method for handling the given message.
     *
     * @param message
     *         a signal message which must be handled
     * @return a handler method for the given signal
     * @throws ModelError
     *         if the handler the the message was not found
     */
    public H getHandlerFor(SignalEnvelope<?, ?, ?> message) {
        var handler = findHandlerFor(message);
        return handler.orElseThrow(() -> {
            var msg = format(
                    "No handler method found for the type `%s`.", message.messageClass()
            );
            _error().log(msg);
            return new ModelError(msg);
        });
    }

    /**
     * Obtains methods for handling messages of the given class.
     *
     * @param messageClass
     *         the message class of the handled message
     * @return handlers method
     */
    public ImmutableSet<H> handlersOf(M messageClass) {
        return handlersOf(messageClass, EmptyClass.instance());
    }

    private static <M extends MessageClass<?>, H extends HandlerMethod<?, M, ?, ?>>
    ImmutableSet<M> messageClasses(Collection<H> handlerMethods) {
        var result = handlerMethods.stream()
                .map(HandlerMethod::messageClass)
                .collect(toImmutableSet());
        return result;
    }

    /**
     * Obtains handler methods matching the passed criteria.
     */
    public ImmutableCollection<H> methods() {
        return map.values();
    }
}
