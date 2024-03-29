/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import io.spine.server.delivery.given.NoOpEndpoint;
import io.spine.server.type.CommandEnvelope;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenUserId;
import io.spine.time.ZoneIds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.delivery.InboxLabel.HANDLE_COMMAND;
import static io.spine.server.delivery.InboxLabel.REACT_UPON_EVENT;

@DisplayName("Delivery endpoints should")
class EndpointsTest {

    @Test
    @DisplayName("be empty by default")
    void beEmpty() {
        var endpoints = new Endpoints<String, CommandEnvelope>();
        assertThat(endpoints.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("allow to append endpoint providers and remember the last one per label")
    void appendEndpoint() {
        var endpoints = new Endpoints<String, CommandEnvelope>();

        var first = noOpEndpoint();
        var second = noOpEndpoint();
        LazyEndpoint<String, CommandEnvelope> firstProvider = envelope -> first;
        LazyEndpoint<String, CommandEnvelope> secondProvider = envelope -> second;

        var label = HANDLE_COMMAND;

        endpoints.add(label, firstProvider);
        checkContains(endpoints, label, first);

        endpoints.add(label, secondProvider);
        checkContains(endpoints, label, second);
    }

    @Test
    @DisplayName("return `Optional.empty()` if no endpoint providers configured for the label")
    void returnEmpty() {
        var endpoints = new Endpoints<String, CommandEnvelope>();
        var result =
                endpoints.get(REACT_UPON_EVENT, cmdEnvelope());
        assertThat(result.isPresent()).isFalse();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")    // checked via the Truth API statement.
    private static void checkContains(Endpoints<String, CommandEnvelope> endpoints,
                                      InboxLabel label,
                                      MessageEndpoint<String, CommandEnvelope> endpoint) {
        var someCmdEnvelope = cmdEnvelope();
        var cmdEndpoint =
                endpoints.get(label, someCmdEnvelope);
        assertThat(cmdEndpoint.isPresent()).isTrue();
        assertThat(cmdEndpoint.get()).isEqualTo(endpoint);
    }

    private static CommandEnvelope cmdEnvelope() {
        var commandFactory =
                new TestActorRequestFactory(GivenUserId.generated(), ZoneIds.systemDefault());
        return CommandEnvelope.of(commandFactory.generateCommand());
    }

    private static MessageEndpoint<String, CommandEnvelope> noOpEndpoint() {
        return new NoOpEndpoint();
    }
}
