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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.ClientTestContext;
import io.spine.test.client.users.UserAccount;
import io.spine.test.client.users.command.LogInUser;
import io.spine.test.client.users.event.UserLoggedIn;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.logging.MuteLogging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@MuteLogging
@DisplayName("`ClientRequest` should")
class ClientRequestTest extends AbstractClientTest {

    private ClientRequest request;

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(ClientTestContext.users());
    }

    @BeforeEach
    void createRequest() {
        request = client().asGuest();
    }

    @Nested
    @DisplayName("initiate")
    class Initiate {

        @Test
        @DisplayName("`CommandRequest`")
        void command() {
            CommandMessage msg = LogInUser
                    .newBuilder()
                    .setUser(GivenUserId.generated())
                    .build();

            CommandRequest commandRequest = request.command(msg);
            assertThat(commandRequest.message())
                    .isEqualTo(msg);
        }

        @Test
        @DisplayName("`SubscriptionRequest`")
        void subscription() {
            Class<? extends EntityState> messageType = UserAccount.class;
            SubscriptionRequest<? extends EntityState> subscriptionRequest =
                    request.subscribeTo(messageType);

            assertThat(subscriptionRequest.messageType())
                    .isEqualTo(messageType);
        }

        @Test
        @DisplayName("`EventSubscriptionRequest`")
        void eventSubscription() {
            Class<? extends EventMessage> eventType = UserLoggedIn.class;
            EventSubscriptionRequest<? extends EventMessage> eventSubscriptionRequest =
                    request.subscribeToEvent(eventType);

            assertThat(eventSubscriptionRequest.messageType())
                    .isEqualTo(eventType);
        }

        @Test
        @DisplayName("`QueryRequest`")
        void query() {
            Class<? extends EntityState> messageType = UserAccount.class;
            QueryRequest<? extends EntityState> queryRequest =
                    request.select(messageType);

            assertThat(queryRequest.messageType())
                    .isEqualTo(messageType);
        }
    }
}
