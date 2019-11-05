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
import io.grpc.inprocess.InProcessServerBuilder;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

/**
 * Abstract base for Client API tests.
 *
 * <p>Both {@code Client} and {@code Server} are created in-process with a name generated
 * by {@link #generateServerName()}. In order to configure the {@code Server} derived test suites
 * must implement the {@link #contexts()} providing builders of contexts of the backend services.
 *
 * <p>Both client and server instances are shut down after each test.
 */
abstract class AbstractClientTest {

    private Server server;
    private Client client;

    /**
     * Generates the name for the in-process server and the client.
     *
     * <p>Derived test suites may override to provide a custom name.
     */
    protected String generateServerName() {
        return InProcessServerBuilder.generateName();
    }

    /**
     * Provides builders of contexts to be added to the in-process server used in the tests.
     */
    protected abstract ImmutableList<BoundedContextBuilder> contexts();

    @BeforeEach
    void createServerAndClient() throws IOException {
        String serverName = generateServerName();
        Server.Builder serverBuilder = Server.inProcess(serverName);
        contexts().forEach(serverBuilder::add);
        server = serverBuilder.build();
        server.start();

        client = Client.inProcess(serverName)
                       .build();
    }

    @AfterEach
    void shutdownClientAndServer() {
        client.shutdown();
        server.shutdown();
    }

    /** Obtains the reference to the client. */
    protected final Client client() {
        return client;
    }
}
