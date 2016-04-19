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
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utilities for working with classes.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class Classes {

    /**
     * Returns the class of the generic type of the passed class.
     *
     * <p>Two restrictions apply to the passed class:
     * <ol>
     *     <li>The passed class must have a generic superclass.
     *     <li>The number of generic parameters in the passed class and its superclass must be the same.
     * </ol>
     *
     * @param clazz the class to check
     * @param paramNumber the number of the generic parameter
     * @param <T> the generic type
     * @return the class reference for the generic type
     */
    @CheckReturnValue
    public static <T> Class<T> getGenericParameterType(Class<?> clazz, int paramNumber) {
        // We cast here as we assume that the superclasses of the classes we operate with are parametrized too.
        final ParameterizedType genericSuperclass = (ParameterizedType) clazz.getGenericSuperclass();

        final Type[] typeArguments = genericSuperclass.getActualTypeArguments();
        @SuppressWarnings("unchecked")
        final Class<T> result = (Class<T>) typeArguments[paramNumber];
        return result;
    }

    /**
     * Returns event/command types handled by the passed class.
     *
     * @return immutable set of message classes or an empty set
     */
    @CheckReturnValue
    public static ImmutableSet<Class<? extends Message>> getHandledMessageClasses(Class<?> clazz,
                                                                                  Predicate<Method> predicate) {
        final ImmutableSet.Builder<Class<? extends Message>> builder = ImmutableSet.builder();

        for (Method method : clazz.getDeclaredMethods()) {
            final boolean methodMatches = predicate.apply(method);
            if (methodMatches) {
                final Class<? extends Message> firstParamType = HandlerMethod.getFirstParamType(method);
                builder.add(firstParamType);
            }
        }

        return builder.build();
    }

    private Classes() {}
}
