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

package io.spine.server.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.Immutable;
import io.spine.core.EmptyClass;
import io.spine.server.model.declare.MethodSignature;
import io.spine.type.MessageClass;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Provides mapping from a class of messages to a method which handles such messages.
 *
 * @param <M>
 *         the type of messages
 * @param <H>
 *         the type of handler methods
 * @author Alexander Yevsyukov
 * @author Dmytro Grankin
 */
@Immutable(containerOf = {"M", "H"})
public class MessageHandlerMap<M extends MessageClass,
                               H extends HandlerMethod<?, M, ?, ?>>
        implements Serializable {

    private static final long serialVersionUID = 0L;

    private final ImmutableMap<HandlerKey, H> map;
    private final ImmutableSet<M> messageClasses;

    private MessageHandlerMap(ImmutableMap<HandlerKey, H> map, ImmutableSet<M> messageClasses) {
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
    public static <M extends MessageClass, H extends HandlerMethod<?, M, ?, ?>>
    MessageHandlerMap<M, H> create(Class<?> declaringClass, MethodSignature<H, ?> signature) {
        checkNotNull(declaringClass);
        checkNotNull(signature);

        ClassScanner scanner = ClassScanner.of(declaringClass);
        ImmutableMap<HandlerKey, H> map = scanner.findMethodsBy(signature);
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
    public ImmutableSet<M> getMessageClasses(Predicate<H> predicate) {
        Map<HandlerKey, H> filtered = Maps.filterValues(map, predicate::test);
        return messageClasses(filtered.values());
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
    private H getMethod(HandlerKey handlerKey) {
        H handlerMethod = map.get(handlerKey);
        checkState(handlerMethod != null,
                   "Unable to find handler with key %s", handlerKey);
        return handlerMethod;
    }

    /**
     * Obtains the method for handling by the passed message and origin classes.
     *
     * <p>If there is no handler matching both the message and origin class,
     * a handler will be searched by a message class only.
     *
     * @param messageClass
     *         the message class of the handled message
     * @param originClass
     *         the class of the message, from which the handled message is originate
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no method found in the map
     */
    public H getMethod(M messageClass, MessageClass originClass) {
        HandlerKey keyWithOrigin = HandlerKey.of(messageClass, originClass);
        HandlerKey presentKey = map.containsKey(keyWithOrigin)
                                ? keyWithOrigin
                                : HandlerKey.of(messageClass);
        return getMethod(presentKey);
    }

    /**
     * Obtains the method for handling by the passed message classes.
     *
     * @param messageClass
     *         the message class of the handled message
     * @return a handler method
     * @throws IllegalStateException
     *         if there is no method found in the map
     */
    public H getMethod(M messageClass) {
        return getMethod(messageClass, EmptyClass.instance());
    }

    private static <M extends MessageClass, H extends HandlerMethod<?, M, ?, ?>>
    ImmutableSet<M> messageClasses(Iterable<H> handlerMethods) {
        Set<M> setToSwallowDuplicates = newHashSet();
        for (H handler : handlerMethods) {
            setToSwallowDuplicates.add(handler.getMessageClass());
        }
        return ImmutableSet.copyOf(setToSwallowDuplicates);
    }
}
