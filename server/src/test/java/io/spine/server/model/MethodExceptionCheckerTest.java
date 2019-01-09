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

import com.google.common.testing.NullPointerTester;
import io.spine.base.ThrowableMessage;
import io.spine.server.model.given.MethodExceptionCheckerTestEnv.StubMethodContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static io.spine.server.model.MethodExceptionChecker.forMethod;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Kuzmin
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* String literals for method names. */})
@DisplayName("MethodExceptionChecker should")
class MethodExceptionCheckerTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(MethodExceptionChecker.class);

        Method method = getMethod("methodNoExceptions");
        MethodExceptionChecker checker = forMethod(method);
        new NullPointerTester().testAllPublicInstanceMethods(checker);
    }

    @Nested
    @DisplayName("pass check when")
    class PassCheck {

        @Test
        @DisplayName("no exceptions are thrown by method")
        void forNoCheckedThrown() {
            Method methodNoExceptions = getMethod("methodNoExceptions");
            MethodExceptionChecker noExceptionsChecker = forMethod(methodNoExceptions);
            noExceptionsChecker.checkDeclaresNoExceptionsThrown();
        }

        @Test
        @DisplayName("allowed exception types are thrown by method")
        void forAllowedThrown() {
            Method methodCustomException = getMethod("methodCustomException");
            MethodExceptionChecker checker = forMethod(methodCustomException);
            checker.checkThrowsNoExceptionsBut(IOException.class);
        }

        @Test
        @DisplayName("allowed exception types descendants are thrown by method")
        void forAllowedDescendantsThrown() {
            Method methodDescendantException = getMethod("methodDescendantException");
            MethodExceptionChecker checker = forMethod(methodDescendantException);
            checker.checkThrowsNoExceptionsBut(ThrowableMessage.class);
        }
    }

    @Nested
    @DisplayName("fail check when")
    class FailCheck {

        @Test
        @DisplayName("checked exceptions are thrown by method")
        void forCheckedThrown() {
            Method methodCheckedException = getMethod("methodCheckedException");
            MethodExceptionChecker checker = forMethod(methodCheckedException);
            assertThrows(IllegalStateException.class, checker::checkDeclaresNoExceptionsThrown);
        }

        @Test
        @DisplayName("runtime exceptions are thrown by method")
        void forRuntimeThrown() {
            Method methodRuntimeException = getMethod("methodRuntimeException");
            MethodExceptionChecker checker = forMethod(methodRuntimeException);
            assertThrows(IllegalStateException.class, checker::checkDeclaresNoExceptionsThrown);
        }

        @Test
        @DisplayName("exception types that are not allowed are thrown by method")
        void forNotAllowedThrown() {
            Method methodCustomException = getMethod("methodCustomException");
            MethodExceptionChecker checker = forMethod(methodCustomException);
            assertThrows(IllegalStateException.class,
                         () -> checker.checkThrowsNoExceptionsBut(RuntimeException.class));
        }
    }

    private static Method getMethod(String methodName) {
        Method method;
        Class<?> clazz = StubMethodContainer.class;
        try {
            method = clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return method;
    }
}
