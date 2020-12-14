/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.security;

import com.example.ForeignClass;
import io.spine.given.NonServerClass;
import io.spine.system.server.given.SystemConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("`Security` should")
class SecurityTest {

    @Test
    @DisplayName("have private constructor")
    void hasPrivateConstructor() {
        assertHasPrivateParameterlessCtor(Security.class);
    }

    @Nested
    @DisplayName("restrict callers to `io.spine.server` packages")
    class ServerFramework {

        @Test
        @DisplayName("allowing calls from `io.spine.server` packages")
        void allowFormServerPackages() {
            try {
                guardedCall();
            } catch (Exception e) {
                fail(e);
            }
        }

        @Test
        @DisplayName("allowing calls form the `io.spine.system.server` packages")
        void allowFromSystemServerPackages() {
            try {
                SystemConfig.guardedCall();
            } catch (Exception e) {
                fail(e);
            }
        }

        @Test
        @DisplayName("prohibiting calls from outside of framework")
        void prohibitingFromOutside() {
            assertThrowsOn(ForeignClass::attemptToCallRestrictedApi);
        }

        @Test
        @DisplayName("prohibiting calls from non-server framework packages")
        void prohibitFrameworkButNonServer() {
            assertThrowsOn(NonServerClass::attemptToCallRestrictedApi);
        }
    }

    private static void assertThrowsOn(Executable executable) {
        assertThrows(SecurityException.class, executable);
    }

    /**
     * This is a guarded method, which is invoked from
     * the {@linkplain ServerFramework#allowFormServerPackages() test}, which should pass as the
     * test belongs to the {@code io.spine.server} package.
     */
    private static void guardedCall() {
        Security.allowOnlyFrameworkServer();
    }
}
