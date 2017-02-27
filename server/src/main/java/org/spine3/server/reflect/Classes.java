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

    private Classes() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Returns a class of the generic type of the passed class.
     *
     * <p>Two restrictions apply to the passed class:
     * <ol>
     *     <li>The passed class must have a generic superclass.
     *     <li>This generic superclass must have the number of generic
     *        parameters greater than value passed in {@code paramNumber}.
     *     <li>The generic parameter must not be a type variable.
     * </ol>
     *
     * @param clazz the class to check
     * @param paramNumber a zero-based index of the generic parameter in the class declaration
     * @param <T> the generic type
     * @return the class reference for the generic type
     * @throws ClassCastException if the passed class does not have a generic
     *                            parameter of the expected class
     */
    @CheckReturnValue
    public static <T> Class<T> getGenericParameterType(Class<?> clazz, int paramNumber) {
        // We cast here as we assume that the superclasses of the classes
        // we operate with are parametrized too.
        final ParameterizedType genericSuperclass =
                (ParameterizedType) clazz.getGenericSuperclass();

        final Type[] typeArguments = genericSuperclass.getActualTypeArguments();
        final Type typeArgument = typeArguments[paramNumber];

        @SuppressWarnings("unchecked") /* The cast is the purpose of this method.
            Correctness of the cast must be ensured in the calling code by passing the class,
            which meets the requirements described in the Javadoc. */
        final Class<T> result = (Class<T>) typeArgument;
        return result;
    }

    /**
     * Returns event/command types handled by the passed class.
     *
     * @return immutable set of message classes or an empty set
     */
    @CheckReturnValue
    public static ImmutableSet<Class<? extends Message>> getHandledMessageClasses(
            Class<?> cls,
            Predicate<Method> predicate) {
        final ImmutableSet.Builder<Class<? extends Message>> builder = ImmutableSet.builder();

        for (Method method : cls.getDeclaredMethods()) {
            final boolean methodMatches = predicate.apply(method);
            if (methodMatches) {
                final Class<? extends Message> firstParamType =
                        HandlerMethod.getFirstParamType(method);
                builder.add(firstParamType);
            }
        }

        return builder.build();
    }

    /**
     * Finds a getter method in given class or it's superclasses.
     *
     * <p>The method must match {@code getFieldName} notation, have no argument to be found.
     *
     * @param clazz     class containing the getter method
     * @param fieldName field to find a getter for
     * @return {@link Method} instance reflecting the getter method
     * @throws RuntimeException upon reflective failure
     */
    public static Method getGetterForField(Class<?> clazz, String fieldName)
            throws NoSuchMethodException {
        @SuppressWarnings("DuplicateStringLiteralInspection")
        final String fieldGetterName = "get" + fieldName.substring(0, 1)
                                                        .toUpperCase() + fieldName.substring(1);
        final Method fieldGetter = clazz.getMethod(fieldGetterName);
        return fieldGetter;
    }
}
