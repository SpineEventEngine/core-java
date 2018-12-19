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

package io.spine.server.security;

import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("InvocationGuard should")
class InvocationGuardTest extends UtilityClassTest<InvocationGuard> {

    InvocationGuardTest() {
        super(InvocationGuard.class);
    }

    @Nested
    @DisplayName("throw SecurityException")
    class Throwing {

        @Test
        @DisplayName("if no classes are allowed")
        void nobodyAllowed() {
            assertThrowsOn(() -> InvocationGuard.allowOnly(""));
        }

        @Test
        @DisplayName("if a calling class is not that allowed")
        void notAllowed() {
            assertThrowsOn(() -> InvocationGuard.allowOnly("java.lang.Boolean"));
        }

        @Test
        @DisplayName("if a calling class is not among allowed")
        void notAllowedFromMany() {
            assertThrowsOn(() -> InvocationGuard.allowOnly(
                    "java.lang.String",
                    "org.junit.jupiter.api.Test")
            );
        }
    }

    @Test
    @DisplayName("do not throw on allowed class")
    void pass() {
        String callingClass = CallerProvider.instance()
                                            .getCallerClass()
                                            .getName();
        try {
            InvocationGuard.allowOnly(callingClass);
        } catch (Exception e) {
            fail(e);
        }
    }

    private static void assertThrowsOn(Executable executable) {
        assertThrows(SecurityException.class, executable);
    }
}
