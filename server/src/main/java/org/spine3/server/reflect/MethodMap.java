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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;

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
        final Map<Class<? extends Message>, Method> rawMethods = Methods.scan(clazz, factory.getPredicate());

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

    /**
     * The registry of message maps by class.
     *
     * @param <T> the type of objects tracked by the registry
     */
    public static class Registry<T> {

        private final Map<Class<? extends T>, MethodMap> entries = Maps.newConcurrentMap();

        /**
         * Verifies if the class is already registered.
         *
         * @param clazz the class to check
         * @return {@code true} if there is a message map for the passed class, {@code false} otherwise
         */
        @CheckReturnValue
        public boolean contains(Class<? extends T> clazz) {
            final boolean result = entries.containsKey(clazz);
            return result;
        }

        /**
         * Registers methods of the class in the registry.
         *
         * @param clazz the class to register
         * @param factory a filter for selecting methods to register
         * @throws IllegalArgumentException if the class was already registered
         * @see #contains(Class)
         */
        public <H extends HandlerMethod> void register(Class<? extends T> clazz, HandlerMethod.Factory<H> factory) {
            if (contains(clazz)) {
                throw new IllegalArgumentException("The class is already registered: " + clazz.getName());
            }

            final MethodMap<H> entry = create(clazz, factory);
            entries.put(clazz, entry);
        }

        /**
         * Obtains method map for the passed class.
         */
        @CheckReturnValue
        public MethodMap get(Class<? extends T> clazz) {
            final MethodMap result = entries.get(clazz);
            return result;
        }
    }
}
