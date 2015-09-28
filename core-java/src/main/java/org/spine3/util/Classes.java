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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

/**
 * Utilities for working with classes.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Classes {

    private Classes() {}

    /**
     * Returns the class of the generic type of the passed class.
     *
     * @param clazz the class to check
     * @param paramNumber the number of the generic parameter
     * @param <T> the generic type
     * @return the class reference for the generic type
     */
    @CheckReturnValue
    public static <T> Class<T> getGenericParameterType(Class<?> clazz, int paramNumber) {
        try {
            Type genericSuperclass = clazz.getGenericSuperclass();
            Field actualTypeArguments = genericSuperclass.getClass().getDeclaredField("actualTypeArguments");

            actualTypeArguments.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<T> result = (Class<T>) ((Type[]) actualTypeArguments.get(genericSuperclass))[paramNumber];
            actualTypeArguments.setAccessible(false);

            return result;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

    /**
     * Returns event/command types handled by given {@code Aggregate} class.
     *
     * @return immutable set of message classes or an empty set
     */
    @CheckReturnValue
    public static ImmutableSet<Class<? extends Message>> getHandledMessageClasses(Class<?> clazz, Predicate<Method> methodPredicate) {

        Set<Class<? extends Message>> result = Sets.newHashSet();

        for (Method method : clazz.getDeclaredMethods()) {

            boolean methodMatches = methodPredicate.apply(method);

            if (methodMatches) {
                Class<? extends Message> firstParamType = Methods.getFirstParamType(method);
                result.add(firstParamType);
            }
        }
        return ImmutableSet.<Class<? extends Message>>builder().addAll(result).build();
    }
}
