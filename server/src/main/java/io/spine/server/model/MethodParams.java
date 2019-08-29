/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.server.type.MessageEnvelope;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

/**
 * Provides information about parameters of a method.
 */
public final class MethodParams {

    private final ImmutableList<Parameter> params;

    /** Obtains parameters from the passed method. */
    private MethodParams(Method method) {
        checkNotNull(method);
        this.params = ImmutableList.copyOf(method.getParameters());
    }

    /** Obtains the number of parameters passed to the method. */
    public int size() {
        return params.size();
    }

    /**
     * Obtains the type of the method parameter.
     *
     * @param index the zero-based index of the type.
     */
    public Class<?> type(int index) {
        Parameter parameter = params.get(index);
        return parameter.getType();
    }

    /**
     * Verifies if the method has only one parameter of the passed type.
     */
    public boolean is(Class<?> type) {
        if (size() != 1) {
            return false;
        }
        Class<?> firstParam = type(0);
        return type.isAssignableFrom(firstParam);
    }

    /**
     * Verifies if the method has only two parameters and they match the passed types.
     */
    public boolean are(Class<?> type1, Class<?> type2) {
        if (size() != 2) {
            return false;
        }
        boolean firstMatches = type1.isAssignableFrom(type(0));
        boolean secondMatches = type2.isAssignableFrom(type(1));
        return firstMatches && secondMatches;
    }

    /**
     * Verifies if these parameters match the passed types.
     */
    public boolean match(List<Class<?>> types) {
        if (size() != types.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            Class<?> actual = type(i);
            Class<?> expected = types.get(i);
            if (!expected.isAssignableFrom(actual)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the given method parameters have exactly one parameter of an expected type.
     *
     * <p>This is a convenience shortcut for {@link #consistsOfTypes(Class[], List)
     * consistsOfTypes(Class[], List)}.
     *
     * @param methodParams
     *         the method parameters to check
     * @param type
     *         the type of expected single parameter
     * @return {@code true} if the method has exactly one parameter of the expected type,
     *         {@code false} otherwise
     */
    public static boolean consistsOfSingle(Class<?>[] methodParams, Class<?> type) {
        checkNotNull(methodParams);
        checkNotNull(type);

        if (1 != methodParams.length) {
            return false;
        }
        Class<?> firstParam = methodParams[0];
        return type.isAssignableFrom(firstParam);
    }

    /**
     * Checks whether the given method parameters are two parameters of expected types.
     *
     * <p>This is a convenience shortcut for {@link #consistsOfTypes(Class[], List)
     * consistsOfTypes(Class[], List)}.
     *
     * @param methodParams
     *         the method parameters to check
     * @param firstType
     *         the expected type of the first parameter
     * @param secondType
     *         the expected type of the second parameter
     * @return {@code true} if the method parameters are of expected types, {@code false} otherwise
     */
    public static boolean consistsOfTwo(Class<?>[] methodParams,
                                        Class<?> firstType,
                                        Class<?> secondType) {
        checkNotNull(methodParams);
        checkNotNull(firstType);
        checkNotNull(secondType);

        return consistsOfTypes(methodParams, asList(firstType, secondType));
    }

    /**
     * Checks whether the given method parameters are of expected types.
     *
     * @param methodParams
     *         the method params to check
     * @param expectedTypes
     *         the expected types
     * @return {@code true} if the method parameters are of expected types, {@code false} otherwise
     */
    public static boolean consistsOfTypes(Class<?>[] methodParams,
                                          List<Class<?>> expectedTypes) {
        if (methodParams.length != expectedTypes.size()) {
            return false;
        }
        for (int i = 0; i < methodParams.length; i++) {
            Class<?> actual = methodParams[i];
            Class<?> expected = expectedTypes.get(i);
            if (!expected.isAssignableFrom(actual)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds matching parameter specification based on the parameters of the given method and
     * the class, describing the parameter specification.
     *
     * @param method
     *         the method, which parameter list is used to match the parameter specification
     * @param paramSpecs
     *         the class of parameter specification
     * @param <E>
     *         the type of message envelope, which the parameter specification class handles
     * @param <S>
     *         the type of the parameter specification
     * @return the matching parameter spec,
     *         or {@link Optional#empty() Optional.empty()} if no matching specification is found
     */
    static <E extends MessageEnvelope<?, ?, ?>, S extends ParameterSpec<E>>
    Optional<S> findMatching(Method method, Collection<S> paramSpecs) {
        MethodParams params = new MethodParams(method);
        Optional<S> result = paramSpecs.stream()
                                       .filter(spec -> spec.matches(params))
                                       .findFirst();
        return result;
    }

    /**
     * Finds out if the first method parameter is a {@linkplain CommandMessage}.
     *
     * @param method
     *         the method to inspect
     * @return {@code true} if the first parameter is a {@code Command} message,
     *         {@code false} otherwise
     */
    public static boolean isFirstParamCommand(Method method) {
        checkNotNull(method);

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            Class<?> firstParameter = parameterTypes[0];
            boolean result = CommandMessage.class.isAssignableFrom(firstParameter);
            return result;
        }
        return false;
    }
}
