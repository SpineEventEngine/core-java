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

import java.lang.reflect.Method;

import static io.spine.server.model.MethodAccessChecker.forMethod;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Kuzmin
 */
public class MethodAccessCheckerShould {

    public static final String STUB_WARNING_MESSAGE = "Stub warning message";

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_method() {
        //noinspection ResultOfMethodCallIgnored
        forMethod(Tests.<Method>nullRef());
    }

    @Test
    public void do_not_log_warning_on_correct_access_modifier() {
        final Method publicMethod = getMethod("publicMethod");
        final TestMethodAccessChecker checkerPublic = testAccessCheckerFor(publicMethod);
        checkerPublic.checkAccessIsPublic(STUB_WARNING_MESSAGE);
        assertEquals(0, checkerPublic.getWarningCount());

        final Method packagePrivateMethod = getMethod("packagePrivateMethod");
        final TestMethodAccessChecker checkerPackagePrivate =
                testAccessCheckerFor(packagePrivateMethod);
        checkerPackagePrivate.checkAccessIsPackagePrivate(STUB_WARNING_MESSAGE);
        assertEquals(0, checkerPackagePrivate.getWarningCount());

        final Method privateMethod = getMethod("privateMethod");
        final TestMethodAccessChecker checkerPrivate = testAccessCheckerFor(privateMethod);
        checkerPrivate.checkAccessIsPrivate(STUB_WARNING_MESSAGE);
        assertEquals(0, checkerPrivate.getWarningCount());
    }

    @Test
    public void log_warning_on_incorrect_access_modifier() {
        final Method method = getMethod("protectedMethod");
        final TestMethodAccessChecker checker = testAccessCheckerFor(method);
        checker.checkAccessIsPublic(STUB_WARNING_MESSAGE);
        checker.checkAccessIsPackagePrivate(STUB_WARNING_MESSAGE);
        checker.checkAccessIsPrivate(STUB_WARNING_MESSAGE);
        assertEquals(3, checker.getWarningCount());
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

    private static TestMethodAccessChecker testAccessCheckerFor(Method method) {
        return new TestMethodAccessChecker(method);
    }

    private static class StubMethodContainer {

        @SuppressWarnings("unused") // Reflective access
        public void publicMethod() {

        }

        @SuppressWarnings("unused") // Reflective access
        protected void protectedMethod() {

        }

        @SuppressWarnings("unused")
            // Reflective access
        void packagePrivateMethod() {

        }

        @SuppressWarnings("unused") // Reflective access
        private void privateMethod() {

        }
    }

    /**
     * Testing method access checker which allows to trace whether the warning function was called
     * and how many times.
     */
    private static class TestMethodAccessChecker extends MethodAccessChecker {

        private int warningCount;

        private TestMethodAccessChecker(Method method) {
            super(method);
            warningCount = 0;
        }

        @Override
        void warnOnWrongModifier(String messageFormat) {
            super.warnOnWrongModifier(messageFormat);
            warningCount++;
        }

        private int getWarningCount() {
            return warningCount;
        }
    }
}
