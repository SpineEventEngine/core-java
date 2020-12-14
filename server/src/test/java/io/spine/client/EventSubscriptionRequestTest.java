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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoTruth;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.EventContext;
import io.spine.core.UserId;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.ClientTestContext;
import io.spine.test.client.users.command.LogInUser;
import io.spine.test.client.users.event.UserLoggedIn;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.logging.MuteLogging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.CompositeEventFilter.all;
import static io.spine.client.EventFilter.eq;

@DisplayName("`EventSubscriptionRequest` should")
class EventSubscriptionRequestTest extends AbstractClientTest {

    private CommandRequest commandRequest;
    private UserId userId;

    /** Registers which event consumers were called. */
    private final ConsumerCallCounter counter = new ConsumerCallCounter();
    private EventMessage rememberedMessage;
    private EventContext rememberedContext;

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(ClientTestContext.users());
    }

    @BeforeEach
    void createCommandRequest() {
        counter.clear();
        rememberedContext = EventContext.getDefaultInstance();
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

    @MuteLogging
    @Test
    @DisplayName("allow to receive event messages")
    void observeEventMessage() {
        client().asGuest()
                .subscribeToEvent(UserLoggedIn.class)
                .observe(counter::add)
                .post();
        commandRequest.postAndForget();

        assertThat(counter.contains(UserLoggedIn.class))
            .isTrue();
    }

    @MuteLogging
    @Nested
    @DisplayName("allow to receive event messages and contexts, filtering by")
    class Filtering {

        private UserId guestUser;

        @BeforeEach
        void subscribeWithFiltering() {
            guestUser = client().asGuest()
                                .user();
            client().asGuest()
                    .subscribeToEvent(UserLoggedIn.class)
                    .where(all(eq(UserLoggedIn.Field.user(), userId),
                               eq(EventContext.Field.pastMessage()
                                                    .actorContext()
                                                    .actor(), guestUser)))
                    .observe((e, c) -> {
                        rememberedMessage = e;
                        rememberedContext = c;
                    })
                    .post();
            commandRequest.postAndForget();
        }

        @Test
        @DisplayName("event message field")
        void byMessageField() {
            assertThat(rememberedMessage)
                    .isInstanceOf(UserLoggedIn.class);
            UserLoggedIn expected = UserLoggedIn
                    .newBuilder()
                    .setUser(userId)
                    .build();
            ProtoTruth.assertThat(rememberedMessage)
                      .comparingExpectedFieldsOnly()
                      .isEqualTo(expected);
        }

        @Test
        @DisplayName("context fields")
        void observeEventMessageAndContext() {
            assertThat(rememberedContext.getPastMessage()
                                        .getActorContext()
                                        .getActor())
                    .isEqualTo(guestUser);
        }
    }
}
