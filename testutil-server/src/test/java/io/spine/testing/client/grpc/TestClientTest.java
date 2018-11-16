/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.client.grpc;

import com.google.common.truth.OptionalSubject;
import io.spine.core.Ack;
import io.spine.core.UserId;
import io.spine.testing.client.grpc.command.Ping;
import io.spine.testing.client.grpc.given.Server;
import io.spine.testing.server.ShardingReset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.truth.Truth8.assertThat;

@SuppressWarnings("StaticVariableMayNotBeInitialized")
@ExtendWith(ShardingReset.class)
class TestClientTest {

    private static Server server;
    private static TestClient client;

    @BeforeAll
    static void setUpAll() throws IOException {
        server = new Server();
        server.start();
        UserId userId = UserId.newBuilder()
                              .setValue(TestClientTest.class.getSimpleName())
                              .build();
        client = new TestClient(userId, "localhost", server.getPort());
    }

    @AfterAll
    @SuppressWarnings("StaticVariableUsedBeforeInitialization") // see setUpAll()
    static void tearDownAll() throws Exception {
        client.shutdown();
        server.shutdown();
    }

    @Test
    void post() {
        Optional<Ack> result = client.post(Ping.getDefaultInstance());

        OptionalSubject assertThat = assertThat(result);
        assertThat.isPresent();
    }

    @Test
    void queryAll() {
        //TODO:2018-11-15:alexander.yevsyukov: Implement
    }

    @Test
    void shutdown() {
        //TODO:2018-11-15:alexander.yevsyukov: Implement as a separate nested test.
    }
}
