/*
 * Copyright 2021, TeamDev. All rights reserved.
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
import io.spine.core.UserId;
import io.spine.server.BoundedContextBuilder;
import io.spine.test.client.ClientTestContext;
import io.spine.test.client.users.ActiveUsers;
import io.spine.test.client.users.LoginStatus;
import io.spine.test.client.users.command.LogInUser;
import io.spine.test.client.users.event.UserLoggedIn;
import io.spine.test.client.users.event.UserLoggedOut;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.logging.mute.MuteLogging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.client.EventFilter.eq;
import static io.spine.test.client.ActiveUsersProjection.THE_ID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MuteLogging
@DisplayName("`Client` should")
class ClientTest extends AbstractClientTest {

    @Override
    protected ImmutableList<BoundedContextBuilder> contexts() {
        return ImmutableList.of(ClientTestContext.users());
    }

    @Test
    @DisplayName("be opened upon creation")
    void opened() {
        assertTrue(client().isOpen());
    }

    @Test
    @DisplayName("close upon request")
    void closing() {
        Client client = client();
        client.close();
        assertFalse(client.isOpen());
    }

    @Test
    @DisplayName("have `shutdown()` alias method")
    void shutdown() {
        Client client = client();
        client.shutdown();
        assertFalse(client.isOpen());
    }

    @Test
    @DisplayName("create requests on behalf of a user")
    void onBehalf() {
        UserId expected = GivenUserId.generated();
        ClientRequest request = client().onBehalfOf(expected);
        assertThat(request.user())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("create requests for a guest user")
    void guestRequest() {
        ClientRequest request = client().asGuest();
        assertThat(request.user())
                .isEqualTo(Client.DEFAULT_GUEST_ID);
    }

    @Nested
    @DisplayName("manage subscriptions")
    class ActiveSubscriptions {

        private List<Subscription> subscriptions;

        @BeforeEach
        void createSubscriptions() {
            subscriptions = new ArrayList<>();
            UserId currentUser = GivenUserId.generated();
            Client client = client();
            Subscription userLoggedIn =
                    client.onBehalfOf(currentUser)
                          .subscribeToEvent(UserLoggedIn.class)
                          .where(eq(UserLoggedIn.Field.user(), currentUser))
                          .observe((e) -> {})
                          .post();
            Subscription userLoggedOut =
                    client.onBehalfOf(currentUser)
                          .subscribeToEvent(UserLoggedOut.class)
                          .where(eq(UserLoggedOut.Field.user(), currentUser))
                          .observe((e) -> {})
                          .post();
            Subscription loginStatus =
                    client.onBehalfOf(currentUser)
                          .subscribeTo(LoginStatus.class)
                          .where(EntityStateFilter.eq(LoginStatus.Field.userId(),
                                                      currentUser.getValue()))
                          .observe((s) -> {})
                          .post();

            subscriptions.add(userLoggedIn);
            subscriptions.add(userLoggedOut);
            subscriptions.add(loginStatus);
        }

        @Test
        @DisplayName("remembering them until canceled")
        void remembering() {
            Client client = client();
            Subscriptions remembered = client.subscriptions();
            subscriptions.forEach(
                    (s) -> assertTrue(remembered.contains(s))
            );
            subscriptions.forEach(
                    (s) -> {
                        remembered.cancel(s);
                        assertFalse(remembered.contains(s));
                    }
            );
        }

        @Test
        @DisplayName("clear subscriptions when closing")
        void clearing() {
            Client client = client();
            Subscriptions subscriptions = client.subscriptions();
            this.subscriptions.forEach(
                    (s) -> assertTrue(subscriptions.contains(s))
            );

            client.close();

            assertThat(subscriptions.isEmpty())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("query")
    class Queries {

        @Test
        @DisplayName("entities by ID")
        void byId() {
            Client client = client();
            UserId user = GivenUserId.generated();

            LogInUser command = LogInUser.newBuilder()
                                         .setUser(user)
                                         .build();
            client.asGuest()
                  .command(command)
                  .postAndForget();
            ActiveUsers.Query query = ActiveUsers.query()
                                                 .id().is(THE_ID)
                                                 .build();
            ImmutableList<ActiveUsers> users =
                    client.onBehalfOf(user)
                          .run(query);
            assertThat(users)
                    .comparingExpectedFieldsOnly()
                    .containsExactly(ActiveUsers.newBuilder()
                                                .setCount(1)
                                                .build());
        }
    }
}
