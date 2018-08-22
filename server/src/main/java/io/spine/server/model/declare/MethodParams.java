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

package io.spine.server.model.declare;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.core.MessageEnvelope;
import io.spine.type.TypeName;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

/**
 * The utility class for working with {@link Method} parameters.
 *
 * @author Alex Tymchenko
 */
public final class MethodParams {

    /**
     * Prevents this utility class from instantiation.
     */
    private MethodParams() {
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
     * @param paramSpecClass
     *         the class of parameter specification
     * @param <E>
     *         the type of message envelope, which the parameter specification class handles
     * @param <S>
     *         the type of the parameter specification
     * @return the matching parameter spec,
     *         or {@link Optional#empty() Optional.empty()} if no matching specification is found
     */
    static <E extends MessageEnvelope<?, ?, ?>, S extends ParameterSpec<E>>
    Optional<S> findMatching(Method method, Class<S> paramSpecClass) {

        Class<?>[] parameterTypes = method.getParameterTypes();
        S[] specs = paramSpecClass.getEnumConstants();
        verify(specs != null,
               "`ParameterSpec` implementations are expected " +
                       "to be enumerations, but that's not true for %s",
               paramSpecClass);

        @SuppressWarnings("ConstantConditions") // the argument is not `null`, as verified above.
                Optional<S> result = stream(specs)
                .filter(s -> s.matches(parameterTypes))
                .findFirst();
        return result;
    }

    /**
     * Finds out if the first method parameter is a {@linkplain io.spine.core.Command Command}
     * message.
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
            if (Message.class.isAssignableFrom(firstParameter)) {
                @SuppressWarnings("unchecked")  // ensured by the check above.
                        Class<? extends Message> msgParameter =
                        (Class<? extends Message>) method.getParameterTypes()[0];
                Descriptors.FileDescriptor fileDescriptor = TypeName.of(msgParameter)
                                                                    .getMessageDescriptor()
                                                                    .getFile();
                boolean isCommand = CommandMessage.File.predicate()
                                                       .test(fileDescriptor);
                return isCommand;
            }
        }
        return false;
    }
}
