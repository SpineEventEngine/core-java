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

import com.google.common.testing.NullPointerTester;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.lang.reflect.Method;

import static io.spine.server.model.MethodExceptionChecker.forMethod;

/**
 * @author Dmytro Kuzmin
 */
public class MethodExceptionCheckerShould {

    @Test
    @DisplayName("pass null check")
    void passNullCheck() {
        new NullPointerTester().testAllPublicStaticMethods(MethodExceptionChecker.class);

        final Method method = getMethod("methodNoExceptions");
        final MethodExceptionChecker checker = forMethod(method);
        new NullPointerTester().testAllPublicInstanceMethods(checker);
    }

    @Test
    @DisplayName("pass check when no checked exceptions thrown")
    void passCheckWhenNoCheckedExceptionsThrown() {
        final Method methodNoExceptions = getMethod("methodNoExceptions");
        final MethodExceptionChecker noExceptionsChecker = forMethod(methodNoExceptions);
        noExceptionsChecker.checkThrowsNoCheckedExceptions();

        final Method methodRuntimeExceptions = getMethod("methodRuntimeException");
        final MethodExceptionChecker runtimeExceptionsChecker = forMethod(methodRuntimeExceptions);
        runtimeExceptionsChecker.checkThrowsNoCheckedExceptions();
    }

    @Test(expected = IllegalStateException.class)
    @DisplayName("fail check when checked exceptions thrown")
    void failCheckWhenCheckedExceptionsThrown() {
        final Method methodCheckedException = getMethod("methodCheckedException");
        final MethodExceptionChecker checker = forMethod(methodCheckedException);
        checker.checkThrowsNoCheckedExceptions();
    }

    @Test
    @DisplayName("pass check for allowed custom exception types")
    void passCheckForAllowedCustomExceptionTypes() {
        final Method methodCustomException = getMethod("methodCustomException");
        final MethodExceptionChecker checker = forMethod(methodCustomException);
        checker.checkThrowsNoExceptionsBut(IOException.class);
    }

    @Test
    @DisplayName("pass check for allowed exception types descendants")
    void passCheckForAllowedExceptionTypesDescendants() {
        final Method methodDescendantException = getMethod("methodDescendantException");
        final MethodExceptionChecker checker = forMethod(methodDescendantException);
        checker.checkThrowsNoExceptionsBut(RuntimeException.class);
    }

    @Test(expected = IllegalStateException.class)
    @DisplayName("fail check for prohibited custom exception types")
    void failCheckForProhibitedCustomExceptionTypes() {
        final Method methodCustomException = getMethod("methodCustomException");
        final MethodExceptionChecker checker = forMethod(methodCustomException);
        checker.checkThrowsNoExceptionsBut(RuntimeException.class);
    }

    private static Method getMethod(String methodName) {
        final Method method;
        final Class<?> clazz = StubMethodContainer.class;
        try {
            method = clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return method;
    }

    private static class StubMethodContainer {

        @SuppressWarnings("unused") // Reflective access.
        private static void methodNoExceptions() {
        }

        @SuppressWarnings("unused") // Reflective access.
        private static void methodCheckedException() throws Exception {
            throw new IOException("Test checked exception");
        }

        @SuppressWarnings("unused") // Reflective access.
        private static void methodRuntimeException() throws RuntimeException {
            throw new RuntimeException("Test runtime exception");
        }

        @SuppressWarnings("unused") // Reflective access.
        private static void methodCustomException() throws IOException {
            throw new IOException("Test custom exception");
        }

        @SuppressWarnings("unused") // Reflective access.
        private static void methodDescendantException() throws IllegalStateException {
            throw new IllegalStateException("Test descendant exception");
        }
    }
}
