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

    public <TargetType,
            MethodType extends HandlerMethod,
            MessageType extends Class<? extends Message>>

    MethodType get(Class<TargetType> targetClass,
            MessageType messageClass,
            HandlerMethod.Factory<MethodType> factory) {

        final Key<?, MethodType> key = new Key<>(targetClass, factory.getMethodClass());

        MethodMap<MethodType> methods = items.get(key);
        if (methods == null) {
            methods = MethodMap.create(targetClass, factory);
            items.put(key, methods);
        }

        final MethodType handlerMethod = methods.get(messageClass);
        return handlerMethod;
    }

    private static class Key<TT, MT extends HandlerMethod> {
        private final Class<TT> targetClass;
        private final Class<MT> methodClass;

        private Key(Class<TT> targetClass, Class<MT> methodClass) {
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
