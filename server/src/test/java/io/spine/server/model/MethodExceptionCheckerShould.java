/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import io.spine.test.Tests;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static io.spine.server.model.MethodExceptionChecker.forMethod;

/**
 * @author Dmytro Kuzmin
 */
public class MethodExceptionCheckerShould {

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_method() {
        //noinspection ResultOfMethodCallIgnored
        forMethod(Tests.<Method>nullRef());
    }

    @Test
    public void pass_check_when_no_checked_exceptions_thrown() {
        final Method methodNoExceptions = getMethod("methodNoExceptions");
        final MethodExceptionChecker noExceptionsChecker = forMethod(methodNoExceptions);
        noExceptionsChecker.checkThrowsNoCheckedExceptions();

        final Method methodRuntimeExceptions = getMethod("methodRuntimeException");
        final MethodExceptionChecker runtimeExceptionsChecker = forMethod(methodRuntimeExceptions);
        runtimeExceptionsChecker.checkThrowsNoCheckedExceptions();
    }

    @Test(expected = IllegalStateException.class)
    public void fail_check_when_checked_exceptions_thrown() {
        final Method methodCheckedException = getMethod("methodCheckedException");
        final MethodExceptionChecker checker = forMethod(methodCheckedException);
        checker.checkThrowsNoCheckedExceptions();
    }

    @Test
    public void pass_check_for_allowed_custom_exception_types() {
        //noinspection DuplicateStringLiteralInspection
        final Method methodCustomException = getMethod("methodCustomException");
        final MethodExceptionChecker checker = forMethod(methodCustomException);
        checker.checkThrowsNoExceptionsExcept(IOException.class);
    }

    @Test
    public void pass_check_for_allowed_exception_types_descendants() {
        final Method methodDescendantException = getMethod("methodDescendantException");
        final MethodExceptionChecker checker = forMethod(methodDescendantException);
        checker.checkThrowsNoExceptionsExcept(RuntimeException.class);
    }

    @Test(expected = IllegalStateException.class)
    public void fail_check_for_prohibited_custom_exception_types() {
        //noinspection DuplicateStringLiteralInspection
        final Method methodCustomException = getMethod("methodCustomException");
        final MethodExceptionChecker checker = forMethod(methodCustomException);
        checker.checkThrowsNoExceptionsExcept(RuntimeException.class);
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

        @SuppressWarnings("unused") // Reflective access
        private static void methodNoExceptions() {

        }

        @SuppressWarnings("unused") // Reflective access
        private static void methodCheckedException() throws Exception {
            throw new IOException("Test checked exception");
        }

        @SuppressWarnings("unused") // Reflective access
        private static void methodRuntimeException() throws RuntimeException {
            throw new RuntimeException("Test runtime exception");
        }

        @SuppressWarnings("unused") // Reflective access
        private static void methodCustomException() throws IOException {
            throw new IOException("Test custom exception");
        }

        @SuppressWarnings("unused") // Reflective access
        private static void methodDescendantException() throws IllegalStateException {
            throw new IllegalStateException("Test descendant exception");
        }
    }
}
