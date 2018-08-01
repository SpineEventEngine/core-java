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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("InvocationGuard should")
class InvocationGuardTest extends UtilityClassTest<InvocationGuard> {

    InvocationGuardTest() {
        super(InvocationGuard.class);
    }

    @Test
    @DisplayName("throw SecurityException if no classes are allowed")
    void nobodyAllowed() {
        assertThrowsOn(InvocationGuard::allowOnly);
    }

    @Test
    @DisplayName("throw SecurityException for not allowed class")
    void notAllowed() {
        assertThrowsOn(() -> InvocationGuard.allowOnly(
                "java.lang.String", "org.junit.jupiter.api.Test")
        );
    }

    @Test
    @DisplayName("does not throw on allowed class")
    void pass() {
        InvocationGuard.allowOnly(getClass().getName());
    }

    private static void assertThrowsOn(Executable executable) {
        assertThrows(SecurityException.class, executable);
    }
}
