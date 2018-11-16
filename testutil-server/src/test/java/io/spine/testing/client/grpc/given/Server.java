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

package io.spine.testing.client.grpc.given;

import io.spine.core.BoundedContextName;
import io.spine.logging.Logging;
import io.spine.server.BoundedContext;
import io.spine.server.CommandService;
import io.spine.server.QueryService;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.transport.GrpcContainer;

import java.io.IOException;

import static io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;
import static io.spine.core.BoundedContextNames.newName;

public class Server implements Logging {

    private static final BoundedContextName contextName = newName("Tennis");

    private final int port;
    private final GrpcContainer grpcContainer;
    private final BoundedContext boundedContext;

    public Server() {
        this.port = DEFAULT_CLIENT_SERVICE_PORT;
        StorageFactory factory = InMemoryStorageFactory.newInstance(contextName, false);
        this.boundedContext = createBoundedContext(factory);
        this.grpcContainer = createGrpcContainer(this.boundedContext);
    }

    private static BoundedContext createBoundedContext(StorageFactory storageFactory) {
        BoundedContext context = BoundedContext
                .newBuilder()
                .setName(contextName)
                .setStorageFactorySupplier(() -> storageFactory)
                .build();

        context.register(new Table());
        return context;
    }

    private GrpcContainer createGrpcContainer(BoundedContext boundedContext) {
        CommandService commandService = CommandService
                .newBuilder()
                .add(boundedContext)
                .build();
        QueryService queryService = QueryService
                .newBuilder()
                .add(boundedContext)
                .build();

        GrpcContainer result = GrpcContainer
                .newBuilder()
                .setPort(port)
                .addService(commandService)
                .addService(queryService)
                .build();
        return result;
    }

    public void start() throws IOException {
        grpcContainer.start();
        grpcContainer.addShutdownHook();

        log().info("Server started, listening to commands on the port {}", port);
    }

    public void shutdown() throws Exception {
        log().info("Shutting down the server...");
        grpcContainer.shutdown();
        boundedContext.close();
    }

    public int getPort() {
        return port;
    }
}
