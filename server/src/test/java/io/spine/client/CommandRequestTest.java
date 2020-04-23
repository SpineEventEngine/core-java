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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.ClientTestContext;
import io.spine.test.client.command.LogInUser;
import io.spine.test.client.event.UserAccountCreated;
import io.spine.test.client.event.UserLoggedIn;
import io.spine.test.client.rejection.Rejections.UserAlreadyLoggedIn;
import io.spine.testing.core.given.GivenUserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`CommandRequest` should")
class CommandRequestTest extends AbstractClientTest {

    /** The object under the test. */
    private CommandRequest commandRequest;

    /** Registers which event consumers were called. */
    private final ConsumerCallCounter counter = new ConsumerCallCounter();

    @BeforeEach
    void createCommandRequest() {
        counter.clear();
        CommandMessage cmd = LogInUser
                .newBuilder()
                .setUser(GivenUserId.generated())
                .build();
        commandRequest = client().asGuest()
                                 .command(cmd);
    }

    @AfterEach
    void clearCounter() {
        counter.clear();
    }

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(ClientTestContext.builder());
    }

    @Test
    @DisplayName("deliver an event to a consumer")
    void eventConsumer() {
        commandRequest.observe(UserLoggedIn.class, counter::add)
                      .observe(UserAccountCreated.class, counter::add)
                      .post();
        assertThat(counter.containsAll(UserLoggedIn.class, UserAccountCreated.class))
                .isTrue();
    }

    @Test
    @DisplayName("deliver an event and its context to a consumer")
    void eventAndContextConsumer() {
        commandRequest.observe(UserLoggedIn.class, (e, c) -> counter.add(e))
                      .observe(UserAccountCreated.class, (e, c) -> counter.add(e))
                      .post();
        assertThat(counter.containsAll(UserLoggedIn.class, UserAccountCreated.class))
                .isTrue();
    }

    @Test
    @DisplayName("deliver a rejection to its consumers")
    void rejections() {
        // Post the command so that the user is logged in. We are not interested in events here.
        commandRequest.post();
        // Now post the command again, expecting the rejection.
        commandRequest.observe(UserAlreadyLoggedIn.class, counter::add)
                      .post();

        assertThat(counter.contains(UserAlreadyLoggedIn.class))
                .isTrue();
    }

    @Nested
    @DisplayName("Allow setting custom streaming error handler")
    class CustomStreamingErrorHandler {

        @Test
        @DisplayName("rejecting `null`")
        void rejectingNull() {
            assertThrows(NullPointerException.class, () -> commandRequest.onStreamingError(null));
        }
    }
}
