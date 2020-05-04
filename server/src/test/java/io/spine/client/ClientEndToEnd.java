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

import io.grpc.ManagedChannel;
import io.spine.server.Server;
import io.spine.test.client.billing.PaymentReceived;
import io.spine.testing.SlowTest;
import io.spine.testing.logging.MuteLogging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static io.spine.client.Client.usingChannel;
import static io.spine.server.Server.atPort;
import static io.spine.test.client.ClientTestContext.tasks;
import static io.spine.test.client.ClientTestContext.users;
import static org.junit.jupiter.api.Assertions.fail;

@SlowTest
@MuteLogging
@DisplayName("Real gRPC-based `Client` should")
class ClientEndToEnd {

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String ADDRESS = "localhost";
    private static final int PORT = 4242;

    private Client client;
    private Server server;

    @BeforeEach
    void startAndConnect() throws IOException {
        ManagedChannel channel = forAddress(ADDRESS, PORT)
                .usePlaintext()
                .build();
        server = atPort(PORT)
                .add(users())
                .add(tasks())
                .build();
        server.start();
        client = usingChannel(channel).build();
    }

    @AfterEach
    void stopAndDisconnect() {
        server.shutdown();
        client.close();
    }

    @Test
    @DisplayName("subscribe to Event type from all contexts at once")
    void subscribeToUnknownEvent() {
        client.asGuest()
              .subscribeToEvent(PaymentReceived.class)
              .onStreamingError(Assertions::fail)
              .onConsumingError((consumer, throwable) -> fail(throwable))
              .observe(event -> {
                 // Do nothing.
              });
    }
}
