/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.server.model.given.MethodParamsTestEnv.ScheduleCommandParamSpec;
import io.spine.server.type.CommandEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth8.assertThat;
import static io.spine.server.model.given.MethodParamsTestEnv.singleParamCommand;
import static io.spine.server.model.given.MethodParamsTestEnv.twoParamCommandAndCtx;

@DisplayName("`AllowedParams` should")
class AllowedParamsTest {

    private static final AllowedParams<CommandEnvelope> PARAMS =
            new AllowedParams<>(ScheduleCommandParamSpec.values());
    @Test
    @DisplayName("find a matching signature for the method among the predefined set of values")
    void findMatchingSignature() {
        Optional<?> matching = PARAMS.findMatching(twoParamCommandAndCtx());

        assertThat(matching).hasValue(ScheduleCommandParamSpec.MESSAGE_AND_CONTEXT);
    }

    @Test
    @DisplayName("return `Optional.empty()` if there is no matching signature")
    void returnOptionalEmptyIfNoSignatureMatch() {
        Optional<?> matching = PARAMS.findMatching(singleParamCommand());
        assertThat(matching).isEmpty();
    }
}
