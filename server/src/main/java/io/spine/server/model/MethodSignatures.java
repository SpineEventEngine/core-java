/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static java.util.Arrays.stream;

/**
 * @author Alex Tymchenko
 */
public final class MethodSignatures {

    /**
     * Prevents this utility class from instantiation.
     */
    private MethodSignatures() {
    }

    public static boolean consistsOfSingle(Class<?>[] methodParams, Class<?> type) {
        checkNotNull(methodParams);
        checkNotNull(type);

        if (!(1 == methodParams.length)) {
            return false;
        }
        Class<?> firstParam = methodParams[0];
        return type.isAssignableFrom(firstParam);
    }

    public static boolean consistsOfTwo(Class<?>[] methodParams,
                                        Class<?> firstType,
                                        Class<?> secondType) {
        checkNotNull(methodParams);
        checkNotNull(firstType);
        checkNotNull(secondType);

        if (!(2 == methodParams.length)) {
            return false;
        }
        Class<?> firstParam = methodParams[0];
        Class<?> secondParam = methodParams[1];
        return firstType.isAssignableFrom(firstParam) &&
                secondType.isAssignableFrom(secondParam);
    }

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

    static <S extends ParameterSpec<?>>
    Optional<S> findMatching(Method method, Class<S> signatureClass) {

        Class<?>[] parameterTypes = method.getParameterTypes();
        S[] signatures = signatureClass.getEnumConstants();
        verify(signatures != null,
               "`ParameterSpec` implementations are expected " +
                       "to be enumerations, but that's not true for %s",
               signatureClass);

        Optional<S> result = stream(signatures)
                .filter(s -> s.matches(parameterTypes))
                .findFirst();
        return result;
    }
}
