/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.Maps;
import com.google.protobuf.Message;

import javax.annotation.CheckReturnValue;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code MethodRegistry} stores handler methods per class exposing those methods.
 *
 * @author Alexander Yevsyukov
 */
public class MethodRegistry {

    private final Map<Key<?, ?>, MethodMap> items = Maps.newConcurrentMap();

    /**
     * Obtains a handler method, which handles the passed message class.
     *
     * <p>If the registry does not have the data structures ready for the target class,
     * they are created using the passed {@code factory} of the handler methods.
     * Then the method is obtained.
     *
     * @param targetClass the class of the target object
     * @param messageClass the class of the message to handle
     * @param factory the handler method factory for getting methods from the target class
     * @param <T> the type of the target object class
     * @param <H> the type of the message handler method
     * @return a handler method
     */
    public <T, H extends HandlerMethod> H get(Class<T> targetClass,
                                              Class<? extends Message> messageClass,
                                              HandlerMethod.Factory<H> factory) {

        final Key<?, H> key = new Key<>(targetClass, factory.getMethodClass());
        @SuppressWarnings("unchecked")
            /* We can cast as the map type is the same as one of the passed factory,
               which is used in creating the entry in the `if` block below. */
        MethodMap<H> methods = items.get(key);
        if (methods == null) {
            methods = MethodMap.create(targetClass, factory);
            items.put(key, methods);
        }

        final H handlerMethod = methods.get(messageClass);
        return handlerMethod;
    }

    /**
     * The map entry key which consists of target object class and method class.
     *
     * @param <T> the type of the target class
     * @param <H> the type of the method handler class
     */
    private static class Key<T, H extends HandlerMethod> {
        private final Class<T> targetClass;
        private final Class<H> methodClass;

        private Key(Class<T> targetClass, Class<H> methodClass) {
            this.targetClass = targetClass;
            this.methodClass = methodClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            final Key key = (Key) o;
            return Objects.equals(targetClass, key.targetClass) &&
                    Objects.equals(methodClass, key.methodClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetClass, methodClass);
        }
    }

    @CheckReturnValue
    public static MethodRegistry getInstance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final MethodRegistry value = new MethodRegistry();
    }
}
