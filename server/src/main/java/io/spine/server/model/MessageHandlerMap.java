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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.spine.type.MessageClass;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Provides mapping from a class of messages to a method which handles such messages.
 *
 * @param <M> the type of messages
 * @param <H> the type of handler methods
 * @author Alexander Yevsyukov
 */
public class MessageHandlerMap<M extends MessageClass, H extends HandlerMethod<M, ?>>
        implements Serializable {

    private static final long serialVersionUID = 0L;

    private final ImmutableMap<HandlerKey, H> map;

    /**
     * Creates a map of methods found in the passed class.
     *
     * @param cls     the class to inspect
     * @param factory the factory of methods
     */
    public MessageHandlerMap(Class<?> cls, HandlerMethod.Factory<H> factory) {
        this.map = scan(cls, factory);
    }

    /**
     * Obtains classes of messages for which handlers are stored in this map.
     */
    public Set<M> getMessageClasses() {
        return messageClasses(map.values());
    }

    /**
     * Obtains classes of messages which handlers satisfy the passed {@code predicate}.
     *
     * @param predicate a predicate for handler methods to filter the corresponding message classes
     */
    public ImmutableSet<M> getMessageClasses(Predicate<H> predicate) {
        Map<HandlerKey, H> filtered = Maps.filterValues(map, predicate);
        return messageClasses(filtered.values());
    }

    /**
     * Obtains the method for handling by the passed key.
     *
     * @param handlerKey the key of the handler to get
     * @return a handler method
     * @throws IllegalStateException if there is no method found in the map
     */
    public H getMethod(HandlerKey handlerKey) {
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
     * @param messageClass the message class of the handled message
     * @param originClass  the class of the message, from which the handled message is originate
     * @return a handler method
     * @throws IllegalStateException if there is no method found in the map
     */
    public H getMethod(M messageClass, MessageClass originClass) {
        HandlerKey keyWithOrigin = HandlerKey.of(messageClass, originClass);
        if (map.containsKey(keyWithOrigin)) {
            return getMethod(keyWithOrigin);
        }
        return getMethod(messageClass);
    }

    /**
     * Obtains the method for handling by the passed message classes.
     *
     * @param messageClass the message class of the handled message
     * @return a handler method
     * @throws IllegalStateException if there is no method found in the map
     */
    public H getMethod(M messageClass) {
        HandlerKey key = HandlerKey.of(messageClass);
        return getMethod(key);
    }

    private static <M extends MessageClass, H extends HandlerMethod<M, ?>>
    ImmutableSet<M> messageClasses(Iterable<H> handlerMethods) {
        Set<M> setToSwallowDuplicates = newHashSet();
        for (H handler : handlerMethods) {
            setToSwallowDuplicates.add(handler.getMessageClass());
        }
        return ImmutableSet.copyOf(setToSwallowDuplicates);
    }

    private static <M extends MessageClass, H extends HandlerMethod<M, ?>>
    ImmutableMap<HandlerKey, H> scan(Class<?> declaringClass, HandlerMethod.Factory<H> factory) {
        Predicate<Method> filter = factory.getPredicate();
        Map<HandlerKey, H> tempMap = Maps.newHashMap();
        Method[] declaredMethods = declaringClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (filter.apply(method)) {
                H handler = factory.create(method);
                HandlerKey handlerKey = handler.key();
                if (tempMap.containsKey(handlerKey)) {
                    Method alreadyPresent = tempMap.get(handlerKey)
                                                   .getMethod();
                    throw new DuplicateHandlerMethodError(
                            declaringClass,
                            handlerKey,
                            alreadyPresent.getName(),
                            method.getName());
                }
                tempMap.put(handlerKey, handler);
            }
        }
        ImmutableMap<HandlerKey, H> result = ImmutableMap.copyOf(tempMap);
        return result;
    }
}
