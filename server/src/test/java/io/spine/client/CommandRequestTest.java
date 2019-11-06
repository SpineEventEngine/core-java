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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.ClientTestContext;
import io.spine.test.client.command.LogInUser;
import io.spine.test.route.UserLoggedIn;
import io.spine.testing.core.given.GivenUserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`CommandRequest` should")
class CommandRequestTest extends AbstractClientTest {

    private CommandRequest commandRequest;
    private boolean consumerCalled;

    @BeforeEach
    void createCommandRequest() {
        consumerCalled = false;
        CommandMessage cmd = LogInUser
                .newBuilder()
                .setUser(GivenUserId.generated())
                .build();
        commandRequest = client().asGuest()
                                 .command(cmd);
    }

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(ClientTestContext.builder());
    }

    @Test
    @DisplayName("deliver an event to a consumer")
    @Disabled("Until `EventsAfterCommand` subscribing is fixed")
    void eventConsumer() {
        commandRequest.observe(UserLoggedIn.class, (e) -> consumerCalled = true)
                      .post();
        assertThat(consumerCalled)
                .isTrue();
    }

    @Test
    @DisplayName("deliver an event and its context fo a consumer")
    @Disabled("Until `EventsAfterCommand` subscribing is fixed")
    void eventAndContextConsumer() {
        commandRequest.observe(UserLoggedIn.class, (e, c) -> consumerCalled = true)
                      .post();
        assertThat(consumerCalled)
                .isTrue();
    }
}
