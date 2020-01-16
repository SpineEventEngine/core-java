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

package io.spine.core;

import com.google.common.truth.Truth8;
import io.spine.core.given.CoreMixinsTestEnv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests of mixins for {@code core} {@code Message}s.
 */
class CoreMixinsTest {

    private static final String SHOULD_MAKE_FIELDS_REACHABLE =
            "should define `readValue` so that the fields are reachable";

    @Test
    @DisplayName("`CommandIdMixin` " + SHOULD_MAKE_FIELDS_REACHABLE)
    void commandIdMixin() {
        CommandId msg = CommandId.generate();
        assertThat(msg.checkFieldsReachable()).isTrue();
    }

    @Test
    @DisplayName("`CommandMixin` " + SHOULD_MAKE_FIELDS_REACHABLE)
    void commandMixin() {
        Command command = CoreMixinsTestEnv.command();
        assertThat(command.checkFieldsReachable()).isTrue();
    }

    @Test
    @DisplayName("`EventContextMixin` " + SHOULD_MAKE_FIELDS_REACHABLE)
    void eventContextMixin() {
        Event event = CoreMixinsTestEnv.event();
        assertThat(event.getContext()
                        .checkFieldsReachable()).isTrue();
    }

    @Test
    @DisplayName("`EventIdMixin` " + SHOULD_MAKE_FIELDS_REACHABLE)
    void eventIdMixin() {
        Event event = CoreMixinsTestEnv.event();
        assertThat(event.getId()
                        .checkFieldsReachable()).isTrue();
    }

    @Test
    @DisplayName("`EventMixin` " + SHOULD_MAKE_FIELDS_REACHABLE)
    void eventMixin() {
        Event event = CoreMixinsTestEnv.event();
        assertThat(event.checkFieldsReachable()).isTrue();
    }

    @Test
    @DisplayName("`MessageIdMixin` " + SHOULD_MAKE_FIELDS_REACHABLE)
    void messageIdMixin() {
        MessageId messageId = CoreMixinsTestEnv.messageId();
        assertThat(messageId.checkFieldsReachable()).isTrue();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")    // Checked by `Truth8.assertThat()`.
    @Test
    @DisplayName("`OriginMixin` " + SHOULD_MAKE_FIELDS_REACHABLE)
    void originIdMixin() {
        Optional<Origin> origin = CoreMixinsTestEnv.event()
                                                   .origin();
        Truth8.assertThat(origin)
              .isPresent();
        assertThat(origin.get()
                         .checkFieldsReachable()).isTrue();
    }
}
