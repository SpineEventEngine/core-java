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
import io.spine.string.Diags;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.ModelError.MessageFormatter.toStringEnumeration;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The checker of the exception types thrown by a {@link Method}.
 *
 * <p>This class checks whether exception types thrown by the method match the provided list of
 * allowed exception types.
 *
 * <p>If such check fails, the {@link IllegalStateException} will be thrown. If the check passes,
 * no action is performed.
 */
final class MethodExceptionCheck {

    private final Method method;
    private final @Nullable Class<? extends Throwable> allowedThrowable;

    private MethodExceptionCheck(Method method, @Nullable Class<? extends Throwable> throwable) {
        this.method = method;
        this.allowedThrowable = throwable;
    }

    /**
     * Creates new instance of the check for the passed method and allowed {@code Throwable}.
     */
    static MethodExceptionCheck
    forMethod(Method method, @Nullable Class<? extends Throwable> allowedThrowable) {
        checkNotNull(method);
        return new MethodExceptionCheck(method, allowedThrowable);
    }

    /**
     * Checks that contained {@link Method} declares no thrown exception types except the ones
     * specified as the {@code whiteList} and their descendants.
     *
     * @throws IllegalStateException if the method throws any exception types apart from the
     *                               types specified in {@code whiteList} and their descendants
     */
    void check() {
        List<Class<? extends Throwable>> prohibited = findProhibited();
        if (!prohibited.isEmpty()) {
            throw prohibitedExceptionThrown(prohibited);
        }
    }

    /**
     * Obtain prohibited exception types thrown by the {@link Method}.
     *
     * <p>Exception types are considered prohibited if they are not contained in the
     * {@code allowedExceptions} and are not descendants of any types from the
     * {@code allowedExceptions}.
     */
    private List<Class<? extends Throwable>> findProhibited() {
        Class<?>[] thrownExceptions = method.getExceptionTypes();
        if (thrownExceptions.length == 0) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<Class<? extends Throwable>> result = ImmutableList.builder();
        for (Class<?> exceptionType : thrownExceptions) {
            if (allowedThrowable == null || !allowedThrowable.isAssignableFrom(exceptionType)) {
                @SuppressWarnings("unchecked")  // As all exceptions extend `Throwable`.
                Class<? extends Throwable> asThrowableCls =
                        (Class<? extends Throwable>) exceptionType;
                result.add(asThrowableCls);
            }
        }
        return result.build();
    }

    /**
     * Throws {@link IllegalStateException} with diagnostics information about the prohibited
     * exception types thrown from the {@link Method}.
     *
     * <p>The message of the exception thrown will provide the user with the info about prohibited
     * exception types thrown by the contained {@link Method}, as well as which exception types are
     * allowed for this {@link Method}.
     */
    private IllegalStateException
    prohibitedExceptionThrown(List<Class<? extends Throwable>> exceptionsThrown) {
        String methodThrows = "The method `%s.%s` throws %s.";
        if (allowedThrowable == null) {
            throw newIllegalStateException(
                    methodThrows + " But throwing is not allowed for this kind of methods.",
                    method.getDeclaringClass()
                          .getCanonicalName(),
                    method.getName(),
                    enumerate(exceptionsThrown)
            );
        }
        throw newIllegalStateException(
                methodThrows + " But only `%s` is allowed for this kind of methods.",
                method.getDeclaringClass()
                      .getCanonicalName(),
                method.getName(),
                enumerate(exceptionsThrown),
                allowedThrowable.getName()
        );
    }

    /**
     * Prints {@link Iterable} to {@link String}, separating elements with comma.
     */
    private static String enumerate(List<Class<? extends Throwable>> throwables) {
        return throwables.stream()
                         .map(Diags::backtick)
                         .collect(toStringEnumeration());
    }
}
