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

import com.google.common.base.Joiner;
import io.spine.annotation.Internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The checker of the exception types thrown by a {@link Method}.
 *
 * <p>This class checks whether exception types thrown by the method match the provided list of
 * allowed exception types.
 *
 * <p>If such check fails, the {@link IllegalStateException} will be thrown. If the check passes,
 * no action is performed.
 *
 * @author Dmytro Kuzmin
 */
@Internal
public class MethodExceptionChecker {

    private final Method method;

    private MethodExceptionChecker(Method method) {
        this.method = method;
    }

    /**
     * Creates new instance of the {@link MethodExceptionChecker} for the specified {@link Method}.
     *
     * @param method the method to create new instance for
     * @return a new instance of {@code MethodExceptionChecker}
     */
    public static MethodExceptionChecker forMethod(Method method) {
        checkNotNull(method);
        return new MethodExceptionChecker(method);
    }

    /**
     * Ensures that contained {@link Method} does not declare any thrown checked exceptions.
     *
     * <p>{@link RuntimeException} and its descendants can still be declared and thrown from
     * the method.
     *
     * @throws IllegalStateException if the check fails
     */
    public void checkThrowsNoCheckedExceptions() {
        checkThrowsNoExceptionsBut(RuntimeException.class);
    }

    /**
     * Checks that contained {@link Method} declares no thrown exception types except the ones
     * specified as the {@code whiteList} and their descendants.
     *
     * @param whiteList the allowed exception types
     * @throws IllegalStateException if the method throws any exception types apart from the
     *                               types specified in {@code whiteList} and their descendants
     */
    public void checkThrowsNoExceptionsBut(Class<?>... whiteList) {
        checkNotNull(whiteList);

        Collection<Class<?>> allowedExceptions = Arrays.asList(whiteList);
        Collection<Class<?>> exceptions = obtainProhibitedExceptionsThrown(allowedExceptions);
        if (!exceptions.isEmpty()) {
            throwCheckFailedException(exceptions, allowedExceptions);
        }
    }

    /**
     * Obtain the {@link Collection} of prohibited exception types thrown by the {@link Method}.
     *
     * <p>Exception types are considered prohibited if they are not contained in the
     * {@code allowedExceptions} and are not descendants of any types from the
     * {@code allowedExceptions}.
     *
     * @param allowedExceptions the list of exceptions whose descendants won't be considered as
     *                          prohibited exceptions
     * @return a {@code Collection} of prohibited exceptions thrown
     */
    private Collection<Class<?>>
    obtainProhibitedExceptionsThrown(Iterable<Class<?>> allowedExceptions) {
        Class<?>[] thrownExceptions = method.getExceptionTypes();
        Collection<Class<?>> result = newLinkedList();
        for (Class<?> exceptionType : thrownExceptions) {
            if (!isMemberOrDescendant(exceptionType, allowedExceptions)) {
                result.add(exceptionType);
            }
        }
        return result;
    }

    /**
     * Throws {@link RuntimeException} with diagnostics information about the prohibited exception
     * types thrown from the {@link Method}.
     *
     * <p>The message of the exception thrown will provide the user with the info about prohibited
     * exception types thrown by the contained {@link Method}, as well as which exception types are
     * allowed for this {@link Method}.
     *
     * @param exceptionsThrown  the list of prohibited exceptions thrown
     * @param allowedExceptions the list of allowed exceptions for the contained {@link Method}
     */
    private void throwCheckFailedException(Iterable<Class<?>> exceptionsThrown,
                                           Iterable<Class<?>> allowedExceptions) {
        throw newIllegalStateException(
                "Method %s.%s throws prohibited exception types: %s. " +
                        "The allowed exception types for this method are: %s",
                method.getDeclaringClass()
                      .getCanonicalName(),
                method.getName(),
                iterableToString(exceptionsThrown),
                iterableToString(allowedExceptions)
        );
    }

    /**
     * Checks if the specified exception type is among the specified list of types or is a
     * descendant of any of them, or none.
     *
     * @param exceptionType  the exception type to check
     * @param exceptionTypes the exception types among which the type is searched
     * @return {@code true} if specified exception type is member or descendant of one of the
     *         specified types, {@code false} otherwise.
     */
    private static boolean
    isMemberOrDescendant(Class<?> exceptionType, Iterable<Class<?>> exceptionTypes) {
        for (Class<?> type : exceptionTypes) {
            if (isEqualOrSubclass(exceptionType, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the specified {@link Class} is the same class as the specified {@linkplain Class
     * superclass} or its descendant.
     *
     * @param classCandidate the class to check
     * @param superClass     the class to check against
     * @return {@code true} if the {@code classCandidate} is the same class as {@code superClass}
     *         or its descendant, {@code false} otherwise.
     */
    private static boolean isEqualOrSubclass(Class<?> classCandidate, Class<?> superClass) {
        return superClass.isAssignableFrom(classCandidate);
    }

    /**
     * Prints {@link Iterable} to {@link String}, separating all its elements by comma.
     *
     * @param iterable the {@code Iterable} to print
     * @return the {@code String} containing all {@code Iterable}'s elements separated by comma
     */
    private static String iterableToString(Iterable<Class<?>> iterable) {
        return Joiner.on(",").join(iterable);
    }
}
