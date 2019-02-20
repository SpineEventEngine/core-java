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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.Immutable;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.type.EmptyClass;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Provides mapping from a class of messages to methods which handle such messages.
 *
 * @param <M>
 *         the type of messages
 * @param <P>
 *         the type of message classes produced by handler methods
 * @param <H>
 *         the type of handler methods
 */
@Immutable(containerOf = {"M", "H"})
public final class MessageHandlerMap<M extends MessageClass<?>,
                                     P extends MessageClass<?>,
                                     H extends HandlerMethod<?, M, ?, P, ?>>
        implements Serializable {

    private static final long serialVersionUID = 0L;

    private final ImmutableMultimap<HandlerTypeInfo, H> map;
    private final ImmutableSet<M> messageClasses;

    private MessageHandlerMap(ImmutableMultimap<HandlerTypeInfo, H> map,
                              ImmutableSet<M> messageClasses) {
        this.map = map;
        this.messageClasses = messageClasses;
    }

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
                   H extends HandlerMethod<?, M, ?, P, ?>>
    MessageHandlerMap<M, P, H> create(Class<?> declaringClass, MethodSignature<H, ?> signature) {
        checkNotNull(declaringClass);
        checkNotNull(signature);

        ClassScanner scanner = ClassScanner.of(declaringClass);
        ImmutableMultimap<HandlerTypeInfo, H> map = scanner.findMethodsBy(signature);
        ImmutableSet<M> messageClasses = messageClasses(map.values());
        return new MessageHandlerMap<>(map, messageClasses);
    }

    /**
     * Obtains classes of messages for which handlers are stored in this map.
     */
    public Set<M> getMessageClasses() {
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
    public ImmutableSet<M> getMessageClasses(Predicate<? super H> predicate) {
        Multimap<HandlerTypeInfo, H> filtered = Multimaps.filterValues(map, predicate::test);
        return messageClasses(filtered.values());
    }

    /**
     * Obtains the classes of messages produced by the handler methods in this map.
     */
    public ImmutableSet<P> getProducedTypes() {
        ImmutableSet<P> result = map
                .values()
                .stream()
                .map(HandlerMethod::getProducedMessages)
                .flatMap(Set::stream)
                .collect(toImmutableSet());
        return result;
    }

    /**
     * Obtains the method for handling by the passed key.
     *
     * @param handlerKey
     *         the key of the handler to get
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no method found in the map
     */
    private ImmutableCollection<H> getMethods(HandlerTypeInfo handlerKey) {
        ImmutableCollection<H> handlers = map.get(handlerKey);
        checkState(!handlers.isEmpty(),
                   "Unable to find handler with key %s", handlerKey);
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
    public ImmutableCollection<H> getMethods(M messageClass, MessageClass originClass) {
        HandlerTypeInfo keyWithOrigin = HandlerTypeInfo
                .newBuilder()
                .setMessageType(typeUrl(messageClass).value())
                .setOriginType(typeUrl(originClass).value())
                .build();
        HandlerTypeInfo presentKey = map.containsKey(keyWithOrigin)
                                 ? keyWithOrigin
                                 : keyWithOrigin.toBuilder()
                                                .clearOriginType()
                                                .build();
        return getMethods(presentKey);
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
    public H getSingleMethod(M messageClass, MessageClass originClass) {
        ImmutableCollection<H> methods = getMethods(messageClass, originClass);
        return checkSingle(methods, messageClass);
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
    public ImmutableCollection<H> getMethods(M messageClass) {
        return getMethods(messageClass, EmptyClass.instance());
    }

    /**
     * Obtains a single handler method for messages of the given class.
     *
     * <p>If there is no such method or several such methods, an {@link IllegalStateException} is
     * thrown.
     *
     * @param messageClass the message class of the handled message
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no such method or several such methods found in the map
     */
    public H getSingleMethod(M messageClass) {
        ImmutableCollection<H> methods = getMethods(messageClass);
        return checkSingle(methods, messageClass);
    }

    private H checkSingle(Collection<H> handlers, M targetType) {
        int count = handlers.size();
        checkState(count == 1,
                   "Unexpected number of handlers for messages of class %s: %s.%n%s",
                   targetType, count, handlers);
        H result = handlers
                .stream()
                .findFirst()
                .get();
        return result;
    }

    private static <M extends MessageClass, H extends HandlerMethod<?, M, ?, ?, ?>>
    ImmutableSet<M> messageClasses(Iterable<H> handlerMethods) {
        ImmutableSet<M> result = Streams.stream(handlerMethods)
                                        .map(HandlerMethod::getMessageClass)
                                        .collect(toImmutableSet());
        return result;
    }

    private static TypeUrl typeUrl(MessageClass<?> cls) {
        return TypeUrl.of(cls.value());
    }
}
