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

package io.spine.server.aggregate;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import io.spine.server.reflect.DuplicateHandlerMethodException;
import io.spine.server.reflect.HandlerMethod;
import io.spine.type.MessageClass;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.reflect.HandlerMethod.getFirstParamType;

public class MessageHandlerMap<M extends MessageClass, H extends HandlerMethod>
        implements Serializable {

    private static final long serialVersionUID = 0L;

    private final ImmutableMap<M, H> map;

    public MessageHandlerMap(Class<?> cls, HandlerMethod.Factory<H> factory) {
        final Predicate<Method> predicate = factory.getPredicate();

        final Map<Class<? extends Message>, Method> rawMethods = scan(cls, predicate);
        final ImmutableMap.Builder<M, H> builder = ImmutableMap.builder();
        for (Method method : rawMethods.values()) {
            final H handlerMethod = factory.create(method);
            @SuppressWarnings("unchecked") // The type is ensured by handler method impl.
            final M messageClass = (M) handlerMethod.getMessageClass();
            builder.put(messageClass, handlerMethod);
        }
        this.map = builder.build();
    }

    private static Map<Class<? extends Message>, Method> scan(Class<?> declaringClass,
                                                              Predicate<Method> filter) {
        final Map<Class<? extends Message>, Method> tempMap = Maps.newHashMap();
        final Method[] declaredMethods = declaringClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (filter.apply(method)) {
                final Class<? extends Message> messageClass = getFirstParamType(method);
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

    public Set<M> getMessageClasses() {
        return map.keySet();
    }

    public H getMethod(M messageClass) {
        final H handlerMethod = map.get(messageClass);
        checkState(handlerMethod != null,
                   "Unable to find handler for the message class %s", messageClass);
        return handlerMethod;
    }
}
