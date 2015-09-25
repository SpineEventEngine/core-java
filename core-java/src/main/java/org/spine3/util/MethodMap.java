/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A map for storing methods handling messages.
 *
 * @author Alexander Yevsyukov
 */
public class MethodMap {

    private final ImmutableMap<Class<? extends Message>, Method> map;

    public MethodMap(Class<?> clazz, Predicate<Method> filter) {
        this(Methods.scan(clazz, filter));
    }

    private MethodMap(Map<Class<? extends Message>, Method> map) {
        this.map = ImmutableMap.<Class<? extends Message>, Method>builder()
                .putAll(map)
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<Class<? extends Message>, Method> map = Maps.newHashMap();

        public Builder addMany(Iterable<Class<? extends Message>> keys, Method value) {
            for (Class<? extends Message> key : keys) {
                map.put(key, value);
            }
            return this;
        }

        public MethodMap build() {
            MethodMap result = new MethodMap(map);
            return result;
        }
    }

    /**
     * @return {@code true} if the map is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    public ImmutableSet<Class<? extends Message>> keySet() {
        return map.keySet();
    }

    public ImmutableSet<Map.Entry<Class<? extends Message>, Method>> entrySet() {
        return map.entrySet();
    }

    public boolean containsHandlerFor(Class<? extends Message> messageClass) {
        return map.containsKey(checkNotNull(messageClass));
    }

    @Nullable
    public Method get(Class<? extends Message> messageClass) {
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
        public boolean contains(Class<? extends T> clazz) {
            boolean result = entries.containsKey(clazz);
            return result;
        }

        /**
         * Registers methods of the class in the registry.
         *
         * @param clazz the class to register
         * @param filter a filter for selecting methods to register
         * @throws IllegalArgumentException if the class was already registered
         * @see #contains(Class)
         */
        public void register(Class<? extends T> clazz, Predicate<Method> filter) {
            if (contains(clazz)) {
                throw new IllegalArgumentException("The class is already registered: " + clazz.getName());
            }

            MethodMap entry = new MethodMap(clazz, filter);
            entries.put(clazz, entry);
        }

        /**
         * Obtains method map for the passed class.
         */
        public MethodMap get(Class<? extends T> clazz) {
            MethodMap result = entries.get(clazz);
            return result;
        }
    }
}
