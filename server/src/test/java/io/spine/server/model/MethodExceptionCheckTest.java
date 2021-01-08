/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.server.model.given.StubMethodContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.model.MethodExceptionCheck.check;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;

@DisplayName("MethodExceptionChecker should")
class MethodExceptionCheckTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(MethodExceptionCheck.class);

        Method method = method("noExceptions");
        MethodExceptionCheck checker = check(method, null);
        new NullPointerTester().testAllPublicInstanceMethods(checker);
    }

    @Nested
    @DisplayName("pass check when")
    class PassCheck {

        @Test
        @DisplayName("no exceptions are thrown by method")
        void forNoCheckedThrown() {
            assertNoViolations(check(method("noExceptions"), null));
        }

        @Test
        @DisplayName("allowed exception types are thrown by method")
        void forAllowedThrown() {
            assertNoViolations(check(method("customException"), IOException.class));
        }

        @Test
        @DisplayName("allowed exception types descendants are thrown by method")
        void forAllowedDescendantsThrown() {
            assertNoViolations(check(method("derivedException"), ThrowableMessage.class));
        }

        void assertNoViolations(MethodExceptionCheck check) {
            assertThat(check.findProhibited()).isEmpty();
        }
    }

    @Nested
    @DisplayName("fail check when")
    class FailCheck {

        @Test
        @DisplayName("checked exceptions are thrown by method")
        void forCheckedThrown() {
            assertFindsProhibited(check(method("checkedException"), null));
        }

        @Test
        @DisplayName("runtime exceptions are thrown by method")
        void forRuntimeThrown() {
            assertFindsProhibited(check(method("runtimeException"), null));
        }

        @Test
        @DisplayName("exception types that are not allowed are thrown by method")
        void forNotAllowedThrown() {
            assertFindsProhibited(check(method("customException"), RuntimeException.class));
        }

        private void assertFindsProhibited(MethodExceptionCheck check) {
            assertThat(check.findProhibited()).isNotEmpty();
        }
    }

    private static Method method(String methodName) {
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
