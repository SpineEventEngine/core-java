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

package io.spine.testing.server.blackbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.server.blackbox.given.GivenCommandError.duplicationError;
import static io.spine.testing.server.blackbox.given.GivenCommandError.nonValidationError;
import static io.spine.testing.server.blackbox.given.GivenCommandError.unsupportedError;

@DisplayName("UnsupportedCommandGuard should")
class UnsupportedCommandGuardTest {

    private UnsupportedCommandGuard guard;

    @BeforeEach
    void initGuard() {
        guard = new UnsupportedCommandGuard();
    }

    @Test
    @DisplayName("check and remember the unsupported command error")
    void checkAndRememberUnsupported() {
        String commandType = "SomeCommand";
        boolean result = guard.checkAndRemember(unsupportedError(commandType));

        assertThat(result).isTrue();
        assertThat(guard.commandType()).isEqualTo(commandType);
    }

    @Test
    @DisplayName("return `false` and ignore the command validation error with the different code")
    void ignoreGenericValidationError() {
        boolean result = guard.checkAndRemember(duplicationError());

        assertThat(result).isFalse();
        assertThat(guard.commandType()).isNull();
    }

    @Test
    @DisplayName("return `false` and ignore the command error of the different type")
    void ignoreGenericCommandError() {
        boolean result = guard.checkAndRemember(nonValidationError());

        assertThat(result).isFalse();
        assertThat(guard.commandType()).isNull();
    }
}
