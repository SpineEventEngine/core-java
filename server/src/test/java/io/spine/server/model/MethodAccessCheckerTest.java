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
import io.spine.testing.logging.Interceptor;
import io.spine.testing.logging.MuteLogging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.logging.Level;

import static io.spine.server.model.MethodAccessChecker.forMethod;
import static io.spine.server.model.given.MethodAccessCheckerTestEnv.publicMethod;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.lang.String.format;

@DisplayName("MethodAccessChecker should")
class MethodAccessCheckerTest {

    private static final String STUB_WARNING_MESSAGE = "Stub warning message %s";

    private final Interceptor interceptor =
            new Interceptor(MethodAccessChecker.class, Level.WARNING);

    @BeforeEach
    void interceptLog() {
        interceptor.intercept();
    }

    @AfterEach
    void releaseLog() {
        interceptor.release();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(MethodAccessChecker.class);

        Method method = publicMethod();
        MethodAccessChecker checker = forMethod(method);
        new NullPointerTester().testAllPublicInstanceMethods(checker);
    }

    @Test
    @MuteLogging
    @DisplayName("log warning on incorrect access modifier")
    void warnOnIncorrectAccess() {
        Method method = publicMethod();
        MethodAccessChecker checker = forMethod(method);

        checker.checkPrivate(STUB_WARNING_MESSAGE);

        String anyMethodName = ".*";
        String expected = format(STUB_WARNING_MESSAGE, anyMethodName);
        interceptor.assertLog()
                   .textOutput()
                   .containsMatch(expected);
    }

    @Test
    @DisplayName("not log warning on correct access modifier")
    void recognizeCorrectAccess() {
        Method publicMethod = publicMethod();
        MethodAccessChecker checker = forMethod(publicMethod);
        checker.checkPublic(STUB_WARNING_MESSAGE);

        interceptor.assertLog()
                   .isEmpty();
    }
}
