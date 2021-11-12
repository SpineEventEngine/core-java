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

package io.spine.testing.server.blackbox;

import io.spine.base.Identifier;
import io.spine.client.Client;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.Closeable;
import io.spine.server.CommandService;
import io.spine.server.GrpcContainer;
import io.spine.server.QueryService;
import io.spine.server.SubscriptionService;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Objects.isNull;

/**
 * Creates {@code Client} instances for the passed Bounded Context.
 *
 * <p>As {@code Client} requires a gRPC server to be connected to, the supplier assembles
 * and starts a {@link GrpcContainer}.
 */
class ClientSupplier implements Closeable {

    /**
     * The context to which this supplier creates {@code Client} instances.
     */
    private final BoundedContext context;

    /**
     * Instances created so far.
     *
     * <p>Ultimately, they need to be closed.
     *
     * @see #close()
     */
    private final List<Client> clients;

    private @MonotonicNonNull GrpcContainer grpcContainer;
    private @MonotonicNonNull String serverName;

    /**
     * Creates a supplier, {@code Client}s of which would be linked to the specified context.
     */
    ClientSupplier(BoundedContext context) {
        this.context = context;
        this.clients = new ArrayList<>();
    }

    /**
     * Opens a new {@code Client} to a single tenant {@code BoundedContext}.
     */
    Client create() {
        ensureServer();
        Client client = Client.inProcess(serverName)
                              .build();
        clients.add(client);
        return client;
    }

    /**
     * Opens a new {@code Client} to a multi-tenant {@code BoundedContext} with a tenant specified.
     */
    Client createFor(TenantId tenantId) {
        ensureServer();
        Client client = Client.inProcess(serverName)
                              .forTenant(tenantId)
                              .build();
        clients.add(client);
        return client;
    }

    /**
     * Runs the gRPC server in case it has not been run previously.
     */
    private void ensureServer() {
        checkOpen();
        if (isNull(serverName)) {
            initServer();
        }
    }

    private void initServer() {
        CommandService commandService = CommandService.withSingle(context);
        QueryService queryService = QueryService.withSingle(context);
        SubscriptionService subscriptionService = SubscriptionService.withSingle(context);

        String serverName = Identifier.newUuid();
        GrpcContainer grpcContainer = GrpcContainer
                .inProcess("BlackBox-Server" + serverName)
                .addService(commandService)
                .addService(queryService)
                .addService(subscriptionService)
                .build();

        try {
            grpcContainer.start();
        } catch (IOException e) {
            throw illegalStateWithCauseOf(e);
        }

        this.serverName = serverName;
        this.grpcContainer = grpcContainer;
    }

    /**
     * Closes all created clients and shuts down the associated {@code GrpcContainer}.
     */
    @Override
    @SuppressWarnings("TestOnlyProblems" /* Calling the production-level method. */)
    public void close() throws Exception {
        if (!isOpen() || isNull(grpcContainer)) {
            return;
        }
        grpcContainer.shutdownNowAndWait();
        clients.forEach(Client::close);
        clients.clear();
    }

    @Override
    public boolean isOpen() {
        return isNull(grpcContainer) || !grpcContainer.isShutdown();
    }
}
