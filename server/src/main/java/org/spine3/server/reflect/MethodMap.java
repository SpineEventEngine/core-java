/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.reflect;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.error.DuplicateHandlerMethodException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A map for storing methods handling messages.
 *
 * @param <H> the type of the handler method instances stored in the map
 * @author Alexander Yevsyukov
 */
public class MethodMap<H extends HandlerMethod> {

    private final ImmutableMap<Class<? extends Message>, H> map;

    private MethodMap(Class<?> clazz, HandlerMethod.Factory<H> factory) {
        final Map<Class<? extends Message>, Method> rawMethods = scan(clazz, factory.getPredicate());

        final ImmutableMap.Builder<Class<? extends Message>, H> builder = ImmutableMap.builder();
        for (Map.Entry<Class<? extends Message>, Method> entry : rawMethods.entrySet()) {
            final H value = factory.create(entry.getValue());
            builder.put(entry.getKey(), value);
        }

        this.map = builder.build();
    }

    /**
     * Creates a new method map for the passed class using the passed factory.
     *
     * @param clazz the class to inspect
     * @param factory the factory for handler methods
     * @param <H> the type of the handler methods
     * @return new method map
     */
    public static <H extends HandlerMethod> MethodMap<H> create(Class<?> clazz, HandlerMethod.Factory<H> factory) {
        return new MethodMap<>(clazz, factory);
    }

    /**
     * Returns a map of the {@link HandlerMethod} objects to the corresponding message class.
     *
     * @param declaringClass   the class that declares methods to scan
     * @param filter the predicate that defines rules for subscriber scanning
     * @return the map of message subscribers
     * @throws DuplicateHandlerMethodException if there are more than one handler for the same message class are encountered
     */
    private static Map<Class<? extends Message>, Method> scan(Class<?> declaringClass, Predicate<Method> filter) {
        final Map<Class<? extends Message>, Method> tempMap = Maps.newHashMap();
        for (Method method : declaringClass.getDeclaredMethods()) {
            if (filter.apply(method)) {
                final Class<? extends Message> messageClass = HandlerMethod.getFirstParamType(method);
                if (tempMap.containsKey(messageClass)) {
                    final Method alreadyPresent = tempMap.get(messageClass);
                    throw new DuplicateHandlerMethodException(
                            declaringClass,
                            messageClass,
                            alreadyPresent.getName(),
                            method.getName());
                }
                tempMap.put(messageClass, method);
            }
        }
        final ImmutableMap<Class<? extends Message>, Method> result = ImmutableMap.copyOf(tempMap);
        return result;
    }

    /**
     * @return {@code true} if the map is empty, {@code false} otherwise
     */
    @CheckReturnValue
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @CheckReturnValue
    public ImmutableSet<Class<? extends Message>> keySet() {
        return map.keySet();
    }

    @CheckReturnValue
    public ImmutableSet<Map.Entry<Class<? extends Message>, H>> entrySet() {
        return map.entrySet();
    }

    @CheckReturnValue
    public ImmutableCollection<H> values() {
        return map.values();
    }

    @CheckReturnValue
    @Nullable
    public H get(Class<? extends Message> messageClass) {
        return map.get(checkNotNull(messageClass));
    }
}
