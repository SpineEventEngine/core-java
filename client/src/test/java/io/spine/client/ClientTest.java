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

import io.spine.core.UserId;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.Server;
import io.spine.test.client.UserAccount;
import io.spine.test.client.event.UserLoggedIn;
import io.spine.test.client.event.UserLoggedOut;
import io.spine.testing.TestValues;
import io.spine.testing.core.given.GivenUserId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`Client` should")
class ClientTest {

    @SuppressWarnings("StaticVariableMayNotBeInitialized")
    private static Server server;
    @SuppressWarnings("StaticVariableMayNotBeInitialized")
    private static int port;

    @BeforeAll
    static void createServer() throws IOException {
        port = TestValues.random(64000, 65000);
        server = Server.newBuilder()
                       .setPort(port)
                       .add(BoundedContextBuilder.assumingTests())
                       .build();
        server.start();
    }

    @AfterAll
    @SuppressWarnings("StaticVariableUsedBeforeInitialization") // see createServer().
    static void shutdownServer() {
        server.shutdown();
    }

    private Client client;

    @BeforeEach
    void createClient() {
        client = Client.connectTo("localhost", port)
                       .build();
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    @DisplayName("be opened upon creation")
    void opened() {
        assertTrue(client.isOpen());
    }

    @Test
    @DisplayName("close upon request")
    void closing() {
        client.close();
        assertFalse(client.isOpen());
    }

    @Test
    @DisplayName("have `shutdown()` alias method")
    void shutdown() {
        client.shutdown();
        assertFalse(client.isOpen());
    }

    @Test
    @DisplayName("create requests on behalf of a user")
    void onBehalf() {
        UserId expected = GivenUserId.generated();
        ClientRequest request = client.onBehalfOf(expected);
        assertThat(request.user())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("create requests for a guest user")
    void guestRequest() {
        ClientRequest request = client.asGuest();
        assertThat(request.user())
                .isEqualTo(Client.DEFAULT_GUEST_ID);
    }

    @Nested
    @DisplayName("manage subscriptions")
    class Subscriptions {

        private List<Subscription> subscriptions;

        @BeforeEach
        void createSubscriptions() {
            UserId currentUser = GivenUserId.generated();
            String userField = "user";
            Subscription userLoggedIn =
                    client.onBehalfOf(currentUser)
                          .subscribeToEvent(UserLoggedIn.class)
                          .where(eq(userField, currentUser))
                          .observe((e) -> {})
                          .post();
            Subscription userLoggedOut =
                    client.onBehalfOf(currentUser)
                          .subscribeToEvent(UserLoggedOut.class)
                          .where(eq(userField, currentUser))
                          .observe((e) -> {})
                          .post();
            Subscription userProfile =
                    client.onBehalfOf(currentUser)
                    .subscribeTo(UserAccount.class)
                    .where(eq(userField, currentUser))
                    .post();

            subscriptions.add(userLoggedIn);
            subscriptions.add(userLoggedOut);
            subscriptions.add(userProfile);
        }

        @Test
        @DisplayName("remembering them until cancelled")
        void remembering() {
            ActiveSubscriptions remembered = client.subscriptions();
            subscriptions.forEach(
                    (s) -> assertTrue(remembered.contains(s))
            );
            subscriptions.forEach(
                    (s) -> {
                        client.cancel(s);
                        assertFalse(remembered.contains(s));
                    }
            );
        }
    }
}
