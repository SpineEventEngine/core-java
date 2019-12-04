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
import io.spine.core.UserId;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.ClientTestContext;
import io.spine.test.client.command.LogInUser;
import io.spine.test.client.event.UserLoggedIn;
import io.spine.testing.core.given.GivenUserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.eq;

@DisplayName("`EventSubscriptionRequest` should")
class EventSubscriptionRequestTest extends AbstractClientTest {

    /** Registers which event consumers were called. */
    private final ConsumerCallCounter counter = new ConsumerCallCounter();
    private CommandRequest commandRequest;
    private UserId userId;

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(ClientTestContext.builder());
    }

    @BeforeEach
    void createCommandRequest() {
        counter.clear();
        userId = GivenUserId.generated();
        CommandMessage cmd = LogInUser
                .newBuilder()
                .setUser(userId)
                .build();
        commandRequest =
                client().asGuest()
                        .command(cmd);
    }

    @AfterEach
    void clearCounter() {
        counter.clear();
    }

    @Test
    @DisplayName("allow to receive event messages")
    void observeEventMessage() {
        UserId guestUser = client().asGuest()
                              .user();
        client().asGuest()
                .subscribeToEvent(UserLoggedIn.class)
                .where(all(eq("user", userId),
                           eq("context.past_message.actor_context.actor", guestUser)))
                .observe(counter::add)
                .post();
        commandRequest.post();

        assertThat(counter.contains(UserLoggedIn.class))
            .isTrue();
    }

    @Test
    @DisplayName("allow to receive event messages and contexts")
    void observeEventMessageAndContext() {
        client().asGuest()
                .subscribeToEvent(UserLoggedIn.class)
                .observe((e, c) -> counter.add(e))
                .post();
        commandRequest.post();

        assertThat(counter.contains(UserLoggedIn.class))
                .isTrue();
    }
}
