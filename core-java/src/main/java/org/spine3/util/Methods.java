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
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.error.DuplicateHandlerMethodException;
import org.spine3.internal.MessageHandlerMethod;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Utilities for client-side methods.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Methods {

    private Methods() {}

    /**
     * Returns a full method name without parameters.
     *
     * @param obj    an object the method belongs to
     * @param method a method to get name for
     * @return full method name
     */
    @SuppressWarnings("TypeMayBeWeakened") // We keep the type to make the API specific.
    public static String getFullMethodName(Object obj, Method method) {
        return obj.getClass().getName() + '.' + method.getName() + "()";
    }

    /**
     * Returns the class of the first parameter of the passed handler method object.
     * <p>
     * It is expected that the first parameter of the passed method is always of
     * a class implementing {@link Message}.
     *
     * @param handler the method object to take first parameter type from
     * @return the {@link Class} of the first method parameter
     * @throws ClassCastException if the first parameter isn't a class implementing {@link Message}
     */
    public static Class<? extends Message> getFirstParamType(Method handler) {
        @SuppressWarnings("unchecked") /** we always expect first param as {@link Message} */
                Class<? extends Message> result = (Class<? extends Message>) handler.getParameterTypes()[0];
        return result;
    }

    /**
     * Returns a map of the {@link MessageHandlerMethod} objects to the corresponding message class.
     *
     * @param clazz   the class that declares methods to scan
     * @param filter the predicate that defines rules for subscriber scanning
     * @return the map of message subscribers
     * @throws DuplicateHandlerMethodException if there are more than one handler for the same message class are encountered
     */
    public static Map<Class<? extends Message>, Method> scan(Class<?> clazz, Predicate<Method> filter) {
        Map<Class<? extends Message>, Method> tempMap = Maps.newHashMap();
        for (Method method : clazz.getDeclaredMethods()) {
            if (filter.apply(method)) {

                Class<? extends Message> messageClass = getFirstParamType(method);

                if (tempMap.containsKey(messageClass)) {
                    Method alreadyPresent = tempMap.get(messageClass);
                    throw new DuplicateHandlerMethodException(clazz, messageClass,
                            alreadyPresent.getName(), method.getName());
                }
                tempMap.put(messageClass, method);
            }
        }
        ImmutableMap.Builder<Class<? extends Message>, Method> builder = ImmutableMap.builder();
        builder.putAll(tempMap);
        return builder.build();
    }
}
